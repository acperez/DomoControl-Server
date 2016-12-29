package services.philips_hue

import java.awt.Color
import javax.inject.{Inject, Singleton}

import models.config.PhilipsConf
import services.common.{ConfigLoader, DomoService, DomoSwitch, SceneManager}
import play.api.{Environment, Logger}
import play.api.inject.ApplicationLifecycle
import play.api.libs.json.{JsValue, Json}

@Singleton
class LightService @Inject() (
  configLoader: ConfigLoader,
  sceneManager: SceneManager,
  env: Environment,
  appLifecycle: ApplicationLifecycle) extends DomoService {

  val lightListener = LightListener(this)

  override def serviceId: Int = LightService.serviceId

  override def serviceName: String = LightService.serviceName

  override def init(): Unit = {
    Logger.info("Init Philips Hue service")
    sceneManager.bootstrap()
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

  def getLightStatus(id: String) = LightControl.getLightStatus(id)

  override def getSwitches = Json.toJson(getLights)

  override def getSwitch(id: String): JsValue = Json.toJson(getLightStatus(id))

  override def setSwitchesStatus(status: Boolean) = getLights.foreach( light => LightControl.setLightStatus(light.id, status))

  override def setSwitchesExtra(data: Long) = getLights.foreach( light => setLightColor("4", data))

  override def setSwitchStatus(id: String, status: Boolean) = LightControl.setLightStatus(id, status)

  override def setSwitchExtra(id: String, data: Long) = setLightColor(id, data)

  def setLightColor(id: String, color: Long) = {
    val r = (color >> 16).toInt
    val g = ((color >> 8) & 255).toInt
    val b = (color & 255).toInt
    val rgb = Color.RGBtoHSB(r, g, b, null)
    //LightControl.setLightColor(id, rgb)
  }

  override def setSwitchesExtraPost(data: JsValue): Unit = {
    val sceneId = (data \ "sceneId").as[Int]

    sceneManager.get(sceneId) match {
      case None =>
        Logger.warn(s"No scene with id $sceneId")

      case Some(scene) =>
        val colors = scene.colors.map { colorStr =>
          val colorRGB = Color.decode(colorStr)
          Array(colorRGB.getRed, colorRGB.getGreen, colorRGB.getBlue)
        }

        val lights = getLights
        val actions =
          if (colors.size > lights.size) lights.zip(colors)
          else lights.zip(Stream.continually(colors).flatten)

        actions.foreach { case (light: DomoSwitch, color: Array[Int]) =>
          LightControl.setLightColor(light.id, color)
        }
    }
  }

  def getScenes(): JsValue = {
    Json.toJson(sceneManager.getAll())
  }

  override def setSwitchExtraPost(id: String, data: JsValue): Unit = ???
}

object LightService {
  val serviceId = 1
  val serviceName = "Philips Hue"
}
