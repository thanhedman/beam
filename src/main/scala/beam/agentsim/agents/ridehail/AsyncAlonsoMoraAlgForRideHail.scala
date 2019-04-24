package beam.agentsim.agents.ridehail

import beam.agentsim.agents.{MobilityRequestTrait, Pickup}
import beam.agentsim.agents.ridehail.AlonsoMoraPoolingAlgForRideHail._
import beam.router.BeamSkimmer
import beam.router.Modes.BeamMode
import beam.sim.BeamServices
import beam.sim.common.GeoUtils

import org.jgrapht.graph.DefaultEdge
import org.matsim.core.utils.collections.QuadTree

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.collection.mutable

class AsyncAlonsoMoraAlgForRideHail(
  spatialDemand: QuadTree[CustomerRequest],
  supply: List[VehicleAndSchedule],
  timeWindow: Map[MobilityRequestTrait, Int],
  maxRequestsPerVehicle: Int,
  maxPassengersPerVehicle: Int,
  beamServices: BeamServices
)(implicit val skimmer: BeamSkimmer) {

  private def vehicle2Requests(
    v: VehicleAndSchedule,
    searchDist: Double,
    passengersPerVehicle: Int
  ): (List[RTVGraphNode], List[(RTVGraphNode, RTVGraphNode)], Int) = {
    val vertices = mutable.ListBuffer.empty[RTVGraphNode]
    val edges = mutable.ListBuffer.empty[(RTVGraphNode, RTVGraphNode)]
    val tripsBuffer = mutable.ListBuffer.empty[RideHailTrip]
    val tripsMapByK = mutable.Map.empty[Int, mutable.ListBuffer[RideHailTrip]]
    val center = v.getLastDropoff.activity.getCoord
    var maxK = 1
    spatialDemand
      .getDisk(center.getX, center.getY, searchDist)
      .asScala
      .toList
      .sortBy(x => GeoUtils.minkowskiDistFormula(center, x.pickup.activity.getCoord))
      .take(maxRequestsPerVehicle) foreach (
      r =>
        AlonsoMoraPoolingAlgForRideHail
          .getRidehailSchedule(timeWindow, v.schedule ++ List(r.pickup, r.dropoff), beamServices) match {
          case Some(schedule) =>
            val t = RideHailTrip(List(r), schedule)
            tripsBuffer append t
            if (!vertices.contains(v)) vertices append v
            vertices append (r, t)
            edges append ((r, t), (t, v))
          case _ =>
        }
    )
    tripsMapByK.put(1, mutable.ListBuffer(tripsBuffer))
    if (tripsBuffer.nonEmpty) (2 until (passengersPerVehicle + 1)).foreach { k =>
      val kTripsBuffer = mutable.ListBuffer.empty[RideHailTrip]
      tripsBuffer.foreach { reqA =>
        tripsMapByK(k - reqA.requests.size)
          .withFilter(x => x != reqA && !(x.requests exists (s => reqA.requests contains s)))
          .foreach(
            reqB =>
              AlonsoMoraPoolingAlgForRideHail
                .getRidehailSchedule(
                  timeWindow,
                  v.schedule ++ (reqA.requests ++ reqB.requests).flatMap(x => List(x.pickup, x.dropoff)),
                  beamServices
                ) match {
                case Some(schedule) =>
                  val trip = RideHailTrip(reqA.requests ++ reqB.requests, schedule)
                  kTripsBuffer append trip
                  vertices append trip
                  trip.requests.foldLeft(()) { case (_, r) => edges append ((r, trip)) }
                  edges append ((trip, v))
                  if(maxK < k) maxK = k
                case _ =>
            }
          )
      }
      tripsBuffer.appendAll(kTripsBuffer)
      tripsMapByK.put(k, kTripsBuffer)
    }
    (vertices.toList, edges.toList, maxK)
  }

  def asyncBuildOfRTVGraph(): Future[(AlonsoMoraPoolingAlgForRideHail.RTVGraph, Int)] = {
    val searchDistance = timeWindow(Pickup) * BeamSkimmer.speedMeterPerSec(BeamMode.CAV)
    var maxK = 0
    Future
      .sequence(supply.withFilter(_.getFreeSeats >= 1).map { v =>
        Future { vehicle2Requests(v, searchDistance, Math.max(maxPassengersPerVehicle, v.getFreeSeats)) }
      })
      .map { result =>
        val rTvG = AlonsoMoraPoolingAlgForRideHail.RTVGraph(classOf[DefaultEdge])
        result foreach {
          case (vertices, edges, maxKPerV) =>
            vertices foreach (vertex => rTvG.addVertex(vertex))
            edges foreach { case (vertexSrc, vertexDst) => rTvG.addEdge(vertexSrc, vertexDst) }
            if (maxK < maxKPerV) maxK = maxKPerV
        }
        (rTvG, maxK)
      }
      .recover {
        case e =>
          println(e.getMessage)
          (AlonsoMoraPoolingAlgForRideHail.RTVGraph(classOf[DefaultEdge]), 0)
      }
  }

  def greedyAssignment(): Future[List[(RideHailTrip, VehicleAndSchedule, Int)]] = {
    val rTvGFuture = asyncBuildOfRTVGraph()
    val C0: Int = timeWindow.foldLeft(0)(_ + _._2)
    rTvGFuture.map { case (rTvG, maxK) =>
      val greedyAssignmentList = mutable.ListBuffer.empty[(RideHailTrip, VehicleAndSchedule, Int)]
      val Rok = mutable.ListBuffer.empty[CustomerRequest]
      val Vok = mutable.ListBuffer.empty[VehicleAndSchedule]
      (maxK to 1 by -1).foreach { k =>
        rTvG
          .vertexSet()
          .asScala
          .filter(t => t.isInstanceOf[RideHailTrip] && t.asInstanceOf[RideHailTrip].requests.size == k)
          .map { t =>
            val trip = t.asInstanceOf[RideHailTrip]
            val vehicle = rTvG
              .getEdgeTarget(
                rTvG
                  .outgoingEdgesOf(trip)
                  .asScala
                  .filter(e => rTvG.getEdgeTarget(e).isInstanceOf[VehicleAndSchedule])
                  .head
              )
              .asInstanceOf[VehicleAndSchedule]
            val cost = trip.cost + C0 * rTvG
              .outgoingEdgesOf(trip)
              .asScala
              .filter(e => rTvG.getEdgeTarget(e).isInstanceOf[CustomerRequest])
              .count(y => !trip.requests.contains(y.asInstanceOf[CustomerRequest]))

            (trip, vehicle, cost)
          }
          .toList
          .sortBy(_._3)
          .foreach { case (trip, vehicle, cost) =>
              if (!(trip.requests exists (r => Rok contains r)) && !(Vok contains vehicle)) {
                Rok.appendAll(trip.requests)
                Vok.append(vehicle)
                greedyAssignmentList.append((trip, vehicle, cost))
              }
          }
      }
      greedyAssignmentList.toList
    }
  }
}
