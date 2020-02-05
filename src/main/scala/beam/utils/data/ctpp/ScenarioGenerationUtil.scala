package beam.utils.data.ctpp

import java.io.{BufferedWriter, File, PrintWriter}

import org.matsim.core.utils.io.IOUtils
import org.matsim.api.core.v01.{Coord, Id}

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
case class Household(income:Double, householdSize: Int, numVehicles:Int)

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


