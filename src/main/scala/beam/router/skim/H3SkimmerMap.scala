package beam.router.skim

import beam.agentsim.infrastructure.taz.TAZ
import beam.sim.vehiclesharing.VehicleManager
import org.matsim.api.core.v01.Id

import scala.collection.{immutable, mutable}

case class H3SkimmerMapKey(timBin: Int,
                           idTaz: Id[TAZ],
                           hexIndex: String,
                           idVehManager: Id[VehicleManager],
                           label: String) extends H3SkimmerKey

case class H3SkimmerMapValue(value: Double) extends H3SkimmerValue


class H3SkimmerMap extends H3Skimmer {
  override def cvsFileName: String = "h3-skims-map.csv.gz"
  override def cvsFileHeader: String = "timeBin,idTaz,hexIndex,idVehManager,label,value"
  override def strMapToKeyVal(strMap: immutable.Map[String, String]): (H3SkimmerKey, H3SkimmerValue) = {
    val time = strMap("timeBin").toInt
    val tazId = Id.create(strMap("idTaz"), classOf[TAZ])
    val hex = strMap("hexIndex")
    val manager = Id.create(strMap("idVehManager"), classOf[VehicleManager])
    val label = strMap("label")
    val value = strMap("value").toDouble
    (H3SkimmerMapKey(time, tazId, hex, manager, label), H3SkimmerMapValue(value))
  }
  override def keyValToStrMap(keyVal: (H3SkimmerKey, H3SkimmerValue)): immutable.Map[String, String] = {
    val imap = mutable.Map.empty[String, String]
    imap.put("timeBin", keyVal._1.asInstanceOf[H3SkimmerMapKey].timBin.toString)
    imap.put("idTaz", keyVal._1.asInstanceOf[H3SkimmerMapKey].idTaz.toString)
    imap.put("hexIndex", keyVal._1.asInstanceOf[H3SkimmerMapKey].hexIndex.toString)
    imap.put("idVehManager", keyVal._1.asInstanceOf[H3SkimmerMapKey].idVehManager.toString)
    imap.put("label", keyVal._1.asInstanceOf[H3SkimmerMapKey].label.toString)
    imap.put("value", keyVal._2.asInstanceOf[H3SkimmerMapValue].value.toString)
    imap.toMap
  }

}


object H3SkimmerMap {

}
