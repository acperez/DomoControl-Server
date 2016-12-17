package models.config

import play.api.libs.functional.syntax._
import play.api.libs.json._
import services.philips_hue.LightService

import scala.util.Random

case class PhilipsConf private(
  id: Int,
  name: String,
  server: Option[String],
  user: Option[String]) extends DomoConfiguration(id, name)


object PhilipsConf {

  def apply(server: Option[String], user: Option[String]) =
    new PhilipsConf(LightService.serviceId, LightService.serviceName, server, user)

  implicit val reads: Reads[PhilipsConf] = (
    (JsPath \ "server").readNullable[String] and
    (JsPath \ "user").readNullable[String]
  )((server, user) => PhilipsConf.apply(server, user))
  //)(PhilipsConf.apply _)

  implicit val writes: OWrites[PhilipsConf] = (
    (JsPath \ "id").write[Int] and
    (JsPath \ "name").write[String] and
    (JsPath \ "server").write[Option[String]] and
    (JsPath \ "user").write[Option[String]]
  )(unlift(PhilipsConf.unapply))

  def generateUsername(): String = {
    val chars = "1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
    val rand = new Random()

    val key = for (index <- 1 to 16) yield chars.charAt(rand.nextInt(chars.length))
    key.mkString
  }
}
