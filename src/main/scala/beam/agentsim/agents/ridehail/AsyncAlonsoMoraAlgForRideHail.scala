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
  beamServices: BeamServices
)(implicit val skimmer: BeamSkimmer) {

  private def vehicle2Requests(v: VehicleAndSchedule): (List[RTVGraphNode], List[(RTVGraphNode, RTVGraphNode)]) = {
    val vertices = mutable.ListBuffer.empty[RTVGraphNode]
    val edges = mutable.ListBuffer.empty[(RTVGraphNode, RTVGraphNode)]
    val requestsBuffer = mutable.ArrayBuffer.empty[RideHailTrip]
    val center = v.getLastDropoff.activity.getCoord
    spatialDemand
      .getDisk(
        center.getX,
        center.getY,
        timeWindow(Pickup) * BeamSkimmer.speedMeterPerSec(BeamMode.CAV)
      )
      .asScala
      .toList
      .sortBy(x => GeoUtils.minkowskiDistFormula(center, x.pickup.activity.getCoord))
      .take(maxRequestsPerVehicle) foreach (
      r =>
        AlonsoMoraPoolingAlgForRideHail
          .getRidehailSchedule(timeWindow, v.schedule ++ List(r.pickup, r.dropoff), beamServices) match {
          case Some(schedule) =>
            val t = RideHailTrip(List(r), schedule)
            requestsBuffer prepend t
            if (!vertices.contains(v)) vertices prepend v
            vertices prepend (r, t)
            edges prepend ((r, t), (t, v))
          case _ =>
        }
    )
    if (requestsBuffer.nonEmpty) {
      for (k <- 2 until v.getFreeSeats + 1) {
        val tripsList = mutable.ListBuffer.empty[RideHailTrip]
        for (i <- 0 until requestsBuffer.size - 1) {
          val reqA = requestsBuffer(i)
          requestsBuffer
            .slice(i + 1, requestsBuffer.size)
            .withFilter(x => (x.requests.size + reqA.requests.size) == k)
            .withFilter(x => !(x.requests exists (s => reqA.requests contains s)))
            .foreach { reqB =>
              AlonsoMoraPoolingAlgForRideHail
                .getRidehailSchedule(
                  timeWindow,
                  v.schedule ++ (reqA.requests ++ reqB.requests).flatMap(x => List(x.pickup, x.dropoff)),
                  beamServices
                ) match {
                case Some(schedule) =>
                  val trip = RideHailTrip(reqA.requests ++ reqB.requests, schedule)
                  tripsList prepend trip
                  vertices prepend trip
                  trip.requests.foldLeft(()) { case (_, r) => edges prepend ((r, trip)) }
                  edges prepend ((trip, v))
                case _ =>
              }
            }
        }
        requestsBuffer.appendAll(tripsList)
      }
    }
    (vertices.toList, edges.toList)
  }

  def asyncBuildOfRTVGraph(): Future[AlonsoMoraPoolingAlgForRideHail.RTVGraph] = {
    Future
      .sequence(supply.withFilter(_.getFreeSeats >= 1).map { v =>
        Future { vehicle2Requests(v) }
      })
      .map { result =>
        val rTvG = AlonsoMoraPoolingAlgForRideHail.RTVGraph(classOf[DefaultEdge])
        result foreach {
          case (vertices, edges) =>
            vertices foreach (vertex => rTvG.addVertex(vertex))
            edges foreach { case (vertexSrc, vertexDst) => rTvG.addEdge(vertexSrc, vertexDst) }
        }
        rTvG
      }
      .recover {
        case e =>
          println(e.getMessage)
          AlonsoMoraPoolingAlgForRideHail.RTVGraph(classOf[DefaultEdge])
      }
  }

  def greedyAssignment(): Future[List[(RideHailTrip, VehicleAndSchedule, Int)]] = {
    val rTvGFuture = asyncBuildOfRTVGraph()
    val V: Int = supply.foldLeft(0) { case (maxCapacity, v) => Math max (maxCapacity, v.getFreeSeats) }
    val C0: Int = timeWindow.foldLeft(0)(_ + _._2)
    rTvGFuture.map { rTvG =>
      val greedyAssignmentList = mutable.ListBuffer.empty[(RideHailTrip, VehicleAndSchedule, Int)]
      val Rok = mutable.ListBuffer.empty[CustomerRequest]
      val Vok = mutable.ListBuffer.empty[VehicleAndSchedule]
      for (k <- V to 1 by -1) {
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
          .sortBy(- _._3)
          .foldLeft(()) {
            case (_, (trip, vehicle, cost)) =>
              if (!(trip.requests exists (r => Rok contains r)) && !(Vok contains vehicle)) {
                Rok.prependAll(trip.requests)
                Vok.prepend(vehicle)
                greedyAssignmentList.prepend((trip, vehicle, cost))
              }
          }
      }
      greedyAssignmentList.toList
    }
  }
}
