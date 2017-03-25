package models.config

import play.api.libs.functional.syntax.unlift
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, OWrites, Reads}
import services.virtual_switch.{VirtualDevice, VirtualService}

case class VirtualConf private(
    id: Int,
    name: String,
    devices: Seq[VirtualDevice]) extends DomoConfiguration(id, name)

object VirtualConf {

  def apply(devices: Seq[VirtualDevice]) =
    new VirtualConf(VirtualService.serviceId, VirtualService.serviceName, devices)

  implicit val reads: Reads[VirtualConf] = (
    (JsPath \ "id").read[Int] and
    (JsPath \ "name").read[String] and
    (JsPath \ "devices").read[Seq[VirtualDevice]]
  )((id, name, devices) => VirtualConf.apply(devices))

  implicit val writes: OWrites[VirtualConf] = (
    (JsPath \ "id").write[Int] and
    (JsPath \ "name").write[String] and
    (JsPath \ "devices").write[Seq[VirtualDevice]]
  )(unlift(VirtualConf.unapply))
}
