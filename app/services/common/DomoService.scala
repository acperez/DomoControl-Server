package services.common

import play.api.libs.json.{JsObject, JsValue, Json, Writes}

trait DomoService {
  def id: Int
  def name: String
  def connected: Boolean

  def init(): Unit
  def stop(): Unit
  def cron(): Unit
  def connect(): Unit
  def disconnect(): Unit

  def getConf: JsValue
  def setConf(conf: JsValue): Unit
  def getConnectionStatus: JsValue
}

object DomoService {
  implicit val serviceWrites = new Writes[DomoService] {
    def writes(domoService: DomoService): JsObject = Json.obj(
      "id" -> domoService.id,
      "name" -> domoService.name
    )
  }
}
