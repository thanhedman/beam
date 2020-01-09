package beam.agentsim.agents.ridehail

import java.io.{BufferedWriter, FileNotFoundException, FileWriter}

import akka.actor.ActorRef
import beam.agentsim.agents.ridehail.AlonsoMoraPoolingAlgForRideHail.{CustomerRequest, VehicleAndSchedule}
import beam.agentsim.agents.vehicles.{BeamVehicleType, PersonIdWithActorRef}
import beam.router.BeamSkimmer
import beam.sim.config.{BeamConfig, MatSimBeamConfigBuilder}
import beam.sim.{BeamHelper, BeamServices}
import beam.utils.FileUtils
import beam.utils.TestConfigUtils.testConfig
import com.typesafe.config.ConfigFactory
import org.matsim.api.core.v01.population.{Activity, Person}
import org.matsim.api.core.v01.{Coord, Id}
import org.matsim.core.controler.AbstractModule
import org.matsim.core.scenario.{MutableScenario, ScenarioUtils}
import org.matsim.core.utils.collections.QuadTree
import org.matsim.core.utils.io.IOUtils

import scala.collection.JavaConverters._
import scala.collection.immutable.List
import scala.concurrent.duration._
import scala.concurrent.{Await, TimeoutException}
import scala.util.Random

object MatchingPerformanceTest extends BeamHelper {

  val config = ConfigFactory
    .parseString("""
                   |beam.outputs.events.fileOutputFormats = xml
                   |beam.physsim.skipPhysSim = true
                   |beam.agentsim.lastIteration = 0
                   |beam.agentsim.agents.rideHail.allocationManager.alonsoMora.waitingTimeInSec = 1800
                   |beam.agentsim.agents.rideHail.allocationManager.alonsoMora.travelTimeDelayAsFraction= 0.80
                   |beam.agentsim.agents.rideHail.allocationManager.alonsoMora.solutionSpaceSizePerVehicle = 4
        """.stripMargin)
    .withFallback(testConfig("test/input/sf-light/sf-light-25k.conf"))
    .resolve()
  val configBuilder = new MatSimBeamConfigBuilder(config)
  val matsimConfig = configBuilder.buildMatSimConf()
  val beamConfig = BeamConfig(config)
  implicit val beamScenario = loadScenario(beamConfig)
  FileUtils.setConfigOutputFile(beamConfig, matsimConfig)
  val scenario = ScenarioUtils.loadScenario(matsimConfig).asInstanceOf[MutableScenario]

  val injector = org.matsim.core.controler.Injector.createInjector(
    scenario.getConfig,
    new AbstractModule() {
      override def install(): Unit = {
        install(module(config, beamConfig, scenario, beamScenario))
      }
    }
  )
  implicit val services = injector.getInstance(classOf[BeamServices])
  implicit val skimmer = injector.getInstance(classOf[BeamSkimmer])
  implicit val actorRef = ActorRef.noSender

  val startTime = 8 * 3600
  val endTime = startTime + 1 * 60

  val (spatialPoolCustomerReqs, availVehicles) = buildSpatialPoolCustomerReqs(startTime, endTime, 100, "1min100veh")

  def main(args: Array[String]): Unit = {

    println(s"requests: ${spatialPoolCustomerReqs.size}, vehicles: ${availVehicles.size}")

    //vehicleCentricMatchingForRideHail()

    //asyncAlonsoMoraAlgForRideHail()

    alonsoMoraPoolingAlgForRideHail()

    println("END")
  }

  private def vehicleCentricMatchingForRideHail(): Unit = {
    val alg1 = new VehicleCentricMatchingForRideHail(spatialPoolCustomerReqs, availVehicles, services, skimmer)
    try {
      val t0 = System.nanoTime()
      val assignment = Await.result(alg1.matchAndAssign(0), atMost = 1440.minutes)
      val t1 = System.nanoTime()
      val elapsed = ((t1 - t0) / 1E9).toInt
      println(s"VehicleCentricMatchingForRideHail, $elapsed sec")
      val pool_1p = 1 * assignment.count(_._1.requests.size == 1)
      val pool_2p = 2 * assignment.count(_._1.requests.size == 2)
      val pool_3p = 3 * assignment.count(_._1.requests.size == 3)
      val pool_4p = 4 * assignment.count(_._1.requests.size == 4)
      println(s"-------> $pool_1p pool_1p, $pool_2p pool_2p, $pool_3p pool_3p, $pool_4p pool_4p")
      val veh_pool = assignment.count(_._1.requests.size > 1)
      val veh_solo = assignment.count(_._1.requests.size == 1)
      println(s"-------> ${veh_pool + veh_solo} vehicles")
    } catch {
      case _: TimeoutException =>
        println("VehicleCentricMatchingForRideHail: TIMEOUT")
    }
  }

