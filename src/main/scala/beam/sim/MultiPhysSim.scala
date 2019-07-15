package beam.sim

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import beam.agentsim.agents.vehicles.EnergyEconomyAttributes.Powertrain
import beam.agentsim.agents.vehicles.VehicleProtocol.StreetVehicle
import beam.agentsim.agents.vehicles.{BeamVehicle, BeamVehicleType}
import beam.agentsim.events.SpaceTime
import beam.router.BeamRouter.{Location, RoutingFailure, RoutingRequest, RoutingResponse}
import beam.router.Modes.BeamMode.WALK
import beam.sim.MultiPhysSim.{MultiPhysSimFinished, StartMultiPhysSim, WaitToFinish}
import org.matsim.api.core.v01.population.Activity
import org.matsim.api.core.v01.{Id, Scenario}

import scala.collection.JavaConverters._
import scala.concurrent.duration._

object MultiPhysSim {
  final case object StartMultiPhysSim
  final case object WaitToFinish
  final case class MultiPhysSimFinished(duration: FiniteDuration)

  def props(beamServices: BeamServices): Props = {
    Props(new MultiPhysSim(beamServices))
  }
}

class MultiPhysSim(beamServices: BeamServices) extends Actor with ActorLogging {
  val scenario: Scenario = beamServices.matsimServices.getScenario

  val personToActivities = scenario.getPopulation.getPersons
    .values()
    .asScala
    .map { person =>
      val activities = person.getPlans.asScala.flatMap { plan =>
        plan.getPlanElements.asScala.collect {
          case activity: Activity if activity.getEndTime.toInt > 0 =>
            activity
        }
      }
      person -> activities
    }
    .toVector

  val router: ActorRef = beamServices.beamRouter

  var requestIdToResponse: Map[Int, RoutingResponse] = Map.empty
  var requestIdToFailedResponse: Map[Int, RoutingFailure] = Map.empty

  var sentRequests: Int = 0

  var started: Long = 0

  val bodyStreetVehicle: StreetVehicle = {
    val bodyType: BeamVehicleType = beamServices.beamScenario.vehicleTypes(
      Id.create(beamServices.beamScenario.beamConfig.beam.agentsim.agents.bodyType, classOf[BeamVehicleType])
    )
    val body: BeamVehicle = new BeamVehicle(
      BeamVehicle.createId("MultiPhysSim", Some("body")),
      new Powertrain(bodyType.primaryFuelConsumptionInJoulePerMeter),
      bodyType
    )
    StreetVehicle(body.id, body.beamVehicleType.id, new SpaceTime(new Location(0, 0), 0), WALK, asDriver = true)
  }

  var replyTo: Option[ActorRef] = None

  val replanningRate: Double = 0.1

  override def receive: Receive = {
    case StartMultiPhysSim =>
      started = System.currentTimeMillis()
      // FIXME Pre-calculate how many request will be sent
      val replanningList = personToActivities.take((personToActivities.size * replanningRate).toInt)
      sentRequests = replanningList.size

      replanningList.foreach {
        case (p, activities) =>
          require(activities.size >= 2)
          // FIXME
          val from = activities(0).getCoord
          val to = activities(1).getCoord
          // FIXME
          val withTransit: Boolean = false
          val routingRequest = RoutingRequest(
            from,
            to,
            activities(0).getEndTime.toInt,
            withTransit,
            Vector(bodyStreetVehicle)
          )
          router ! routingRequest
      }
    case routingResponse: RoutingResponse =>
      requestIdToResponse = requestIdToResponse.updated(routingResponse.requestId, routingResponse)
      checkAndContinue()
    case routingFailure: RoutingFailure =>
      log.error(
        routingFailure.cause,
        s"Request with id ${routingFailure.requestId} failed: ${routingFailure.cause.getMessage}"
      )
      requestIdToFailedResponse = requestIdToFailedResponse.updated(routingFailure.requestId, routingFailure)
    case WaitToFinish =>
      replyTo = Some(sender())
  }

  def checkAndContinue(): Unit = {
    val areRoutesReady = sentRequests == requestIdToResponse.size + requestIdToFailedResponse.size
    log.info("sentRequests: {}, requestIdToResponse.size: {}", sentRequests, requestIdToResponse.size)
    if (areRoutesReady) {
      log.info(s"Received all routing responses: ${requestIdToResponse}")
      log.info(s"Replying back to $replyTo")
      // We can do PhysSimHere or reply back routing responses to the caller and let him decide what to do next

      // And reply back that it is done.
      replyTo.foreach(
        ref => ref ! MultiPhysSimFinished(FiniteDuration(System.currentTimeMillis() - started, TimeUnit.MILLISECONDS))
      )
    }
  }
}
