package services.wemo

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class WemoMonitorData(
  id: String,
  timestamp: Long,
  state: Boolean,
  lastStateChange: Long,
  lastOnFor: Double,
  onToday: Double,
  onTotal: Double,
  timeSpan: Long,
  averagePowerW: Double,
  currentW: Double,
  energyTodayWh: Double,
  energyTotalWh: Double,
  standbyLimitW: Double)

object WemoMonitorData {

  implicit val reads: Reads[WemoMonitorData] = (
    (JsPath \ "id").read[String] and
    (JsPath \ "timestamp").read[Long] and
    (JsPath \ "state").read[Boolean] and
    (JsPath \ "lastStateChange").read[Long] and
    (JsPath \ "lastOnFor").read[Double] and
    (JsPath \ "onToday").read[Double] and
    (JsPath \ "onTotal").read[Double] and
    (JsPath \ "timeSpan").read[Long] and
    (JsPath \ "averagePowerW").read[Double] and
    (JsPath \ "currentW").read[Double] and
    (JsPath \ "energyTodayWh").read[Double] and
    (JsPath \ "energyTotalWh").read[Double] and
    (JsPath \ "standbyLimitW").read[Double]
  ) (WemoMonitorData.apply _)

  implicit val writes: OWrites[WemoMonitorData] = (
    (JsPath \ "id").write[String] and
    (JsPath \ "timestamp").write[Long] and
    (JsPath \ "state").write[Boolean] and
    (JsPath \ "lastStateChange").write[Long] and
    (JsPath \ "lastOnFor").write[Double] and
    (JsPath \ "onToday").write[Double] and
    (JsPath \ "onTotal").write[Double] and
    (JsPath \ "timeSpan").write[Long] and
    (JsPath \ "averagePowerW").write[Double] and
    (JsPath \ "currentW").write[Double] and
    (JsPath \ "energyTodayWh").write[Double] and
    (JsPath \ "energyTotalWh").write[Double] and
    (JsPath \ "standbyLimitW").write[Double]
  )(unlift(WemoMonitorData.unapply))
}
