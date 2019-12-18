package beam.agentsim.agents.ridehail

import beam.agentsim.agents.planning.Trip
import beam.agentsim.agents.ridehail.AlonsoMoraPoolingAlgForRideHail._
import beam.agentsim.agents.ridehail.RideHailVehicleManager.RideHailAgentLocation
import beam.agentsim.agents.vehicles.EnergyEconomyAttributes.Powertrain
import beam.agentsim.agents.vehicles.{BeamVehicle, BeamVehicleType, PersonIdWithActorRef}
import beam.agentsim.agents.{MobilityRequest, _}
import beam.router.BeamRouter.Location
import beam.router.BeamSkimmer
import beam.router.BeamSkimmer.Skim
import beam.router.Modes.BeamMode
import beam.sim.common.GeoUtils
import beam.sim.{BeamServices, Geofence}
import org.jgrapht.graph.{DefaultEdge, DefaultUndirectedWeightedGraph}
import org.matsim.api.core.v01.Id
import org.matsim.api.core.v01.population.Activity
import org.matsim.core.population.PopulationUtils
import org.matsim.core.utils.collections.QuadTree

import scala.collection.JavaConverters._
import scala.collection.immutable.List
import scala.collection.mutable.ListBuffer
import beam.sim.config.BeamConfig.Beam.Agentsim.Agents.RideHail.AllocationManager

import scala.collection.mutable
import scala.concurrent.Future

