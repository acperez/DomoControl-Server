package models.config

import java.net.URL

import play.api.libs.functional.syntax._
import play.api.libs.json._
import services.wemo.{Device, WemoService}

case class WemoConf private(
  id: Int,
  name: String,
  description: Option[String],
  url: Option[String],
  deviceType: Option[String],
  deviceName: Option[String]) extends DomoConfiguration(id, name) {

  val device: Option[Device] = if (url.isEmpty || deviceType.isEmpty) None
    else Some(new Device(new URL(url.get), deviceType.get, Some(name), description))
}

object WemoConf {

  def apply(description: Option[String], url: Option[String], deviceType: Option[String], deviceName: Option[String]) =
    new WemoConf(WemoService.serviceId, WemoService.serviceName, description, url, deviceType, deviceName)

  implicit val reads: Reads[WemoConf] = (
    (JsPath \ "description").readNullable[String] and
    (JsPath \ "url").readNullable[String] and
    (JsPath \ "deviceType").readNullable[String] and
    (JsPath \ "deviceName").readNullable[String]
  )((description, url, deviceType, deviceName) => WemoConf.apply(description, url, deviceType, deviceName))

  implicit val writes: OWrites[WemoConf] = (
    (JsPath \ "id").write[Int] and
    (JsPath \ "name").write[String] and
    (JsPath \ "description").write[Option[String]] and
    (JsPath \ "url").write[Option[String]] and
    (JsPath \ "deviceType").write[Option[String]] and
    (JsPath \ "deviceName").write[Option[String]]
  )(unlift(WemoConf.unapply))
}
