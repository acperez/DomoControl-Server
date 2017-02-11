package models.config

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, OWrites, Reads}

case class PhilipsScene(
  name: String,
  lights: Seq[String],
  colors: Seq[String],
  default: Boolean)

object PhilipsScene {

  implicit val reads: Reads[PhilipsScene] = (
    (JsPath \ "name").read[String] and
    (JsPath \ "lights").read[Seq[String]] and
    (JsPath \ "colors").read[Seq[String]] and
    (JsPath \ "default").read[Boolean]
  )((name, lights, colors, default) => PhilipsScene.apply(name, lights, colors, default))

  implicit val writes: OWrites[PhilipsScene] = (
    (JsPath \ "name").write[String] and
    (JsPath \ "lights").write[Seq[String]] and
    (JsPath \ "colors").write[Seq[String]] and
    (JsPath \ "default").write[Boolean]
  )(scene => (scene.name.toLowerCase(), scene.lights, scene.colors, scene.default))
}
