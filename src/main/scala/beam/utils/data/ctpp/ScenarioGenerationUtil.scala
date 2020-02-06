package beam.utils.data.ctpp

import java.io.{BufferedWriter, File, PrintWriter}

import beam.sim.population.PopulationAdjustment
import org.matsim.core.utils.io.IOUtils
import org.matsim.api.core.v01.{Coord, Id}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object ScenarioGenerationUtil {

  // use synthpop for hh and pop generation + home location
  // use ctpp work location
  // use NHTS for act durations



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

  val random = scala.util.Random

  def drawGender(scenario: Scenario, taz:TAZ): Gender  = {
    val frequencies = getGender(scenario, taz)
    getFrequency[Gender](frequencies)
  }

  def drawIncome(scenario: Scenario, taz:TAZ): Int = {
    val frequencies=getIncome(scenario, taz)
    val range=getFrequency[Range](frequencies)
    range.start + random.nextInt(range.end-range.start)
  }

  def drawWorkTAZs(scenario: Scenario, taz:TAZ): TAZ = {
    val frequencies=getWorkTAZs(scenario, taz)
    getFrequency[TAZ](frequencies)
  }

  def drawNumberOfVehicles(scenario: Scenario, taz:TAZ): Int = {
    val frequencies=getNumberOfVehicles(scenario, taz)
    getFrequency[Int](frequencies)
  }

  def drawHouseHoldSize(scenario: Scenario, taz:TAZ): Int = {
    val frequencies=houseHoldSize(scenario, taz)
    getFrequency[Int](frequencies)
  }

  def drawAge(scenario: Scenario, taz:TAZ): Int = {
    val frequencies=getAge(scenario, taz)
    val range=getFrequency[Range](frequencies)
    range.start + random.nextInt(range.end-range.start)
  }

  def drawEndTimeOfHomeActivityInSec(scenario: Scenario,homeTAZ:TAZ): Int = {
    val frequencies=getEndTimeOfHomeActivity(scenario, homeTAZ)
    val range=getFrequency[Range](frequencies)
    range.start + random.nextInt(range.end-range.start)
  }

  def drawTravelTimeToWorkInSec(scenario: Scenario,homeTAZ:TAZ):Int = {
    val frequencies=getTravelTimeToWork(scenario, homeTAZ)
    val range=getFrequency[Range](frequencies)
    range.start + random.nextInt(range.end-range.start)
  }

  def drawWeeklyWorkHoursInSec(scenario: Scenario,homeTAZ:TAZ):Int = {
    val frequencies=getWeeklyWorkHours(scenario, homeTAZ)
    val range=getFrequency[Range](frequencies)
    range.start + random.nextInt(range.end-range.start)
  }

  def getFrequency[A](frequencies: Map[A, Int]): A = {
    val total = frequencies.values.sum.toDouble
    val randomNumber = random.nextDouble()
    var sum = 0.0
    for((key, value) <- frequencies) {
      val probability = value.toDouble/ total
      if(randomNumber >= sum && randomNumber < sum + probability) {
        return key
      }
      else {
        sum = sum + probability
      }
    }
    frequencies.keys.last
  }

  //def drawHousehold(scenario: Scenario): Household = ???
  //def drawHouseholds(scenario: Scenario): Set[Household] = ???



  //def sampleHomeToWorkTAZ(scenario: Scenario):(TAZ,TAZ) = ???
  def getAllTAZs(scenario: Scenario):Set[TAZ] = ???
  //def sampleFromTAZ(taz: TAZ):Coord = ???
  //def drawFirstHomeActEndTime():Double = ???
  //def drawWorkActDuration(firstHomeActEndTime:Double): Double = ???

  //def drawHousehold(scenario: Scenario, taz:TAZ): Household = ???



  def createHousehold(householdId: String,scenario: Scenario, taz: TAZ, householdTableRows: mutable.ListBuffer[HouseholdTableRow], homeLocation: Coord) = {
    val income=drawIncome(scenario,taz)
    householdTableRows+=HouseholdTableRow(householdId, income, homeLocation.getX, homeLocation.getY)
  }

  def createVehicles(householdId: String, scenario: Scenario, taz: TAZ, vehicleTableRows: ListBuffer[VehicleTableRow]) = {
    val numberOfVehicles=drawNumberOfVehicles(scenario,taz)

    for (i <- 1 to numberOfVehicles){
      vehicleTableRows+=VehicleTableRow(i.toString + "-" + householdId, "CAR-TODO-GENERALIZE", householdId)
    }
  }

  // TODO: check with Zach, if derive from just household income or more?
  def getValueOfTime(scenario: Scenario, taz:TAZ): Double ={
    PopulationAdjustment.IncomeToValueOfTime(drawIncome(scenario,taz)).get
  }



  def createPopulationAndPlans(householdId: String, scenario: Scenario, taz: TAZ, populationTableRows: ListBuffer[PopulationTableRow], plansTableRows: ListBuffer[PlansTableRow],houseHoldSize:Int, homeLocation:Coord) = {

    for (i <- 1 to houseHoldSize) {
      val personId=i.toString + "-" + householdId
      populationTableRows+=PopulationTableRow(personId, drawAge(scenario,taz), drawGender(scenario,taz).isInstanceOf[Gender.Female], householdId, i, "\"\"", getValueOfTime(scenario,taz))

      val endTimeOfHomeActivity=drawEndTimeOfHomeActivityInSec(scenario,taz)
      val endTimeWorkActivity=endTimeOfHomeActivity+drawTravelTimeToWorkInSec(scenario,taz)+drawWeeklyWorkHoursInSec(scenario,taz)/5
      plansTableRows++=getPlanRows(personId,homeLocation,endTimeOfHomeActivity,drawCoordinates(scenario,taz,1).head,endTimeWorkActivity)
    }
  }

  // TODO - refactor: as we are passing scenario everywhere, probably better to make all methods member of scneario object
  def generateOutputTables(scenario: Scenario): (List[VehicleTableRow],List[PopulationTableRow],List[PlansTableRow],List[HouseholdTableRow] ) = {
    val vehicleTableRows=mutable.ListBuffer[VehicleTableRow]()
    val populationTableRows=mutable.ListBuffer[PopulationTableRow]()
    val plansTableRows=mutable.ListBuffer[PlansTableRow]()
    val householdTableRows=mutable.ListBuffer[HouseholdTableRow]()



    var actualPopulationSize=0
    var controlPopulationSize=0
    getAllTAZs(scenario).foreach{ taz=>
      val numberOfHouseHoldsInTAZ=numberOfHouseholds(scenario,taz)


      for (i <- 1 to numberOfHouseHoldsInTAZ) {
        val houseHoldSize=drawHouseHoldSize(scenario,taz)
        actualPopulationSize+=houseHoldSize
        val homeLocation=drawCoordinates(scenario,taz,1).head
        val householdId=i.toString
        createHousehold(householdId,scenario,taz, householdTableRows,homeLocation)

        createVehicles(householdId,scenario,taz,vehicleTableRows)



          createPopulationAndPlans(householdId,scenario,taz, populationTableRows,plansTableRows,houseHoldSize,homeLocation)
      }


      controlPopulationSize+=numberOfPeopleInTAZ(scenario,taz)
      // TODO: log difference between people

    }
    // discuss, Art, which tables or methods available


    println(s"actualPopulationSize: $actualPopulationSize, controlPopulationSize: $controlPopulationSize")


   ???
  }


  def getPlanRows(personId: String, homeLocation: Coord, endTimeFirstHomeActivity: Double, workLocation: Coord, endTimeWorkActivity:Double): List[PlansTableRow] = {
    List(
      PlansTableRow(personId, 0, "activity", 0, "Home", homeLocation.getX.toString, homeLocation.getY.toString, endTimeFirstHomeActivity.toString, ""),
      PlansTableRow(personId, 0, "leg", 1, "", "", "", "", ""),
      PlansTableRow(personId, 0, "activity", 2, "Work", workLocation.getX.toString, workLocation.getY.toString, endTimeWorkActivity.toString, ""),
      PlansTableRow(personId, 0, "leg", 3, "", "", "", "", ""),
      PlansTableRow(personId, 0, "activity", 4, "Home", homeLocation.getX.toString, homeLocation.getY.toString, Double.NegativeInfinity.toString, "")
    )
  }


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
case class PlansTableRow(personId:String, planIndex:Int, planElementType:String, planElementIndex:Int, activityType:String, activityLocationX: String, activityLocationY: String, activityEndTime: String, legMode:String) extends Row {
  override def toString: String = s"$personId,$planIndex,$planElementType,$planElementIndex,$activityType,$activityLocationX,$activityLocationY,$activityEndTime,$legMode"
}
case class HouseholdTableRow(householdId:String, incomeValue:Double, locationX:Double, locationY:Double) extends Row {
  override def toString: String = s"$householdId,$incomeValue,$locationX,$locationY"
}

sealed trait Gender
object Gender{
  case class Male() extends Gender
  case class Female() extends Gender
}
