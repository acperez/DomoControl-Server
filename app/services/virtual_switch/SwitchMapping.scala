package services.virtual_switch

import play.api.libs.json.{Json, OFormat}

case class SwitchMapping(
    serviceId: Int,
    switchId: String)

object SwitchMapping {
  implicit val domoSwitchFormat: OFormat[SwitchMapping] = Json.format[SwitchMapping]
}
