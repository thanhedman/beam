package beam.agentsim.agents.ridehail

import beam.agentsim.agents.ridehail.AlonsoMoraPoolingAlgForRideHail.CustomerRequest
import beam.agentsim.agents.vehicles.BeamVehicle
import beam.agentsim.agents.vehicles.EnergyEconomyAttributes.Powertrain
import org.matsim.api.core.v01.Id
import org.matsim.api.core.v01.population.{Activity, Population}
import org.matsim.core.config.ConfigUtils
import org.matsim.core.population.io.PopulationReader
import org.matsim.core.scenario.ScenarioUtils
import org.matsim.core.utils.collections.QuadTree
import org.matsim.households.{Household, HouseholdImpl, HouseholdsReaderV10}

import scala.collection.immutable.List
import scala.collection.{JavaConverters, mutable}
import scala.concurrent.{Await, TimeoutException}
import scala.concurrent.duration._
import scala.collection.JavaConverters._

object MatchingPerformanceTest {

  def main(args: Array[String]): Unit = {
    // VehicleCentricMatchingForRideHail
      val alg1 = new VehicleCentricMatchingForRideHail(
          spatialPoolCustomerReqs,
          availVehicles,
          rideHailManager.beamServices,
          skimmer
        )
    try {
      val t0 = System.nanoTime()
      Await.result(alg1.matchAndAssign(0), atMost = 1440.minutes)
      val t1 = System.nanoTime()
      val elapsed = ((t1 - t0) / 1E9).toInt
      println(s"*** scenario 6 *** ${sum / count} avg combinations per household, $elapsed sec elapsed ")
    } catch {
      case _: TimeoutException =>
        println("alg1: TIMEOUT")
    }


    // AsyncAlonsoMoraAlgForRideHail
      val alg2 = new AsyncAlonsoMoraAlgForRideHail(
          spatialPoolCustomerReqs,
          availVehicles,
          rideHailManager.beamServices,
          skimmer
        )
    try {
      val t0 = System.nanoTime()
      Await.result(alg2.matchAndAssign(0), atMost = 1440.minutes)
      val t1 = System.nanoTime()
      val elapsed = ((t1 - t0) / 1E9).toInt
      println(s"*** scenario 6 *** ${sum / count} avg combinations per household, $elapsed sec elapsed ")
    } catch {
      case _: TimeoutException =>
        println("alg2: TIMEOUT")
    }



    // AlonsoMoraPoolingAlgForRideHail
      val alg3 = new AlonsoMoraPoolingAlgForRideHail(
        spatialPoolCustomerReqs,
        availVehicles,
        rideHailManager.beamServices,
        skimmer
      )
    try {
      val t0 = System.nanoTime()
      Await.result(alg3.matchAndAssign(0), atMost = 1440.minutes)
      val t1 = System.nanoTime()
      val elapsed = ((t1 - t0) / 1E9).toInt
      println(s"*** scenario 6 *** ${sum / count} avg combinations per household, $elapsed sec elapsed ")
    } catch {
      case _: TimeoutException =>
        println("alg3: TIMEOUT")
    }
  }


  private def scenario(): (Population, List[(Household, List[BeamVehicle])]) = {
    val config = ConfigUtils.loadConfig("test/input/sf-light/sf-light-25k.conf")
    val sc: org.matsim.api.core.v01.Scenario = ScenarioUtils.createMutableScenario(config)
    ScenarioUtils.loadScenario(sc)
    val coords = sc.getNetwork.getNodes.asScala.map(_._2.getCoord)

    val spatialPoolCustomerReqs: QuadTree[CustomerRequest] = new QuadTree[CustomerRequest](
      coords.minBy(_.getX).getX, coords.minBy(_.getY).getY, coords.maxBy(_.getX).getX, coords.maxBy(_.getY).getY)

    sc.getPopulation.getPersons.values().asScala
      .filter(p => p.getSelectedPlan.getPlanElements.get(0).asInstanceOf[Activity].getEndTime >= 28800 && p.getSelectedPlan.getPlanElements.get(0).asInstanceOf[Activity].getEndTime <= 32400)
      .foreach {
        p =>
          val pickup = p.getSelectedPlan.getPlanElements.get(0).asInstanceOf[Activity]
          val dropoff = p.getSelectedPlan.getPlanElements.get(1).asInstanceOf[Activity]
          val req = MatchmakingUtils.createPersonRequest(
            p,
            rhr.pickUpLocationUTM,
            pickup.getEndTime,
            rhr.destinationUTM,
            rideHailManager.beamServices
          )
          spatialPoolCustomerReqs.put(pickup.getCoord.getX, dropoff.getCoord.getY, req)
      }
    for(person <- sc.getPopulation.getPersons.values().asScala) {

    }


    new HouseholdsReaderV10(sc.getHouseholds).readFile("test/input/sf-light/sample/25k/households.xml")
    val households: mutable.ListBuffer[(Household, List[BeamVehicle])] =
      mutable.ListBuffer.empty[(Household, List[BeamVehicle])]
    import scala.collection.JavaConverters._
    for (hh: Household <- sc.getHouseholds.getHouseholds.asScala.values) {
      //hh.asInstanceOf[HouseholdImpl].setMemberIds(hh.getMemberIds)
      val vehicles = List[BeamVehicle](
        new BeamVehicle(
          Id.createVehicleId(hh.getId.toString + "-veh1"),
          new Powertrain(0.0),
          defaultCAVBeamVehicleType
        ),
        new BeamVehicle(
          Id.createVehicleId(hh.getId.toString + "-veh2"),
          new Powertrain(0.0),
          defaultCAVBeamVehicleType
        )
      )
      hh.asInstanceOf[HouseholdImpl]
        .setVehicleIds(JavaConverters.seqAsJavaList(vehicles.map(veh => veh.toStreetVehicle.id)))
      households.append((hh.asInstanceOf[HouseholdImpl], vehicles))
    }
    (sc.getPopulation, households.toList)
  }

}