  private def alonsoMoraPoolingAlgForRideHail(): Unit = {
    val alg3 = new AlonsoMoraPoolingAlgForRideHail(spatialPoolCustomerReqs, availVehicles, services, skimmer)
    try {
      val t0 = System.nanoTime()
      val assignment = Await.result(alg3.matchAndAssign(0), atMost = 1440.minutes)
      val t1 = System.nanoTime()
      val elapsed = ((t1 - t0) / 1E9).toInt
      println(s"AlonsoMoraPoolingAlgForRideHail, $elapsed sec")
      val pool_1p = 1 * assignment.count(_._1.requests.size == 1)
      val pool_2p = 2 * assignment.count(_._1.requests.size == 2)
      val pool_3p = 3 * assignment.count(_._1.requests.size == 3)
      val pool_4p = 4 * assignment.count(_._1.requests.size == 4)
      println(s"-------> $pool_1p pool_1p, $pool_2p pool_2p, $pool_3p pool_3p, $pool_4p pool_4p")
      val veh_pool = assignment.count(_._1.requests.size > 1)
      val veh_solo = assignment.count(_._1.requests.size == 1)
      println(s"-------> ${veh_pool + veh_solo} vehicles")
    } catch {
      case _: TimeoutException =>
        println("AlonsoMoraPoolingAlgForRideHail: TIMEOUT")
    }
  }

  private def asyncAlonsoMoraAlgForRideHail(): Unit = {
    val alg2 = new AsyncAlonsoMoraAlgForRideHail(spatialPoolCustomerReqs, availVehicles, services, skimmer)
    try {
      val t0 = System.nanoTime()
      val assignment = Await.result(alg2.matchAndAssign(0), atMost = 1440.minutes)
      val t1 = System.nanoTime()
      val elapsed = ((t1 - t0) / 1E9).toInt
      println(s"AsyncAlonsoMoraAlgForRideHail, $elapsed sec")
      val pool_1p = 1 * assignment.count(_._1.requests.size == 1)
      val pool_2p = 2 * assignment.count(_._1.requests.size == 2)
      val pool_3p = 3 * assignment.count(_._1.requests.size == 3)
      val pool_4p = 4 * assignment.count(_._1.requests.size == 4)
      println(s"-------> $pool_1p pool_1p, $pool_2p pool_2p, $pool_3p pool_3p, $pool_4p pool_4p")
      val veh_pool = assignment.count(_._1.requests.size > 1)
      val veh_solo = assignment.count(_._1.requests.size == 1)
      println(s"-------> ${veh_pool + veh_solo} vehicles")
    } catch {
      case _: TimeoutException =>
        println("AsyncAlonsoMoraAlgForRideHail: TIMEOUT")
    }
  }

