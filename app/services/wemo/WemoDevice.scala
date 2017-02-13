package services.wemo

import java.net.URL

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, OWrites, Reads}
import services.common.DomoSwitch
import services.wemo.ssdp.Device

import scala.concurrent.{ExecutionContext, Future}

case class WemoDevice(
  serial: String,
  name: String,
  description: String,
  url: String,
  deviceType: String,
  failedConnections: Int) {

  val CONNECTION_RETRIES: Int = 0

  lazy private val device = Device(serial, new URL(url), deviceType)

  def setState(state: Boolean, retries: Int = CONNECTION_RETRIES)(implicit ec: ExecutionContext): Future[Boolean] = device.setState(state, retries)

  def getState(retries: Int = CONNECTION_RETRIES)(implicit ec: ExecutionContext): Future[DomoSwitch] = device.getState(retries)
    .map(state => DomoSwitch(WemoService.serviceId, serial, state, name, available = true))

  override def equals(other: Any) = other match {
    case that: WemoDevice =>
      that.serial.equalsIgnoreCase(this.serial)
    case _ => false
  }
  override def hashCode = serial.toUpperCase.hashCode
}

object WemoDevice {

  def apply(id: String, url: String, deviceType: String, failedConnections: Int) = {
    new WemoDevice(id, id, id, url, deviceType, failedConnections)
  }

  implicit val reads: Reads[WemoDevice] = (
    (JsPath \ "serial").read[String] and
    (JsPath \ "name").read[String] and
    (JsPath \ "description").read[String] and
    (JsPath \ "url").read[String] and
    (JsPath \ "deviceType").read[String] and
    (JsPath \ "failedConnections").read[Int]
  )((serial, name, description, url, deviceType, failedConnections) =>
    WemoDevice.apply(serial, name, description, url, deviceType, failedConnections))

  implicit val writes: OWrites[WemoDevice] = (
    (JsPath \ "serial").write[String] and
    (JsPath \ "name").write[String] and
    (JsPath \ "description").write[String] and
    (JsPath \ "url").write[String] and
    (JsPath \ "deviceType").write[String] and
    (JsPath \ "failedConnections").write[Int]
  )(unlift(WemoDevice.unapply))
}
