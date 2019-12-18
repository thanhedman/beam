package beam.agentsim.agents.ridehail

import beam.agentsim.agents.EnRoute
import beam.agentsim.agents.ridehail.AlonsoMoraPoolingAlgForRideHail._
import beam.router.BeamSkimmer
import beam.router.Modes.BeamMode
import beam.sim.BeamServices
import beam.sim.common.GeoUtils
import com.vividsolutions.jts.geom.{Coordinate, GeometryFactory}
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
  private val maxSolutionAlternativeForKPassengers: Int = Int.MaxValue
  private val maxAngleDirection: Int = 45

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
        .filter (
          r => mainTasks.foldLeft(false) { (acc, t) =>
            acc || (t.pickupRequest match {
              case Some(pickup) =>
                getAngle(pickup.activity.getCoord, t.activity.getCoord, r.dropoff.activity.getCoord) <= maxAngleDirection
              case _ =>
                false
            })
          }
        )
        .sortBy(r => GeoUtils.minkowskiDistFormula(center, r.pickup.activity.getCoord))
    } else {
      // if vehicle is empty, prioritize the destination of the current closest customers
      customers = customers.sortBy(r => GeoUtils.minkowskiDistFormula(center, r.pickup.activity.getCoord))
      val mainRequests = customers.slice(0, Math.min(customers.size, 1))
      customers = mainRequests ::: customers
        .drop(mainRequests.size)
        .filter (
          r => mainRequests.foldLeft(false) { (acc, t) =>
            acc || getAngle(t.pickup.activity.getCoord, t.dropoff.activity.getCoord, r.dropoff.activity.getCoord) <= maxAngleDirection
          }
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

  private def angleBetween2Lines(org1: Coord, dest1: Coord, org2: Coord, dest2: Coord): Double = {
    val angle1: Double = Math.atan2(dest1.getY - org1.getY, dest1.getX - org1.getX)
    val angle2: Double = Math.atan2(dest2.getY - org2.getY, dest2.getX - org2.getX)
    val degrees = Math.toDegrees(angle1 - angle2)
    Math.abs(degrees)
  }

  private def getAngle(origin: Coord, dest1: Coord, dest2: Coord): Double = {
    import org.geotools.referencing.GeodeticCalculator
    import org.geotools.referencing.crs.DefaultGeographicCRS
    val crs = DefaultGeographicCRS.WGS84
    //val crs = MGC.getCRS(services.beamConfig.beam.spatial.localCRS)
    val orgWgs = services.geo.utm2Wgs.transform(origin)
    val dst1Wgs = services.geo.utm2Wgs.transform(dest1)
    val dst2Wgs = services.geo.utm2Wgs.transform(dest2)
    //val crs = CRS.decode(services.beamConfig.beam.spatial.localCRS)
    val calc = new GeodeticCalculator(crs)
    val gf = new GeometryFactory()
    val point1 = gf.createPoint(new Coordinate(orgWgs.getX, orgWgs.getY))
    calc.setStartingGeographicPoint(point1.getX, point1.getY)
    val point2 = gf.createPoint(new Coordinate(dst1Wgs.getX, dst1Wgs.getY))
    calc.setDestinationGeographicPoint(point2.getX, point2.getY)
    val azimuth1 = calc.getAzimuth
    val point3 = gf.createPoint(new Coordinate(dst2Wgs.getX, dst2Wgs.getY))
    calc.setDestinationGeographicPoint(point3.getX, point3.getY)
    val azimuth2 = calc.getAzimuth
    val degrees = azimuth2 - azimuth1
    Math.abs(degrees)
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
    val passengers = trip.requests.size + vehicle.getNoPassengers
    val delay = trip.sumOfDelays
    val maximum_delay = trip.upperBoundDelays
    val cost = passengers + (1 - delay/maximum_delay.toDouble)
    -1 * cost
  }

  def computeCost3(trip: RideHailTrip, vehicle: VehicleAndSchedule, alpha: Double, beta: Double): Double = {
    val passengers = trip.requests.size + vehicle.getNoPassengers
    val capacity = vehicle.getSeatingCapacity
    val delay = trip.sumOfDelays
    val maximum_delay = trip.upperBoundDelays
    val cost = alpha * (1-(passengers/capacity.toDouble)) + beta * delay/maximum_delay.toDouble
    cost
  }

  def computeCost3_50to50(trip: RideHailTrip, vehicle: VehicleAndSchedule) = computeCost3(trip, vehicle, 0.50, 0.50)
  def computeCost3_60to40(trip: RideHailTrip, vehicle: VehicleAndSchedule) = computeCost3(trip, vehicle, 0.60, 0.40)
  def computeCost3_75to25(trip: RideHailTrip, vehicle: VehicleAndSchedule) = computeCost3(trip, vehicle, 0.75, 0.25)
  def computeCost3_95to05(trip: RideHailTrip, vehicle: VehicleAndSchedule) = computeCost3(trip, vehicle, 0.95, 0.05)

}
