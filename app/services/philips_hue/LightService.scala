package services.philips_hue

import java.awt.Color
import javax.inject.{Inject, Singleton}

import models.config.PhilipsConf
import services.common.{ConfigLoader, DomoService}
import play.api.Logger
import play.api.inject.ApplicationLifecycle
import play.api.libs.json.{JsValue, Json}

@Singleton
class LightService @Inject() (
  configLoader: ConfigLoader,
  appLifecycle: ApplicationLifecycle) extends DomoService {

  val lightListener = LightListener(this)

  override def serviceId: Int = LightService.serviceId

  override def serviceName: String = LightService.serviceName

  override def init(): Unit = {
    Logger.info("Init Philips Hue service")
    val conf = getConf
    Logger.info("Lights config: " + conf)
    connect()
  }

  override def stop(): Unit = {
    Logger.info("Stop Philips Hue service")
    disconnect()
  }

  def onConnect(): Unit = {
    val lights = getSwitches
    Logger.info(f"Light list: $lights")
  }

  def getServiceConf: PhilipsConf = configLoader.getConfig(PhilipsConf(None, None))

  override def getConf = Json.toJson(getServiceConf)

  def setServiceConf(philipsConf: PhilipsConf) = configLoader.setConfig(philipsConf)

  override def setConf(conf: JsValue) = setServiceConf(conf.as[PhilipsConf])

  override def connected: Boolean = LightControl.isConnected(getServiceConf)

  override def getConnectionStatus: JsValue = Json.toJson(connected)

  override def connect() = LightControl.connect(getServiceConf, lightListener)

  override def disconnect() = LightControl.disconnect(lightListener)

  def getLights = LightControl.getLights

  def getLightStatus(id: Int) = LightControl.getLightStatus(id)

  override def getSwitches = Json.toJson(getLights)

  override def getSwitch(id: Int): JsValue = Json.toJson(getLightStatus(id))

  override def setSwitchesStatus(status: Boolean) = getLights.foreach( light => LightControl.setLightStatus(light.id, status))

  override def setSwitchesExtra(data: Long) = getLights.foreach( light => setLightColor(4, data))

  override def setSwitchStatus(id: Int, status: Boolean) = LightControl.setLightStatus(id, status)

  override def setSwitchExtra(id: Int, data: Long) = setLightColor(4, data)

  def setLightColor(id: Int, color: Long) = {
    val r = (color >> 16).toInt
    val g = ((color >> 8) & 255).toInt
    val b = (color & 255).toInt
    val rgb = Color.RGBtoHSB(r, g, b, null)
    LightControl.setLightColor(id, rgb)
  }
}

object LightService {
  val serviceId = 1
  val serviceName = "Philips Hue"
}
