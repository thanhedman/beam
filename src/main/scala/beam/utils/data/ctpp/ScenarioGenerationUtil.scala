package beam.utils.data.ctpp

import java.io.{BufferedWriter, File, PrintWriter}

import org.matsim.core.utils.io.IOUtils
import org.matsim.api.core.v01.{Coord, Id}

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



  def drawhouseHoldSize(scenario: Scenario, taz:TAZ): Int = {
    val frequencies=houseHoldSize(scenario, taz)  // hhSize -> freq

    // convert to propbabilities (divide freq/sum(freq))  // hhSize -> prob
      // 1-> 0.15
      // 2-> 0.35
      // 3-> 0.25
      // 4-> 0.25

    // ranges: 1-> [0,0.15),
               2-> [0.15,0.5)
                 3-> [0.5,0.75)
                 4-> [0.75,1.0]
    // draw number between 0 and 1 -> 0.45 -> 2
                                      0.76 -> 4




  }

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
  def getPlanRows(personId: String, homeLocation: Coord, endTimeFirstHomeActivity: Double, workLocation: Coord, endTimeWorkActivity:Double): List[PlansTableRow] = ???


  def writeVehicleTableToCSV(vehicleTable:List[VehicleTableRow], outputPath: String) = {
    val header = "vehicleId,vehicleTypeId,householdId"
    writeRows(header, vehicleTable, outputPath)
  }
  def writePopulationTableToCSV(populationTable:List[PopulationTableRow], outputPath: String) = {
    val header = "personId,age,isFemale,householdId,householdRank,excludedModes,valueOfTime"
    writeRows(header, populationTable, outputPath)
  }
  def writePlansTableToCSV(plansTable:List[PlansTableRow], outputPath: String) = {
    val header = "personId,planIndex,planElementType,planElementIndex,activityType,activityLocationX,activityLocationY,activityEndTime,legMode"
    writeRows(header, plansTable, outputPath)
  }
  def writeHouseholdTableToCSV(householdTable:List[HouseholdTableRow], outputPath: String) = {
    val header = "householdId,incomeValue,locationX,locationY"
    writeRows(header, householdTable, outputPath)
  }

  def writeRows(header: String, rows: List[Row], outputPath: String): Unit = {
    val writer = IOUtils.getBufferedWriter(outputPath)
    writer.write(header)
    writer.newLine()
    rows.foreach(row => {
      writer.write(row.toString)
      writer.newLine()
    })
    writer.close()
  }
}







case class TAZ(trazId: Id[TAZ],centerCoord:Coord)
case class Scenario()
case class Household(income:Double, householdSize: Int, numVehicles:Int, homeLocation: Coord)

trait Row
case class VehicleTableRow(vehicleId:String, vehicleTypeId:String, householdId:String) extends Row {
  override def toString: String = s"$vehicleId,$vehicleTypeId,$householdId"
}
case class PopulationTableRow(personId:String, age:Int, isFemale:Boolean, householdId:String, householdRank:Int, excludedModes:String, valueOfTime:Double) extends Row {
  override def toString: String = s"$personId,$age,$isFemale,$householdId,$householdRank,$excludedModes,$valueOfTime"
}
case class PlansTableRow(personId:String, planIndex:Int, planElementType:String, planElementIndex:Int, activityType:String, activityLocationX:Double, activityLocationY:Double, activityEndTime:Double, legMode:String) extends Row {
  override def toString: String = s"$personId,$planIndex,$planElementType,$planElementIndex,$activityType,$activityLocationX,$activityLocationY,$activityEndTime,$legMode"
}
case class HouseholdTableRow(householdId:String, incomeValue:Double, locationX:Double, locationY:Double) extends Row {
  override def toString: String = s"$householdId,$incomeValue,$locationX,$locationY"
}

sealed trait Gender
object Gender{
  case object Male extends Gender
  case object Female extends Gender
}
