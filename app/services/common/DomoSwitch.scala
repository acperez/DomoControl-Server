package services.common

import play.api.libs.json.Json

trait DomoSwitchBase {
  def serviceId: Int
  def id: String
  def status: Boolean
  def name: String
}

case class DomoSwitch(
  serviceId: Int,
  id: String,
  status: Boolean,
  name: String
)  extends DomoSwitchBase

object DomoSwitch {
  implicit val personFormat = Json.format[DomoSwitch]
}