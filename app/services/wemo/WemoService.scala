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

  def getServiceConf: WemoConf = configLoader.getConfig(WemoConf(None, None, None, None))

  override def getConf = Json.toJson(getServiceConf)

  def setServiceConf(wemoConf: WemoConf) = configLoader.setConfig(wemoConf)

  override def setConf(conf: JsValue): Unit = setServiceConf(conf.as[WemoConf])

  override def connected: Boolean = WemoControl.isConnected(getServiceConf)

  override def connect() = WemoControl.connect(getServiceConf, this)

  override def disconnect() = {}

  def getWemoDevices = WemoControl.getDevices(getServiceConf)

  override def getSwitches: JsValue = Json.toJson(getWemoDevices)

  def getWemoStatus(id: Int) = WemoControl.getSwitchStatus(getServiceConf, id)

  override def getSwitch(id: Int): JsValue = Json.toJson(getWemoStatus(id))

  override def setSwitchStatus(id: Int, status: Boolean): Unit = WemoControl.setSwitchStatus(getServiceConf, id, status)

  override def getConnectionStatus: JsValue = Json.toJson(connected)

  override def setSwitchesStatus(status: Boolean): Unit = WemoControl.setSwitchStatus(getServiceConf, 0, status)

  override def setSwitchesExtra(status: Long): Unit = {}

  override def setSwitchExtra(id: Int, status: Long): Unit = {}
}

object WemoService {
  val serviceId = 2
  val serviceName = "Wemo"
}