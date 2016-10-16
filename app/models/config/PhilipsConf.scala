package models.config

import play.api.libs.functional.syntax._
import play.api.libs.json._
import services.common.Config

case class PhilipsConf private(
  server: Option[String],
  user: Option[String],
  id: String) extends Config(id)

object PhilipsConf {
  implicit val jsonFormat = Json.format[PhilipsConf]

  val writes: OWrites[PhilipsConf] = (
    (JsPath \ "server").write[Option[String]] and
    (JsPath \ "user").write[Option[String]] and
    (JsPath \ "id").write[String]
  )(unlift(PhilipsConf.unapply))

  def apply(server: Option[String], user: Option[String]) = new PhilipsConf(server, user, PhilipsHue.id)
}