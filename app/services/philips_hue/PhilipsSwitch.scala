package services.philips_hue

import play.api.libs.json.Json
import services.common.DomoSwitchBase

case class PhilipsSwitch (
  serviceId: Int,
  id: String,
  status: Boolean,
  name: String,
  available: Boolean,
  color: String
)  extends DomoSwitchBase

object PhilipsSwitch {
  implicit val domoSwitchFormat = Json.format[PhilipsSwitch]
}