class AlonsoMoraPoolingAlgForRideHail(
  spatialDemand: QuadTree[CustomerRequest],
  supply: List[VehicleAndSchedule],
  beamServices: BeamServices,
  skimmer: BeamSkimmer
) {

  // Methods below should be kept as def (instead of val) to allow automatic value updating
  private def alonsoMora: AllocationManager.AlonsoMora =
    beamServices.beamConfig.beam.agentsim.agents.rideHail.allocationManager.alonsoMora
  private def solutionSpaceSizePerVehicle: Int = alonsoMora.solutionSpaceSizePerVehicle
  private def waitingTimeInSec: Int = alonsoMora.waitingTimeInSec

  // Request Vehicle Graph
  private def pairwiseRVGraph: RVGraph = {
    val rvG = RVGraph(classOf[RideHailTrip])
    val searchRadius = waitingTimeInSec * BeamSkimmer.speedMeterPerSec(BeamMode.CAV)

    for (r1: CustomerRequest <- spatialDemand.values().asScala) {
      val center = r1.pickup.activity.getCoord
      val demand = spatialDemand.getDisk(center.getX, center.getY, searchRadius).asScala
      for (r2: CustomerRequest <- demand) {
        if (r1 != r2 && !rvG.containsEdge(r1, r2)) {
          getRidehailSchedule(
            List.empty[MobilityRequest],
            List(r1.pickup, r1.dropoff, r2.pickup, r2.dropoff),
            Integer.MAX_VALUE,
            skimmer
          ).map { schedule =>
            rvG.addVertex(r2)
            rvG.addVertex(r1)
            rvG.addEdge(r1, r2, RideHailTrip(List(r1, r2), schedule))
          }
        }
      }
    }

    for (v: VehicleAndSchedule <- supply.withFilter(_.getFreeSeats >= 1)) {
      val requestWithCurrentVehiclePosition = v.getRequestWithCurrentVehiclePosition
      val center = requestWithCurrentVehiclePosition.activity.getCoord
      val demand = spatialDemand
        .getDisk(center.getX, center.getY, searchRadius)
        .asScala
        .toList
        .sortBy(r => GeoUtils.minkowskiDistFormula(center, r.pickup.activity.getCoord))
        .take(solutionSpaceSizePerVehicle)
      for (r: CustomerRequest <- demand) {
        getRidehailSchedule(v.schedule, List(r.pickup, r.dropoff), v.vehicleRemainingRangeInMeters.toInt, skimmer).map {
          schedule =>
            rvG.addVertex(v)
            rvG.addVertex(r)
            rvG.addEdge(v, r, RideHailTrip(List(r), schedule))
        }
      }
    }
    rvG
  }

  // Request Trip Vehicle Graph
  private def rTVGraph(rvG: RVGraph): RTVGraph = {
    val rTvG = RTVGraph(classOf[DefaultEdge])

    for (v <- supply.filter(rvG.containsVertex)) {
      rTvG.addVertex(v)
      val finalRequestsList: ListBuffer[RideHailTrip] = ListBuffer.empty[RideHailTrip]
      val individualRequestsList = ListBuffer.empty[RideHailTrip]
      for (t <- rvG.outgoingEdgesOf(v).asScala) {
        individualRequestsList.append(t)
        rTvG.addVertex(t)
        rTvG.addVertex(t.requests.head)
        rTvG.addEdge(t.requests.head, t)
        rTvG.addEdge(t, v)
      }
      finalRequestsList.appendAll(individualRequestsList)

      if (v.getFreeSeats > 1) {
        val pairRequestsList = ListBuffer.empty[RideHailTrip]
        for (t1 <- individualRequestsList) {
          for (t2 <- individualRequestsList
                 .drop(individualRequestsList.indexOf(t1))
                 .filter(x => rvG.containsEdge(t1.requests.head, x.requests.head))) {
            getRidehailSchedule(
              v.schedule,
              (t1.requests ++ t2.requests).flatMap(x => List(x.pickup, x.dropoff)),
              v.vehicleRemainingRangeInMeters.toInt,
              skimmer
            ) map { schedule =>
              val t = RideHailTrip(t1.requests ++ t2.requests, schedule)
              pairRequestsList append t
              rTvG.addVertex(t)
              rTvG.addEdge(t1.requests.head, t)
              rTvG.addEdge(t2.requests.head, t)
              rTvG.addEdge(t, v)
            }
          }
        }
        finalRequestsList.appendAll(pairRequestsList)

        for (k <- 3 to v.getFreeSeats) {
          val kRequestsList = ListBuffer.empty[RideHailTrip]
          for (t1 <- finalRequestsList) {
            for (t2 <- finalRequestsList
                   .drop(finalRequestsList.indexOf(t1))
                   .filter(
                     x =>
                       !(x.requests exists (s => t1.requests contains s)) && (t1.requests.size + x.requests.size) == k
                   )) {
              getRidehailSchedule(
                v.schedule,
                (t1.requests ++ t2.requests).flatMap(x => List(x.pickup, x.dropoff)),
                v.vehicleRemainingRangeInMeters.toInt,
                skimmer
              ).map { schedule =>
                val t = RideHailTrip(t1.requests ++ t2.requests, schedule)
                kRequestsList.append(t)
                rTvG.addVertex(t)
                t.requests.foreach(rTvG.addEdge(_, t))
                rTvG.addEdge(t, v)
              }
            }
          }
          finalRequestsList.appendAll(kRequestsList)
        }
      }
    }
    rTvG
  }

  // a greedy assignment using a cost function
  def matchAndAssign(tick: Int): Future[List[(RideHailTrip, VehicleAndSchedule, Double)]] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    Future {
      val rvG = pairwiseRVGraph
      val rTvG = rTVGraph(rvG)
      val V: Int = supply.foldLeft(0) { case (maxCapacity, v) => Math max (maxCapacity, v.getFreeSeats) }
      val assignment = greedyAssignment(rTvG, V)
      assignment
    }
  }

}

object AlonsoMoraPoolingAlgForRideHail {

  def computeCost(trip: RideHailTrip, vehicle: VehicleAndSchedule): Double = {
    val empty = vehicle.getSeatingCapacity - trip.requests.size - vehicle.getNoPassengers
    val largeConstant = trip.upperBoundDelays
    val cost = trip.sumOfDelays + empty * largeConstant
    cost
  }

  def checkDistance(r: MobilityRequest, schedule: List[MobilityRequest], searchRadius: Double): Boolean = {
    schedule.foreach { s =>
      if (GeoUtils.distFormula(r.activity.getCoord, s.activity.getCoord) <= searchRadius)
        return true
    }
    false
  }

