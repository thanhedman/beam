package beam.utils.data.ctpp

import org.matsim.api.core.v01.{Coord, Id}
import scala.collection.mutable

object ScenarioGenerationUtil {
  def sampleHomeToWorkTAZ(scenario: Scenario):(TAZ,TAZ) = ???
  def getAllTAZs(scenario: Scenario):Set[TAZ] = ???
  def sampleFromTAZ(taz: TAZ):Coord = ???
  def drawFirstHomeActEndTime():Double = ???
  def drawWorkActDuration(firstHomeActEndTime:Double): Double = ???
  def drawHousehold(scenario: Scenario): Household = ???



  def drawHouseholds(scenario: Scenario): Set[Household] = ???
  def generateOutputTables(household: Set[Household]): (List[VehicleTableRow],List[PopulationTableRow],List[PlansTableRow],List[HouseholdTableRow] ) = {

    // discuss, Art, which tables or methods available


    // create people
    // create plans
    // create





   ???
  }



  // TODO: rajnikant should start implementing following
  def getPlanRows(personId: String, homeLocation: Coord, homeDepartureTime: Double, workLocation: Coord, workDepartureTime:Double): List[PlansTableRow] = ???


  // TODO: rajnikant should start implementing following
  def writeVehicleTableToCSV(vehicleTable:List[VehicleTableRow], outputPath: String) = ???
  def writePopulationTableToCSV(populationTable:List[PopulationTableRow], outputPath: String) = ???
  def writePlansTableToCSV(plansTable:List[PlansTableRow], outputPath: String) = ???
  def writeHouseholdTableToCSV(householdTable:List[HouseholdTableRow], outputPath: String) = ???
}

case class TAZ(trazId: Id[TAZ],centerCoord:Coord)
case class Scenario()
case class Household(income:Double, householdSize: Int, numVehicles:Int)

case class VehicleTableRow(vehicleId:String, vehicleTypeId:String, householdId:String)
case class PopulationTableRow(personId:String, age:Int, isFemale:Boolean, householdId:String, householdRank:Int, excludedModes:String, valueOfTime:Double)
case class PlansTableRow(personId:String, planIndex:Int, planElementType:String, planElementIndex:Int, activityType:String, activityLocationX:Double, activityLocationY:Double, activityEndTime:Double, legMode:String)
case class HouseholdTableRow(householdId:String, incomeValue:Double, locationX:Double, locationY:Double)


