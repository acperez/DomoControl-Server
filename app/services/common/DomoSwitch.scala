package services.common

import play.api.libs.json.{Json, OFormat}

trait DomoSwitchBase {
  def serviceId: Int
  def id: String
  def status: Boolean
  def name: String
  def alias: Option[String]
  def available: Boolean
}

case class DomoSwitch(
  serviceId: Int,
  id: String,
  status: Boolean,
  name: String,
  alias: Option[String],
  available: Boolean
)  extends DomoSwitchBase

object DomoSwitch {
  implicit val domoSwitchFormat: OFormat[DomoSwitch] = Json.format[DomoSwitch]
}