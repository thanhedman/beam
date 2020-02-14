package beam.agentsim.agents.vehicles

import beam.agentsim.events.SpaceTime
import beam.router.Modes.BeamMode
import org.matsim.api.core.v01.Id
import org.matsim.vehicles.Vehicle

object VehicleProtocol {

  case class RemovePassengerFromTrip(passId: PersonIdWithActorRef)

  case class StreetVehicle(
    id: Id[Vehicle],
    vehicleTypeId: Id[BeamVehicleType],
    locationUTM: SpaceTime,
    mode: BeamMode,
    asDriver: Boolean,
    vehicleUid: Option[java.util.UUID]
  )/* {
    var vehicle: Option[BeamVehicle] = None
    def setVehicle(vehicle1:BeamVehicle) = {
      this.vehicle = Some(vehicle1)
      this
    }
  }*/

}
