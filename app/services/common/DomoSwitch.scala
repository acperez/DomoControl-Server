package services.common

import play.api.libs.json.Json

trait DomoSwitchBase {
  def serviceId: Int
  def id: String
  def status: Boolean
  def name: String
  def available: Boolean
}

case class DomoSwitch(
  serviceId: Int,
  id: String,
  status: Boolean,
  name: String,
  available: Boolean
)  extends DomoSwitchBase

object DomoSwitch {
  implicit val domoSwitchFormat = Json.format[DomoSwitch]
}