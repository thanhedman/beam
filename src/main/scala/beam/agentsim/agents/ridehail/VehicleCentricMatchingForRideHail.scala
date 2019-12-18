package beam.agentsim.agents.ridehail

import beam.agentsim.agents.EnRoute
import beam.agentsim.agents.ridehail.AlonsoMoraPoolingAlgForRideHail._
import beam.router.BeamSkimmer
import beam.router.Modes.BeamMode
import beam.sim.BeamServices
import beam.sim.common.GeoUtils
import org.matsim.api.core.v01.Coord
import org.matsim.core.utils.collections.QuadTree

import scala.collection.JavaConverters._
import scala.collection.immutable.List
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class VehicleCentricMatchingForRideHail(
  demand: QuadTree[CustomerRequest],
  supply: List[VehicleAndSchedule],
  services: BeamServices,
  skimmer: BeamSkimmer
) {
  private val solutionSpaceSizePerVehicle =
    services.beamConfig.beam.agentsim.agents.rideHail.allocationManager.alonsoMora.solutionSpaceSizePerVehicle
  private val waitingTimeInSec =
    services.beamConfig.beam.agentsim.agents.rideHail.allocationManager.alonsoMora.waitingTimeInSec
  private val searchRadius = waitingTimeInSec * BeamSkimmer.speedMeterPerSec(BeamMode.CAV)

  type AssignmentKey = (RideHailTrip, VehicleAndSchedule, Double)
  private val maxSolutionAlternativeForKPassengers: Int = 2

  def matchAndAssign(tick: Int): Future[List[AssignmentKey]] = {
    Future
      .sequence(supply.withFilter(_.getFreeSeats >= 1).map { v =>
        Future { vehicleCentricMatching(v) }
      })
      .map(result => getAssignment(result.flatten))
      .recover {
        case e =>
          println(e.getMessage)
          List.empty[AssignmentKey]
      }
  }

  private def vehicleCentricMatching(
    v: VehicleAndSchedule
  ): List[AssignmentKey] = {
    val requestWithCurrentVehiclePosition = v.getRequestWithCurrentVehiclePosition
    val center = requestWithCurrentVehiclePosition.activity.getCoord

    // get all customer requests located at a proximity to the vehicle
    var customers = v.geofence match {
      case Some(gf) =>
        val gfCenter = new Coord(gf.geofenceX, gf.geofenceY)
        demand
          .getDisk(center.getX, center.getY, searchRadius)
          .asScala
          .filter(
            r =>
              GeoUtils.distFormula(r.pickup.activity.getCoord, gfCenter) <= gf.geofenceRadius &&
              GeoUtils.distFormula(r.dropoff.activity.getCoord, gfCenter) <= gf.geofenceRadius
          )
          .toList
      case _ =>
        demand.getDisk(center.getX, center.getY, searchRadius).asScala.toList
    }

    // if no customer found, returns
    if (customers.isEmpty)
      return List.empty[AssignmentKey]

    if (requestWithCurrentVehiclePosition.tag == EnRoute) {
      // if vehicle is EnRoute, then filter list of customer based on the destination of the passengers
      val i = v.schedule.indexWhere(_.tag == EnRoute)
      val mainTasks = v.schedule.slice(0, i)
      customers = customers
        .filter(r => AlonsoMoraPoolingAlgForRideHail.checkDistance(r.dropoff, mainTasks, searchRadius))
        .sortBy(r => GeoUtils.minkowskiDistFormula(center, r.pickup.activity.getCoord))
    } else {
      // if vehicle is empty, prioritize the destination of the current closest customers
      customers = customers.sortBy(r => GeoUtils.minkowskiDistFormula(center, r.pickup.activity.getCoord))
      val mainRequests = customers.slice(0, Math.max(customers.size, 1))
      customers = mainRequests ::: customers
        .drop(mainRequests.size)
        .filter(
          r => AlonsoMoraPoolingAlgForRideHail.checkDistance(r.dropoff, mainRequests.map(_.dropoff), searchRadius)
        )
    }

    val potentialTrips = mutable.ListBuffer.empty[AssignmentKey]
    // consider solo rides as initial potential trips
    customers
      .take(solutionSpaceSizePerVehicle)
      .flatten(
        c =>
          getRidehailSchedule(v.schedule, List(c.pickup, c.dropoff), v.vehicleRemainingRangeInMeters.toInt, skimmer)
            .map(schedule => (c, schedule))
      )
      .foreach {
        case (c, schedule) =>
          val trip = RideHailTrip(List(c), schedule)
          potentialTrips.append((trip, v, getCost(trip, v)))
      }

    // if no solo ride is possible, returns
    if (potentialTrips.isEmpty)
      return List.empty[AssignmentKey]

    // building pooled rides from bottom up
    val numPassengers = v.getFreeSeats
    for (k <- 2 to numPassengers) {
      val tripsWithKPassengers = mutable.ListBuffer.empty[AssignmentKey]
      potentialTrips.zipWithIndex.foreach {
        case ((t1, _, _), pt1_index) =>
          potentialTrips
            .drop(pt1_index)
            .filter {
              case (t2, _, _) =>
                !(t2.requests exists (s => t1.requests contains s)) && (t1.requests.size + t2.requests.size) == k
            }
            .foreach {
              case (t2, _, _) =>
                val requests = t1.requests ++ t2.requests
                getRidehailSchedule(
                  v.schedule,
                  requests.flatMap(x => List(x.pickup, x.dropoff)),
                  v.vehicleRemainingRangeInMeters.toInt,
                  skimmer
                ) match {
                  case Some(schedule) =>
                    val t = RideHailTrip(requests, schedule)
                    val cost = getCost(t, v)
                    if (tripsWithKPassengers.size == maxSolutionAlternativeForKPassengers) {
                      // then replace the trip with highest sum of delays
                      val ((_, _, tripWithHighestCost), index) = tripsWithKPassengers.zipWithIndex.maxBy(_._1._3)
                      if (tripWithHighestCost > cost) {
                        tripsWithKPassengers.remove(index)
                      }
                    }
                    if(tripsWithKPassengers.size < maxSolutionAlternativeForKPassengers) {
                      tripsWithKPassengers.append((t, v, cost))
                    }
                  case _ =>
                }
            }
      }
      potentialTrips.appendAll(tripsWithKPassengers)
    }
    potentialTrips.toList
  }

  private def getCost(trip: RideHailTrip, vehicle: VehicleAndSchedule): Double = {
    computeCost2(trip, vehicle)
  }

  private def getAssignment(trips: List[AssignmentKey]): List[AssignmentKey] = {
    greedyAssignment2(trips)
  }

  private def greedyAssignment(trips: List[AssignmentKey]): List[AssignmentKey] = {
    val greedyAssignmentList = mutable.ListBuffer.empty[AssignmentKey]
    var tripsByPoolSize = trips.sortBy(-_._1.requests.size)
    while (tripsByPoolSize.nonEmpty) {
      val maxPool = tripsByPoolSize.head._1.requests.size
      val Rok = collection.mutable.HashSet.empty[CustomerRequest]
      val Vok = collection.mutable.HashSet.empty[VehicleAndSchedule]
      val tripsWithPoolSizeMaxPool = tripsByPoolSize.filter(_._1.requests.size == maxPool).sortBy(_._3)
      for((trip, vehicle, cost) <- tripsWithPoolSizeMaxPool) {
        if(!(Vok contains vehicle) && !(trip.requests exists (r => Rok contains r))) {
          trip.requests.foreach(Rok.add)
          Vok.add(vehicle)
          greedyAssignmentList.append((trip, vehicle, cost))
        }
      }
      tripsByPoolSize = tripsByPoolSize.filter(t => !(Vok contains t._2) && t._1.requests.exists(r => Rok contains r))
    }
    greedyAssignmentList.toList
  }

  private def greedyAssignment2(trips: List[AssignmentKey]): List[AssignmentKey] = {
    val greedyAssignmentList = mutable.ListBuffer.empty[AssignmentKey]
    var tripsByPoolSize = trips.sortBy(_._3)
    while (tripsByPoolSize.nonEmpty) {
      val (trip, vehicle, cost) = tripsByPoolSize.head
      greedyAssignmentList.append((trip, vehicle, cost))
      tripsByPoolSize = tripsByPoolSize.filter(t => t._2 != vehicle && !t._1.requests.exists(trip.requests.contains))
    }
    greedyAssignmentList.toList
  }

  def computeCost2(trip: RideHailTrip, vehicle: VehicleAndSchedule): Double = {
    val alpha = 0.50
    val beta = 0.50
    val passengers = trip.requests.size + vehicle.getNoPassengers
    val capacity = vehicle.getSeatingCapacity
    val delay = trip.sumOfDelays
    val maximum_delay = trip.upperBoundDelays
    val cost = passengers + (1 - delay/maximum_delay.toDouble)
    -1 * cost
//    val cost = alpha * (1-(passengers/capacity.toDouble)) + beta * delay/maximum_delay.toDouble
//    cost
  }
}
