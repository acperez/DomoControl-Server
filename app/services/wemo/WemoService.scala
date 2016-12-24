package services.wemo

import javax.inject.{Inject, Singleton}

import models.config.WemoConf
import play.api.Logger
import play.api.inject.ApplicationLifecycle
import play.api.libs.json.{JsValue, Json}
import services.common.{ConfigLoader, DomoService}

@Singleton
class WemoService @Inject() (
  configLoader: ConfigLoader,
  appLifecycle: ApplicationLifecycle) extends DomoService {

  override def serviceId: Int = WemoService.serviceId

  override def serviceName: String = WemoService.serviceName

  override def init(): Unit = {
    Logger.info(f"Init $serviceName service")
    val conf = getConf
    Logger.info(f"$serviceName config: " + conf)
    connect()
  }

  def stop(): Unit = {
    Logger.info(f"Stop $serviceName service")
  }

  def getServiceConf: WemoConf = configLoader.getConfig(WemoConf(Seq()))

  override def getConf = Json.toJson(getServiceConf)

  def setServiceConf(wemoConf: WemoConf) = configLoader.setConfig(wemoConf)

  override def setConf(conf: JsValue): Unit = setServiceConf(conf.as[WemoConf])

  override def connected: Boolean = WemoControl.isConnected(getServiceConf)

  override def connect() = WemoControl.connect(this)

  override def disconnect() = {}

  def getWemoDevices = WemoControl.getDevices(getServiceConf)

  override def getSwitches: JsValue = Json.toJson(getWemoDevices)

  def getWemoStatus(id: String) = WemoControl.getSwitchStatus(getServiceConf, id)

  override def getSwitch(id: String): JsValue = Json.toJson(getWemoStatus(id))

  override def setSwitchStatus(id: String, status: Boolean): Unit = WemoControl.setSwitchStatus(getServiceConf, id, status)

  override def getConnectionStatus: JsValue = Json.toJson(connected)

  override def setSwitchesStatus(status: Boolean): Unit = {
    getServiceConf.devices.foreach { device =>
      WemoControl.setSwitchStatus(getServiceConf, device.serial, status)
    }
  }

  override def setSwitchesExtra(status: Long): Unit = {}

  override def setSwitchExtra(id: String, status: Long): Unit = {}
}

object WemoService {
  val serviceId = 2
  val serviceName = "Wemo"
}