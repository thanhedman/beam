package beam.sim

import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import beam.agentsim.scheduler.BeamAgentScheduler.{ScheduleTrigger, StartSchedule}
import beam.agentsim.agents.PersonAgent.PersonData
import beam.agentsim.agents.TaxiAgent.TaxiData
import beam.agentsim.agents._
import beam.agentsim.agents.vehicles._
import beam.agentsim.agents.vehicles.household.HouseholdActor
import beam.agentsim.events.{EventsSubscriber, JsonFriendlyEventWriterXML, PathTraversalEvent, PointProcessEvent}
import beam.agentsim.scheduler.BeamAgentScheduler
import beam.router.RoutingMessages.InitializeRouter
import beam.utils.JsonUtils
import beam.physsim.{DummyPhysSim, InitializePhysSim}
import beam.router.DummyRouter
import com.google.inject.Inject
import glokka.Registry
import glokka.Registry.Created
import org.matsim.api.core.v01.events._
import org.matsim.api.core.v01.population.{Activity, Person}
import org.matsim.api.core.v01.{Coord, Id}
import org.matsim.core.api.experimental.events.{AgentWaitingForPtEvent, EventsManager, TeleportationArrivalEvent}
import org.matsim.core.controler.events.{IterationEndsEvent, IterationStartsEvent, ShutdownEvent, StartupEvent}
import org.matsim.core.controler.listener.{IterationEndsListener, IterationStartsListener, ShutdownListener, StartupListener}
import org.matsim.core.events.EventsUtils
import org.matsim.households.Household
import org.matsim.vehicles.Vehicle
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.immutable.ListMap
import scala.concurrent.Await
import scala.util.Random
import scala.collection.mutable
import scala.collection.JavaConverters._

/**
  * AgentSim entrypoint.
  * Should instantiate the [[ActorSystem]], [[BeamServices]] and interact concurrently w/ the QSim.
  *
  * Created by sfeygin on 2/8/17.
  */
