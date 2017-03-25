package services.philips_hue

import play.api.libs.json.{Json, OFormat}
import services.common.DomoSwitchBase

case class PhilipsSwitch (
  serviceId: Int,
  id: String,
  status: Boolean,
  name: String,
  alias: Option[String],
  available: Boolean,
  color: String
)  extends DomoSwitchBase

object PhilipsSwitch {
  implicit val domoSwitchFormat: OFormat[PhilipsSwitch] = Json.format[PhilipsSwitch]
}
