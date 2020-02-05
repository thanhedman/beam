package beam.utils.data.ctpp

import org.matsim.api.core.v01.{Coord, Id}
import scala.collection.mutable

object ScenarioGenerationUtil {

  // TODO: Art will implement

  def numberOfHouseholds(scenario: Scenario, taz:TAZ): Int = ???
  def numberOfPeopleInTAZ(scenario: Scenario, taz:TAZ): Int = ???
  def houseHoldSize(scenario: Scenario, taz:TAZ): Map[Int,Int] = ??? // hhSize -> freq
  def getAge(scenario: Scenario, taz:TAZ): Map[Range,Int] = ??? // hhSize -> freq
  //case class(lowerAgeIncluded:Int,upperAgeExcluded)
  def getGender(scenario: Scenario, taz:TAZ): Map[Gender,Int] = ??? // hhSize -> freq
  //case class Household(income:Double, householdSize: Int, numVehicles:Int, homeLocation: Coord)
  def getIncome(scenario: Scenario, taz:TAZ): Map[Range,Int] = ??? // income range -> freq
  def getWorkTAZs(scenario: Scenario,homeTAZ:TAZ): Map[TAZ,Int] = ??? // destinationTAZ -> freq
  def getNumberOfVehicles(scenario: Scenario, taz:TAZ): Map[Int,Int] = ??? // numberOfVehicles -> freq


  def getEndTimeOfHomeActivity(scenario: Scenario,homeTAZ:TAZ):Map[Range,Int] = ???
  def getTravelTimeToWork(scenario: Scenario,homeTAZ:TAZ):Map[Range,Int] = ???
  def getWeeklyWorkHours(scenario: Scenario,homeTAZ:TAZ):Map[Range,Int] = ???


 //TODO: Evgeniy
  def drawCoordinates(scenario: Scenario,taz:TAZ,sampleSize:Int):Set[Coord]= ???


  // TODO: Zach/Colin
  // TODO: define file format, and create readers [RASHID]
  def drawFirstHomeActEndTime():Double = ???
  def drawWorkActDuration(firstHomeActEndTime:Double): Double = ???


// TODO: Rashid -> create tasks for converting above data to probabilities

  def drawHousehold(scenario: Scenario): Household = ???
  def drawHouseholds(scenario: Scenario): Set[Household] = ???



  def sampleHomeToWorkTAZ(scenario: Scenario):(TAZ,TAZ) = ???
  def getAllTAZs(scenario: Scenario):Set[TAZ] = ???
  def sampleFromTAZ(taz: TAZ):Coord = ???
  def drawFirstHomeActEndTime():Double = ???
  def drawWorkActDuration(firstHomeActEndTime:Double): Double = ???

  def drawHousehold(scenario: Scenario, taz:TAZ): Household = ???







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
case class Household(income:Double, householdSize: Int, numVehicles:Int, homeLocation: Coord)

case class VehicleTableRow(vehicleId:String, vehicleTypeId:String, householdId:String)
case class PopulationTableRow(personId:String, age:Int, isFemale:Boolean, householdId:String, householdRank:Int, excludedModes:String, valueOfTime:Double)
case class PlansTableRow(personId:String, planIndex:Int, planElementType:String, planElementIndex:Int, activityType:String, activityLocationX:Double, activityLocationY:Double, activityEndTime:Double, legMode:String)
case class HouseholdTableRow(householdId:String, incomeValue:Double, locationX:Double, locationY:Double)

sealed trait Gender
object Gender{
  case object Male extends Gender
  case object Female extends Gender
}
