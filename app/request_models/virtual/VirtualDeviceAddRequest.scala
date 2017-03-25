package request_models.virtual

import play.api.libs.json.{JsPath, Reads}
import play.api.libs.functional.syntax._
import services.virtual_switch.SwitchMapping

case class VirtualDeviceAddRequest(
    name: String,
    switches: Seq[SwitchMapping])

object VirtualDeviceAddRequest {
  implicit val reads: Reads[VirtualDeviceAddRequest] = (
    (JsPath \ "name").read[String] and
    (JsPath \ "switches").read[Seq[SwitchMapping]]
  )((name, switches) => VirtualDeviceAddRequest.apply(name, switches))
}
