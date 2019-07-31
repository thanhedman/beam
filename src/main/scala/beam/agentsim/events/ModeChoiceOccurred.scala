package beam.agentsim.events

import beam.router.model.EmbodiedBeamTrip
import org.matsim.api.core.v01.events.Event

object ModeChoiceOccurred {
  val EVENT_TYPE: String = "ModeChoiceOccurred"

  case class AltUtility(utility: Double, expUtility: Double)
  case class AltCostTimeTransfer(cost: Double, time: Double, numTransfers: Int)

  def apply(
    personId: String,
    alternatives: IndexedSeq[EmbodiedBeamTrip],
    modeCostTimeTransfers: Map[String, AltCostTimeTransfer],
    alternativesUtility: Map[String, AltUtility],
    chosenAlternativeIdx: Int
  ): ModeChoiceOccurred = {

    //TODO: this is wrong, this is not a time of current event appearance
    val time = alternatives.flatMap(trip => trip.legs).map(leg => leg.beamLeg.startTime).min

    new ModeChoiceOccurred(
      time,
      personId,
      alternatives,
      modeCostTimeTransfers,
      alternativesUtility,
      chosenAlternativeIdx
    )
  }
}

case class ModeChoiceOccurred(
  time: Double,
  personId: String,
  alternatives: IndexedSeq[EmbodiedBeamTrip],
  modeCostTimeTransfers: Map[String, ModeChoiceOccurred.AltCostTimeTransfer],
  alternativesUtility: Map[String, ModeChoiceOccurred.AltUtility],
  chosenAlternativeIdx: Int
) extends Event(time) {
  import ModeChoiceOccurred._

  override def getEventType: String = EVENT_TYPE

}
