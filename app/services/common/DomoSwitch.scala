package services.common

import play.api.libs.json.Json

trait DomoSwitchBase {
  def serviceId: Int
  def id: Int
  def status: Boolean
  def name: String
}

case class DomoSwitch(
  serviceId: Int,
  id: Int,
  status: Boolean,
  name: String
)  extends DomoSwitchBase

object DomoSwitch {
  implicit val personFormat = Json.format[DomoSwitch]
}