  // a greedy assignment using a cost function
  def greedyAssignment(
    rTvG: RTVGraph,
    maximumVehCapacity: Int
  ): List[(RideHailTrip, VehicleAndSchedule, Double)] = {
    val Rok = collection.mutable.HashSet.empty[CustomerRequest]
    val Vok = collection.mutable.HashSet.empty[VehicleAndSchedule]
    val greedyAssignmentList = ListBuffer.empty[(RideHailTrip, VehicleAndSchedule, Double)]
    for (k <- maximumVehCapacity to 1 by -1) {
      val sortedList = rTvG
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
          val cost = AlonsoMoraPoolingAlgForRideHail.computeCost(trip, vehicle)
          (trip, vehicle, cost)
        }
        .toList
        .sortBy(_._3)

      sortedList.foreach {
        case (trip, vehicle, cost) if !(Vok contains vehicle) && !(trip.requests exists (r => Rok contains r)) =>
          trip.requests.foreach(Rok.add)
          Vok.add(vehicle)
          greedyAssignmentList.append((trip, vehicle, cost))
        case _ =>
      }
    }

    greedyAssignmentList.toList
  }

//  def optimalAssignment(
//    rTvG: RTVGraph,
//    maximumVehCapacity: Int
//  ): List[(RideHailTrip, VehicleAndSchedule, Double)] = {
//
//    val trips = rTvG
//      .vertexSet()
//      .asScala
//      .filter(t => t.isInstanceOf[RideHailTrip])
//      .map { t =>
//        val trip = t.asInstanceOf[RideHailTrip]
//        val vehicle = rTvG
//          .getEdgeTarget(
//            rTvG
//              .outgoingEdgesOf(trip)
//              .asScala
//              .filter(e => rTvG.getEdgeTarget(e).isInstanceOf[VehicleAndSchedule])
//              .head
//          )
//          .asInstanceOf[VehicleAndSchedule]
//        val cost = AlonsoMoraPoolingAlgForRideHail.computeCost(trip, vehicle)
//        (trip, vehicle, cost)
//      }
//      .toList
//
//    val combinations = new mutable.ListBuffer[List[(RideHailTrip, VehicleAndSchedule, Double)]]
//
//    for(trip <- trips) {
//      val tempComb = new mutable.ListBuffer[List[(RideHailTrip, VehicleAndSchedule, Double)]]
//      for(comb <- combinations) {
//        val alternatives = comb.filter(_._1 == trip._1)
//        if(alternatives.isEmpty) {
//          tempComb.append(comb ++ List(trip))
//        } else {
//
//        }
//      }
//    }
//
//
//
//    val Rok = collection.mutable.HashSet.empty[CustomerRequest]
//    val Vok = collection.mutable.HashSet.empty[VehicleAndSchedule]
//    val greedyAssignmentList = ListBuffer.empty[(RideHailTrip, VehicleAndSchedule, Double)]
//    for (k <- maximumVehCapacity to 1 by -1) {
//      val sortedList = rTvG
//        .vertexSet()
//        .asScala
//        .filter(t => t.isInstanceOf[RideHailTrip] && t.asInstanceOf[RideHailTrip].requests.size == k)
//        .map { t =>
//          val trip = t.asInstanceOf[RideHailTrip]
//          val vehicle = rTvG
//            .getEdgeTarget(
//              rTvG
//                .outgoingEdgesOf(trip)
//                .asScala
//                .filter(e => rTvG.getEdgeTarget(e).isInstanceOf[VehicleAndSchedule])
//                .head
//            )
//            .asInstanceOf[VehicleAndSchedule]
//          val cost = AlonsoMoraPoolingAlgForRideHail.computeCost(trip, vehicle)
//          (trip, vehicle, cost)
//        }
//        .toList
//        .sortBy(_._3)
//
//      sortedList.foreach {
//        case (trip, vehicle, cost) if !(Vok contains vehicle) && !(trip.requests exists (r => Rok contains r)) =>
//          trip.requests.foreach(Rok.add)
//          Vok.add(vehicle)
//          greedyAssignmentList.append((trip, vehicle, cost))
//        case _ =>
//      }
//    }
//
//    greedyAssignmentList.toList
//  }

  // ************ Helper functions ************
  def getTimeDistanceAndCost(src: MobilityRequest, dst: MobilityRequest, skimmer: BeamSkimmer): Skim = {
    skimmer.getTimeDistanceAndCost(
      src.activity.getCoord,
      dst.activity.getCoord,
      src.baselineNonPooledTime,
      BeamMode.CAR,
      Id.create(
        skimmer.beamScenario.beamConfig.beam.agentsim.agents.rideHail.initialization.procedural.vehicleTypeId,
        classOf[BeamVehicleType]
      )
    )
  }

  def getRidehailSchedule(
    schedule: List[MobilityRequest],
    newRequests: List[MobilityRequest],
    remainingVehicleRangeInMeters: Int,
    skimmer: BeamSkimmer
  ): Option[List[MobilityRequest]] = {
    val newPoolingList = scala.collection.mutable.ListBuffer.empty[MobilityRequest]
    val reversedSchedule = schedule.reverse
    val sortedRequests = reversedSchedule.lastOption match {
      case Some(_) if reversedSchedule.exists(_.tag == EnRoute) =>
        val enRouteIndex = reversedSchedule.indexWhere(_.tag == EnRoute) + 1
        newPoolingList.appendAll(reversedSchedule.slice(0, enRouteIndex))
        // We make sure that request time is always equal or greater than the driver's "current tick" as denoted by time in EnRoute
        val shiftRequestsBy =
          Math.max(0, reversedSchedule(enRouteIndex - 1).baselineNonPooledTime - newRequests.head.baselineNonPooledTime)
        (reversedSchedule.slice(enRouteIndex, reversedSchedule.size) ++ newRequests.map(
          req =>
            req.copy(
              baselineNonPooledTime = req.baselineNonPooledTime + shiftRequestsBy,
              serviceTime = req.serviceTime + shiftRequestsBy
          )
        )).sortBy(
          mr => (mr.baselineNonPooledTime, mr.person.map(_.personId.toString).getOrElse("ZZZZZZZZZZZZZZZZZZZZZZZ"))
        )
      case Some(_) =>
        newPoolingList.appendAll(reversedSchedule)
        newRequests.sortBy(_.baselineNonPooledTime)
      case None =>
        val temp = newRequests.sortBy(_.baselineNonPooledTime)
        newPoolingList.append(temp.head)
        temp.drop(1)
    }
    sortedRequests.foreach { curReq =>
      val prevReq = newPoolingList.lastOption.getOrElse(newPoolingList.last)
      val tdc = getTimeDistanceAndCost(prevReq, curReq, skimmer)
      val serviceTime = prevReq.serviceTime + tdc.time
      val serviceDistance = prevReq.serviceDistance + tdc.distance.toInt
      if (serviceTime <= curReq.upperBoundTime && serviceDistance <= remainingVehicleRangeInMeters) {
        newPoolingList.append(curReq.copy(serviceTime = serviceTime, serviceDistance = serviceDistance))
      } else {
        return None
      }
    }
    Some(newPoolingList.toList)
  }

  def createPersonRequest(
    vehiclePersonId: PersonIdWithActorRef,
    src: Location,
    departureTime: Int,
    dst: Location,
    beamServices: BeamServices
  )(
    implicit skimmer: BeamSkimmer
  ): CustomerRequest = {
    val alonsoMora: AllocationManager.AlonsoMora =
      beamServices.beamConfig.beam.agentsim.agents.rideHail.allocationManager.alonsoMora
    val waitingTimeInSec = alonsoMora.waitingTimeInSec
    val travelTimeDelayAsFraction = alonsoMora.travelTimeDelayAsFraction

    val p1Act1: Activity = PopulationUtils.createActivityFromCoord(s"${vehiclePersonId.personId}Act1", src)
    p1Act1.setEndTime(departureTime)
    val p1Act2: Activity = PopulationUtils.createActivityFromCoord(s"${vehiclePersonId.personId}Act2", dst)
    val skim = skimmer
      .getTimeDistanceAndCost(
        p1Act1.getCoord,
        p1Act2.getCoord,
        departureTime,
        BeamMode.CAR,
        Id.create(
          skimmer.beamScenario.beamConfig.beam.agentsim.agents.rideHail.initialization.procedural.vehicleTypeId,
          classOf[BeamVehicleType]
        )
      )
    CustomerRequest(
      vehiclePersonId,
      MobilityRequest(
        Some(vehiclePersonId),
        p1Act1,
        departureTime,
        Trip(p1Act1, None, null),
        BeamMode.RIDE_HAIL,
        Pickup,
        departureTime,
        departureTime + waitingTimeInSec,
        0
      ),
      MobilityRequest(
        Some(vehiclePersonId),
        p1Act2,
        departureTime + skim.time,
        Trip(p1Act2, None, null),
        BeamMode.RIDE_HAIL,
        Dropoff,
        departureTime + skim.time,
        Math.round(departureTime + skim.time + waitingTimeInSec + travelTimeDelayAsFraction * skim.time).toInt,
        skim.distance.toInt
      )
    )
  }

  def createVehicleAndScheduleFromRideHailAgentLocation(
    veh: RideHailAgentLocation,
    tick: Int,
    beamServices: BeamServices,
    remainingRangeInMeters: Double
  ): VehicleAndSchedule = {
    val v1 = new BeamVehicle(
      Id.create(veh.vehicleId, classOf[BeamVehicle]),
      new Powertrain(0.0),
      veh.vehicleType
    )
    val vehCurrentLocation =
      veh.currentPassengerSchedule.map(_.locationAtTime(tick, beamServices)).getOrElse(veh.currentLocationUTM.loc)
    val v1Act0: Activity = PopulationUtils.createActivityFromCoord(s"${veh.vehicleId}Act0", vehCurrentLocation)
    v1Act0.setEndTime(tick)
    var alonsoSchedule: ListBuffer[MobilityRequest] = ListBuffer()

    val alonsoMora: AllocationManager.AlonsoMora =
      beamServices.beamConfig.beam.agentsim.agents.rideHail.allocationManager.alonsoMora
    val waitingTimeInSec = alonsoMora.waitingTimeInSec
    val travelTimeDelayAsFraction = alonsoMora.travelTimeDelayAsFraction

    veh.currentPassengerSchedule.foreach {
      _.schedule.foreach {
        case (leg, manifest) =>
          if (manifest.riders.isEmpty) {
            val theActivity = PopulationUtils.createActivityFromCoord(
              s"${veh.vehicleId}Act0",
              beamServices.geo.wgs2Utm(leg.travelPath.startPoint.loc)
            )
            alonsoSchedule = ListBuffer(
              MobilityRequest(
                None,
                theActivity,
                leg.startTime,
                Trip(theActivity, None, null),
                BeamMode.RIDE_HAIL,
                Relocation,
                leg.startTime,
                leg.startTime + Int.MaxValue - 30000000,
                0
              )
            )
          } else {
            val thePickups = manifest.boarders.map { boarder =>
              val theActivity = PopulationUtils.createActivityFromCoord(
                s"${veh.vehicleId}Act0",
                beamServices.geo.wgs2Utm(leg.travelPath.startPoint.loc)
              )
              theActivity.setEndTime(leg.startTime)
              MobilityRequest(
                Some(boarder),
                theActivity,
                leg.startTime,
                Trip(theActivity, None, null),
                BeamMode.RIDE_HAIL,
                Pickup,
                leg.startTime,
                leg.startTime + waitingTimeInSec,
                0
              )
            }
            val theDropoffs = manifest.alighters.map { alighter =>
              val theActivity = PopulationUtils.createActivityFromCoord(
                s"${veh.vehicleId}Act0",
                beamServices.geo.wgs2Utm(leg.travelPath.endPoint.loc)
              )
              theActivity.setEndTime(leg.endTime)
              MobilityRequest(
                Some(alighter),
                theActivity,
                leg.endTime,
                Trip(theActivity, None, null),
                BeamMode.RIDE_HAIL,
                Dropoff,
                leg.endTime,
                Math
                  .round(leg.endTime + waitingTimeInSec + (leg.endTime - leg.startTime) * travelTimeDelayAsFraction)
                  .toInt,
                leg.travelPath.distanceInM.toInt
              )
            }
            alonsoSchedule ++= thePickups ++ theDropoffs
          }
      }
    }
    if (alonsoSchedule.isEmpty) {
      alonsoSchedule += MobilityRequest(
        None,
        v1Act0,
        tick,
        Trip(v1Act0, None, null),
        BeamMode.RIDE_HAIL,
        Dropoff,
        tick,
        tick,
        0
      )
    } else {
      alonsoSchedule += MobilityRequest(
        None,
        v1Act0,
        tick + 1,
        Trip(v1Act0, None, null),
        BeamMode.RIDE_HAIL,
        EnRoute,
        tick,
        tick + Int.MaxValue - 30000000,
        0
      )
    }
    val res = VehicleAndSchedule(
      v1,
      alonsoSchedule
        .sortBy(mr => (mr.baselineNonPooledTime, mr.person.map(_.personId.toString).getOrElse("")))
        .reverse
        .toList,
      veh.geofence,
      remainingRangeInMeters
    )
    res
  }

  def createVehicleAndSchedule(
    vid: String,
    vehicleType: BeamVehicleType,
    dst: Location,
    dstTime: Int,
    geofence: Option[Geofence] = None,
    seatsAvailable: Int
  ): VehicleAndSchedule = {
    val v1 = new BeamVehicle(
      Id.create(vid, classOf[BeamVehicle]),
      new Powertrain(0.0),
      vehicleType
    )
    val v1Act0: Activity = PopulationUtils.createActivityFromCoord(s"${vid}Act0", dst)
    v1Act0.setEndTime(dstTime)
    VehicleAndSchedule(
      v1,
      List(
        MobilityRequest(
          None,
          v1Act0,
          dstTime,
          Trip(v1Act0, None, null),
          BeamMode.RIDE_HAIL,
          Dropoff,
          dstTime,
          dstTime,
          0
        )
      ),
      geofence,
      seatsAvailable
    )

  }

  // ***** Graph Structure *****
  sealed trait RTVGraphNode {
    def getId: String
    override def toString: String = s"[$getId]"
  }
  sealed trait RVGraphNode extends RTVGraphNode
  // customer requests
  case class CustomerRequest(person: PersonIdWithActorRef, pickup: MobilityRequest, dropoff: MobilityRequest)
      extends RVGraphNode {
    override def getId: String = person.personId.toString
    override def toString: String = s"Person:${person.personId}|Pickup:$pickup|Dropoff:$dropoff"
  }
  // Ride Hail vehicles, capacity and their predefined schedule
  case class VehicleAndSchedule(
    vehicle: BeamVehicle,
    schedule: List[MobilityRequest],
    geofence: Option[Geofence],
    vehicleRemainingRangeInMeters: Double = Double.MaxValue
  ) extends RVGraphNode {
    private val numberOfPassengers: Int =
      schedule.takeWhile(_.tag != EnRoute).count(req => req.person.isDefined && req.tag == Dropoff)
    private val seatingCapacity: Int = vehicle.beamVehicleType.seatingCapacity
    override def getId: String = vehicle.id.toString
    def getNoPassengers: Int = numberOfPassengers
    def getSeatingCapacity: Int = seatingCapacity
    def getFreeSeats: Int = seatingCapacity - numberOfPassengers
    def getRequestWithCurrentVehiclePosition: MobilityRequest = schedule.find(_.tag == EnRoute).getOrElse(schedule.head)
  }
  // Trip that can be satisfied by one or more ride hail vehicle
  case class RideHailTrip(requests: List[CustomerRequest], schedule: List[MobilityRequest])
      extends DefaultEdge
      with RTVGraphNode {
    var sumOfDelays: Int = 0
    var upperBoundDelays: Int = 0
    schedule.foreach { s =>
      sumOfDelays += (s.serviceTime - s.baselineNonPooledTime)
      upperBoundDelays += (s.upperBoundTime - s.baselineNonPooledTime)
    }
    //val sumOfDelaysAsFraction: Double = sumOfDelays / upperBoundDelays.toDouble

    override def getId: String = requests.foldLeft(s"trip:") { case (c, x) => c + s"$x -> " }
    override def toString: String =
      s"${requests.size} requests and this schedule: ${schedule.map(_.toString).mkString("\n")}"
  }
  case class RVGraph(clazz: Class[RideHailTrip])
      extends DefaultUndirectedWeightedGraph[RVGraphNode, RideHailTrip](clazz)
  case class RTVGraph(clazz: Class[DefaultEdge])
      extends DefaultUndirectedWeightedGraph[RTVGraphNode, DefaultEdge](clazz)
  // ***************************

}