  private def buildSpatialPoolCustomerReqs(
                                            starTime: Int,
                                            endTime: Int,
                                            nbVehicles: Int,
                                            suffix: String
                                          ): (QuadTree[CustomerRequest], List[VehicleAndSchedule]) = {

    val heading1 = "person,pickup.x,pickup.y,time,dropoff.x,dropoff.y"
    val heading2 = "vehicle,type,x,y,time"
    val buffer = 5000
    var sample: Option[(QuadTree[CustomerRequest], List[VehicleAndSchedule])] = None
    try {
      val requests: Array[CustomerRequest] = IOUtils
        .getBufferedReader(s"test/input/sf-light/requests_$suffix.csv")
        .lines()
        .toArray
        .drop(1)
        .map { line =>
          val row = line.asInstanceOf[String].split(",")
          MatchmakingUtils.createPersonRequest(
            PersonIdWithActorRef(Id.create(row(0), classOf[Person]), actorRef),
            new Coord(row(1).toDouble, row(2).toDouble),
            row(3).toInt,
            new Coord(row(4).toDouble, row(5).toDouble),
            services
          )
        }
      val vehicles: Array[VehicleAndSchedule] = IOUtils
        .getBufferedReader(s"test/input/sf-light/vehicles_$suffix.csv")
        .lines()
        .toArray
        .drop(1)
        .map { line =>
          val row = line.asInstanceOf[String].split(",")
          MatchmakingUtils.createVehicleAndSchedule(
            row(0),
            beamScenario.vehicleTypes(Id.create(row(1), classOf[BeamVehicleType])),
            new Coord(row(2).toDouble, row(3).toDouble),
            row(4).toInt
          )
        }
      val coords = requests.flatMap(r => List(r.pickup.activity.getCoord, r.dropoff.activity.getCoord))
      val spatialPoolCustomerReqs: QuadTree[CustomerRequest] = new QuadTree[CustomerRequest](
        coords.minBy(_.getX).getX - buffer,
        coords.minBy(_.getY).getY - buffer,
        coords.maxBy(_.getX).getX + buffer,
        coords.maxBy(_.getY).getY + buffer
      )
      requests.foreach(
        r => spatialPoolCustomerReqs.put(r.pickup.activity.getCoord.getX, r.dropoff.activity.getCoord.getY, r)
      )
      sample = Some((spatialPoolCustomerReqs, vehicles.toList))
    } catch {
      case _: Throwable =>
        println("Now sampling!")
    }
    if (sample.isEmpty) {
      val pops = scenario.getPopulation.getPersons
        .values()
        .asScala
        .filter(
          p =>
            p.getSelectedPlan.getPlanElements
              .get(0)
              .asInstanceOf[Activity]
              .getEndTime >= starTime && p.getSelectedPlan.getPlanElements
              .get(0)
              .asInstanceOf[Activity]
              .getEndTime <= endTime
        )
      val coords = pops.map(_.getSelectedPlan.getPlanElements.get(0).asInstanceOf[Activity].getCoord)
      val spatialPoolCustomerReqs: QuadTree[CustomerRequest] = new QuadTree[CustomerRequest](
        coords.minBy(_.getX).getX - buffer,
        coords.minBy(_.getY).getY - buffer,
        coords.maxBy(_.getX).getX + buffer,
        coords.maxBy(_.getY).getY + buffer
      )
      val out1 = new BufferedWriter(new FileWriter(s"test/input/sf-light/requests_$suffix.csv"))
      out1.write(heading1)
      pops.foreach { p =>
        val pickup = p.getSelectedPlan.getPlanElements.get(0).asInstanceOf[Activity]
        val dropoff = p.getSelectedPlan.getPlanElements.get(2).asInstanceOf[Activity]
        val req = MatchmakingUtils.createPersonRequest(
          PersonIdWithActorRef(p.getId, actorRef),
          pickup.getCoord,
          pickup.getEndTime.toInt,
          dropoff.getCoord,
          services
        )
        spatialPoolCustomerReqs.put(pickup.getCoord.getX, dropoff.getCoord.getY, req)
        val line = "\n" + p.getId + "," +
          pickup.getCoord.getX + "," + pickup.getCoord.getY + "," +
          pickup.getEndTime.toInt + "," + dropoff.getCoord.getX + "," + dropoff.getCoord.getY
        out1.write(line)
      }
      out1.close()

      val out2 = new BufferedWriter(new FileWriter(s"test/input/sf-light/vehicles_$suffix.csv"))
      out2.write(heading2)
      val vehicleType = beamScenario.vehicleTypes(Id.create("Car", classOf[BeamVehicleType]))
      val rand: Random = new Random(beamScenario.beamConfig.matsim.modules.global.randomSeed)
      val vehicles = (1 to nbVehicles).map { i =>
        val x = spatialPoolCustomerReqs.getMinEasting + (spatialPoolCustomerReqs.getMaxEasting - spatialPoolCustomerReqs.getMinEasting) * rand
          .nextDouble()
        val y = spatialPoolCustomerReqs.getMinNorthing + (spatialPoolCustomerReqs.getMaxNorthing - spatialPoolCustomerReqs.getMinNorthing) * rand
          .nextDouble()
        val time = starTime + (endTime - starTime) * rand.nextDouble()
        val vehid = s"v$i"
        out2.write("\n" + vehid + "," + vehicleType.id.toString + "," + x + "," + y + "," + time.toInt)
        MatchmakingUtils.createVehicleAndSchedule(vehid, vehicleType, new Coord(x, y), time.toInt)
      }.toList
      out2.close()
      sample = Some((spatialPoolCustomerReqs, vehicles))
    }
    sample.get
  }
}
