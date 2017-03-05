package models.config

import play.api.libs.functional.syntax._
import play.api.libs.json._
import services.wemo.{WemoDevice, WemoService}

case class WemoConf private(
  id: Int,
  name: String,
  devices: Seq[WemoDevice]) extends DomoConfiguration(id, name)

object WemoConf {

  def apply(devices: Seq[WemoDevice]) =
    new WemoConf(WemoService.serviceId, WemoService.serviceName, devices)

  implicit val reads: Reads[WemoConf] = (
    (JsPath \ "id").read[Int] and
    (JsPath \ "name").read[String] and
    (JsPath \ "devices").read[Seq[WemoDevice]]
  )((id, name, devices) => WemoConf.apply(devices))

  implicit val writes: OWrites[WemoConf] = (
    (JsPath \ "id").write[Int] and
    (JsPath \ "name").write[String] and
    (JsPath \ "devices").write[Seq[WemoDevice]]
  )(unlift(WemoConf.unapply))
}
