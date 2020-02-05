package beam.utils.data.ctpp

object  CreatePlansMain {

  def main(args: Array[String]): Unit = {
    val scenario=Scenario()




    val households=ScenarioGenerationUtil.drawHouseholds(scenario)
    val (vehicleTable,populationTable,plansTable,householdTable)=ScenarioGenerationUtil.generateOutputTables(households)


/*    val vehicleTable:Set[VehicleTable] = ???
    val populationTable:Set[PopulationTable] = ???
    val plansTable:Set[PlansTable] = ???
    val householdTable:Set[HouseholdTable] = ???*/

    ScenarioGenerationUtil.writeVehicleTableToCSV(vehicleTable,"path-to-vehicles.csv")
    ScenarioGenerationUtil.writePopulationTableToCSV(populationTable,"path-to-vehicles.csv")
    ScenarioGenerationUtil.writePlansTableToCSV(plansTable,"path-to-vehicles.csv")
    ScenarioGenerationUtil.writeHouseholdTableToCSV(householdTable,"path-to-vehicles.csv")


  }








}
