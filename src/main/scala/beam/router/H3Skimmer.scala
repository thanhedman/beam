package beam.router
import beam.agentsim.infrastructure.taz.TAZ
import beam.sim.vehiclesharing.VehicleManager
import org.matsim.api.core.v01.Id

class H3Skimmer {
  import H3Skimmer._
  private type SkimLabel = String
  private type h3skimmerKey = (TimeBin, Id[TAZ], HexIndex, Id[VehicleManager], SkimLabel)
  private val h3skimmerFileName = "h3skims.csv.gz"
}

object H3Skimmer {
  type TimeBin = Int
  type HexIndex = String
}
