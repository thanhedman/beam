//package beam.utils.data.synthpop
//
//import beam.utils.data.ctpp.models.{HouseholdIncome, OD, ResidenceToWorkplaceFlowGeography}
//import beam.utils.data.ctpp.readers.BaseTableReader.PathToData
//import beam.utils.data.ctpp.readers.flow.HouseholdIncomeTableReader
//import beam.utils.data.synthpop.models.Models.{PowPumaGeoId, PumaGeoId, TazGeoId}
//import com.typesafe.scalalogging.StrictLogging
//import com.vividsolutions.jts.geom.Geometry
//import org.apache.commons.math3.random.MersenneTwister
//
//trait ParkingStallGenerator {
//  def next(tazGeoId: TazGeoId, nResidents: Int, nWorkers: Int): Option[List[String]]
//}
//
//class PopulationDensityParkingStallGenerator(val geoService: GeoService, val randomSeed: Int)
//    extends ParkingStallGenerator
//    with StrictLogging {
//  private val rndGen: MersenneTwister = new MersenneTwister(randomSeed) // Random.org
//  private val tazGeoIdToArea: Map[String, Double] = geoService.tazGeoIdToGeom.map {
//    case (tazGeoId: TazGeoId, geometry: Geometry) =>
//    tazGeoId.asUniqueKey -> geometry.getArea
//  }
//
//  private val tazGeoIdToCoord: Map[String, (Double, Double)] = geoService.tazGeoIdToGeom.map {
//    case (tazGeoId: TazGeoId, geometry: Geometry) =>
//      tazGeoId.asUniqueKey -> (geometry.getCentroid.getX, geometry.getCentroid.getY)
//  }
//
//
//  override def next(tazGeoId: TazGeoId, nResidents: Int, nWorkers: Int): Option[String] = {
//    tazGeoIdToArea.get(tazGeoId) match {
//      case Some(xs) =>
//        val incomeInRange = xs.filter(od => od.attribute.contains(income.toInt))
//        if (incomeInRange.isEmpty) {
//          logger.info(s"Could not find OD with income ${income} in ${xs.mkString(" ")}")
//        }
//        ODSampler.sample(incomeInRange, rndGen).map(x => x.destination)
//      case None =>
//        logger.info(
//          s"Could not find '${homeLocation}' key as ${homeLocation} in the `householdGeoIdToIncomeOD`"
//        )
//        None
//    }
//
//  }
//}