class BeamSim @Inject()(private val actorSystem: ActorSystem,
                        private val services: BeamServices
                        ) extends StartupListener with IterationStartsListener with IterationEndsListener with ShutdownListener {

  private val logger: Logger = LoggerFactory.getLogger(classOf[BeamSim])
  val eventsManager: EventsManager = EventsUtils.createEventsManager()
  implicit val eventSubscriber: ActorRef = actorSystem.actorOf(Props(classOf[EventsSubscriber], eventsManager), "MATSimEventsManagerService")
  var writer: JsonFriendlyEventWriterXML = _
  var currentIter = 0
  var vehicleActors: Option[Map[Id[Vehicle], ActorRef]] = None
  var householdActors: Option[Map[Id[Household], ActorRef]] = None

  private implicit val timeout = Timeout(5000, TimeUnit.SECONDS)

  override def notifyStartup(event: StartupEvent): Unit = {
    val scenario = services.matsimServices.getScenario
    services.popMap = Some(ListMap(scala.collection.JavaConverters
      .mapAsScalaMap(scenario.getPopulation.getPersons).toSeq.sortBy(_._1): _*))
    services.vehicles = scenario.getVehicles.getVehicles.asScala.toMap
    services.households = scenario.getHouseholds.getHouseholds.asScala.toMap

    subscribe(ActivityEndEvent.EVENT_TYPE)
    subscribe(ActivityStartEvent.EVENT_TYPE)
    subscribe(PersonEntersVehicleEvent.EVENT_TYPE)
    subscribe(PersonLeavesVehicleEvent.EVENT_TYPE)
    subscribe(VehicleEntersTrafficEvent.EVENT_TYPE)
    subscribe(PathTraversalEvent.EVENT_TYPE)
    subscribe(VehicleLeavesTrafficEvent.EVENT_TYPE)
    subscribe(PersonDepartureEvent.EVENT_TYPE)
    subscribe(AgentWaitingForPtEvent.EVENT_TYPE)
    subscribe(TeleportationArrivalEvent.EVENT_TYPE)
    subscribe(PersonArrivalEvent.EVENT_TYPE)
    subscribe(PointProcessEvent.EVENT_TYPE)

    val schedulerFuture = services.registry ? Registry.Register("scheduler", Props(classOf[BeamAgentScheduler]))
    services.schedulerRef = Await.result(schedulerFuture, timeout.duration).asInstanceOf[Created].ref

    val routerFuture = services.registry ? Registry.Register("router", DummyRouter.props(services))
    services.beamRouter = Await.result(routerFuture, timeout.duration).asInstanceOf[Created].ref
    val routerInitFuture = services.beamRouter ? InitializeRouter
    Await.result(routerInitFuture, timeout.duration)

    val physSimFuture = services.registry ? Registry.Register("physSim", DummyPhysSim.props(services))
    services.physSim = Await.result(physSimFuture, timeout.duration).asInstanceOf[Created].ref
    val physSimInitFuture = services.physSim ? new InitializePhysSim()
    Await.result(physSimInitFuture, timeout.duration)

    val taxiManagerFuture = services.registry ? Registry.Register("taxiManager", TaxiManager.props(services))
    services.taxiManager = Await.result(taxiManagerFuture, timeout.duration).asInstanceOf[Created].ref

  }

  override def notifyIterationStarts(event: IterationStartsEvent): Unit = {
    // TODO replace magic numbers
    currentIter = event.getIteration
    writer = new JsonFriendlyEventWriterXML(services.matsimServices.getControlerIO.getIterationFilename(currentIter, "events.xml.gz"))
    eventsManager.addHandler(writer)
    resetPop(event.getIteration)
    eventsManager.initProcessing()
    Await.result(services.schedulerRef ? StartSchedule(3600*9.0, 300.0), timeout.duration)
  }

  override def notifyIterationEnds(event: IterationEndsEvent): Unit = {
    cleanupWriter()
    cleanupVehicle()
    cleanupHouseHolder()
  }

  private def cleanupWriter() = {
    eventsManager.finishProcessing()
    writer.closeFile()
    eventsManager.removeHandler(writer)
    writer = null
    JsonUtils.processEventsFileVizData(services.matsimServices.getControlerIO.getIterationFilename(currentIter, "events.xml.gz"),
      services.matsimServices.getControlerIO.getOutputFilename("trips.json"))
  }

  private def cleanupVehicle() = {
    for ( actors <- vehicleActors) {
      for ( vehicleActor <- actors) {
        logger.debug(s"Stopping ${vehicleActor._2.path.name} ")
        actorSystem.stop(vehicleActor._2)
      }
    }
  }

  private def cleanupHouseHolder() = {
    for ( actors <- householdActors) {
      for ( idAndHouseholdActor <- actors) {
        logger.debug(s"Stopping ${idAndHouseholdActor._2.path.name} ")
        actorSystem.stop(idAndHouseholdActor._2)
      }
    }
  }

  override def notifyShutdown(event: ShutdownEvent): Unit = {

    if (writer != null && event.isUnexpected) {
      cleanupWriter()
    }
    actorSystem.stop(eventSubscriber)
    actorSystem.stop(services.schedulerRef)
    actorSystem.terminate()
  }

  def resetPop(iter: Int): Unit = {
    val personAgents = mutable.Map[Id[Person], ActorRef]()
    for ((k, v) <- services.popMap.take(services.beamConfig.beam.agentsim.numAgents).flatten) {
      val props = Props(classOf[PersonAgent], k, PersonData(v.getSelectedPlan),services)
      val ref: ActorRef = actorSystem.actorOf(props, s"${k.toString}_$iter")
      services.schedulerRef ! ScheduleTrigger(InitializeTrigger(0.0), ref)
      personAgents +=((k, ref))
    }
    // Generate taxis and intialize them to be located within ~initialLocationJitter km of a subset of agents
    //TODO put these in config
    val taxiFraction = 0.1
    val initialLocationJitter = 2000 // meters
    for((k,v) <- services.popMap.get.take(math.round(taxiFraction * services.popMap.size).toInt)){
      val personInitialLocation: Coord = v.getSelectedPlan.getPlanElements.iterator().next().asInstanceOf[Activity].getCoord
      val taxiInitialLocation: Coord = new Coord(personInitialLocation.getX + initialLocationJitter * 2.0 * (Random.nextDouble() - 0.5),personInitialLocation.getY + initialLocationJitter * 2.0 * (Random.nextDouble() - 0.5))
      val props = Props(classOf[TaxiAgent], Id.create(k.toString,TaxiAgent.getClass), TaxiData(taxiInitialLocation))
      val ref: ActorRef = actorSystem.actorOf(props, s"taxi_${k.toString}_$iter")
      services.schedulerRef ! ScheduleTrigger(InitializeTrigger(0.0), ref)
    }
    val iterId = Option(iter.toString)
    vehicleActors = initVehicleActors(iterId)
    householdActors = initHouseholds(personAgents, iterId)
  }

  private def initHouseholds(personAgents: mutable.Map[Id[Person], ActorRef]  ,iterId: Option[String])  = {
    val actors = Option(services.households.map { case (householdId, matSimHousehold) =>
      val houseHoldVehicles = matSimHousehold.getVehicleIds.asScala.map { vehicleId =>
        val vehicleActRef = vehicleActors.get.apply(vehicleId)
        (vehicleId, vehicleActRef)
      }.toMap
      val membersActors = matSimHousehold.getMemberIds.asScala.map { personId => (personId, personAgents(personId)) }.toMap
      val props = HouseholdActor.props(householdId, matSimHousehold, houseHoldVehicles, membersActors)
      val householdActor = actorSystem.actorOf(props, HouseholdActor.buildActorName(householdId, iterId))
      householdActor ! InitializeTrigger(0)
      (householdId, householdActor)
    })
    actors
  }

  private def initVehicleActors(iterId: Option[String]) = {
    val actors = Option(services.vehicles.map { case (vehicleId, matSimVehicle) =>
      val props = BeamVehicleAgent.props(vehicleId, matSimVehicle, new Trajectory(),
        new Powertrain(BeamVehicle.energyPerUnitByType(matSimVehicle.getType.getId)))
      val beamVehicle = actorSystem.actorOf(props, BeamVehicle.buildActorName(vehicleId, iterId))
      beamVehicle ! InitializeTrigger(0)
      (vehicleId, beamVehicle)
    })
    actors
  }

  def subscribe(eventType: String): Unit = {
    services.agentSimEventsBus.subscribe(eventSubscriber, eventType)
  }
}



