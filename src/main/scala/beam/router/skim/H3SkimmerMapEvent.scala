package beam.router.skim

import beam.agentsim.infrastructure.taz.TAZ
import beam.sim.vehiclesharing.VehicleManager
import org.matsim.api.core.v01.Id
import H3Skimmer._

case class H3SkimmerMapEvent(time: Double,
                             timBin: Int,
                             idTaz: Id[TAZ],
                             hexIndex: String,
                             idVehManager: Id[VehicleManager],
                             label: String,
                             value: Double) extends H3SkimmerEvent(time) {

  override def getEventType: String = "H3SkimmerMapEvent"

  override def getKeyVal: (H3SkimmerKey, H3SkimmerValue) = {
    (H3SkimmerMapKey(timBin, idTaz, hexIndex, idVehManager, label), H3SkimmerMapValue(value))
  }

}
