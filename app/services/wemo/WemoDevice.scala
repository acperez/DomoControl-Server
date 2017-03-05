package services.wemo

import java.net.URL

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, OWrites, Reads}
import services.common.DomoSwitch
import services.wemo.WemoDeviceType.WemoDeviceType
import services.wemo.ssdp.Device

import scala.concurrent.{ExecutionContext, Future}

case class WemoDevice(
  serial: String,
  name: String,
  description: String,
  url: String,
  deviceType: WemoDeviceType,
  failedConnections: Int) {

  val CONNECTION_RETRIES: Int = 0

  lazy private val device = Device(serial, new URL(url), deviceType.toString)

  def setFailedConnections(value: Int): WemoDevice = copy(failedConnections = value)

  def setState(state: Boolean, retries: Int = CONNECTION_RETRIES)(implicit ec: ExecutionContext): Future[Boolean] = device.setState(state, retries)

  def getState(retries: Int = CONNECTION_RETRIES)(implicit ec: ExecutionContext): Future[DomoSwitch] = device.getState(retries)
    .map(state => DomoSwitch(WemoService.serviceId, serial, state, name, available = true))

  def getUsage(retries: Int = CONNECTION_RETRIES)(implicit ec: ExecutionContext): Future[Option[WemoMonitorData]] = device.getUsage(retries)
}

object WemoDevice {

  def apply(id: String, url: String, deviceType: String, failedConnections: Int): WemoDevice =
    new WemoDevice(id, id, id, url, WemoDeviceType(deviceType), failedConnections)

  implicit val reads: Reads[WemoDevice] = (
    (JsPath \ "serial").read[String] and
    (JsPath \ "name").read[String] and
    (JsPath \ "description").read[String] and
    (JsPath \ "url").read[String] and
    (JsPath \ "deviceType").read[String] and
    (JsPath \ "failedConnections").read[Int]
  )((serial, name, description, url, deviceType, failedConnections) =>
    WemoDevice.apply(serial, name, description, url, WemoDeviceType(deviceType), failedConnections))

  implicit val writes: OWrites[WemoDevice] = (
    (JsPath \ "serial").write[String] and
    (JsPath \ "name").write[String] and
    (JsPath \ "description").write[String] and
    (JsPath \ "url").write[String] and
    (JsPath \ "deviceType").write[WemoDeviceType] and
    (JsPath \ "failedConnections").write[Int]
  )(unlift(WemoDevice.unapply))
}

object WemoDeviceType extends Enumeration {
  type WemoDeviceType = Value
  val Monitor = Value("urn:Belkin:device:insight:1")
  val Plug = Value("urn:Belkin:device:controllee:1")
  val Unknown = Value("Unknown")

  def apply(deviceType: String): WemoDeviceType = {
    deviceType match {
      case "urn:Belkin:device:insight:1" => Monitor
      case "urn:Belkin:device:controllee:1" => Plug
      case _ => Unknown
    }
  }
}
