package services.virtual_switch

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, OWrites, Reads}

case class VirtualDevice(
    id: String,
    name: String,
    alias: Option[String],
    switches: Seq[SwitchMapping]) {

  def setAlias(value: String): VirtualDevice = copy(alias = Some(value))
}

object VirtualDevice {

  implicit val reads: Reads[VirtualDevice] = (
    (JsPath \ "id").read[String] and
    (JsPath \ "name").read[String] and
    (JsPath \ "alias").readNullable[String] and
    (JsPath \ "switches").read[Seq[SwitchMapping]]
  )(VirtualDevice.apply _)

  implicit val writes: OWrites[VirtualDevice] = (
    (JsPath \ "id").write[String] and
    (JsPath \ "name").write[String] and
    (JsPath \ "alias").writeNullable[String] and
    (JsPath \ "switches").write[Seq[SwitchMapping]]
  )(unlift(VirtualDevice.unapply))
}
