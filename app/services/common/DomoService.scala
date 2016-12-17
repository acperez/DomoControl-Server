package services.common

import play.api.libs.json.JsValue

trait DomoService {
  def serviceId: Int
  def serviceName: String
  def connected: Boolean

  def init(): Unit
  def stop(): Unit
  def connect(): Unit
  def disconnect(): Unit

  def getConf: JsValue
  def setConf(conf: JsValue): Unit
  def getConnectionStatus: JsValue

  def getSwitches: JsValue
  def getSwitch(id: Int): JsValue
  def setSwitchesStatus(status: Boolean): Unit
  def setSwitchStatus(id: Int, status: Boolean): Unit
  def setSwitchesExtra(status: Long): Unit
  def setSwitchExtra(id: Int, status: Long): Unit
}
