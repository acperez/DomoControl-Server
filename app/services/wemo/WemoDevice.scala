package services.wemo

import java.net.URL

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, OWrites, Reads}
import services.wemo.ssdp.Device

case class WemoDevice(
  serial: String,
  name: String,
  description: String,
  url: String,
  deviceType: String) {

  lazy private val device = Device(serial, new URL(url), deviceType)

  def setState(state: Boolean) = device.setState(state)

  def getState: Boolean = device.getState()

  override def equals(other: Any) = other match {
    case that: WemoDevice =>
      that.serial.equalsIgnoreCase(this.serial)
    case _ => false
  }
  override def hashCode = serial.toUpperCase.hashCode
}

object WemoDevice {

  def apply(id: String, url: String, deviceType: String) = {
    new WemoDevice(id, id, id, url, deviceType)
  }

  implicit val reads: Reads[WemoDevice] = (
    (JsPath \ "serial").read[String] and
    (JsPath \ "name").read[String] and
    (JsPath \ "description").read[String] and
    (JsPath \ "url").read[String] and
    (JsPath \ "deviceType").read[String]
  )((serial, name, description, url, deviceType) => WemoDevice.apply(serial, name, description, url, deviceType))

  implicit val writes: OWrites[WemoDevice] = (
    (JsPath \ "serial").write[String] and
    (JsPath \ "name").write[String] and
    (JsPath \ "description").write[String] and
    (JsPath \ "url").write[String] and
    (JsPath \ "deviceType").write[String]
  )(unlift(WemoDevice.unapply))
}
