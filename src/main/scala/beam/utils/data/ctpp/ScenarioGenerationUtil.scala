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
  def generateOutputTables(household: Set[Household]): (Set[VehicleTable],Set[PopulationTable],Set[PlansTable],Set[HouseholdTable] ) = ???



  def writeVehicleTableToCSV(vehicleTable:Set[VehicleTable],outputPath: String) = ???
  def writePopulationTableToCSV(populationTable:Set[PopulationTable],outputPath: String) = ???
  def writePlansTableToCSV(plansTable:Set[PlansTable],outputPath: String) = ???
  def writeHouseholdTableToCSV(householdTable:Set[HouseholdTable] ,outputPath: String) = ???
}

case class TAZ(trazId: Id[TAZ],centerCoord:Coord)
case class Scenario()
case class Household(income:Double, householdSize: Int, numVehicles:Int)

case class VehicleTable(vehicleId:String,vehicleTypeId:String,householdId:String)
case class PopulationTable(personId:String,age:Int,isFemale:Boolean,householdId:String,householdRank:Int,excludedModes:String,valueOfTime:Double)
case class PlansTable(personId:String,planIndex:Int,planElementType:String,planElementIndex:Int,activityType:String,activityLocationX:Double,activityLocationY:Double,activityEndTime:Double,legMode:String)
case class HouseholdTable(householdId:String,incomeValue:Double,locationX:Double,locationY:Double)


