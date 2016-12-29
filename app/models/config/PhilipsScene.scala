package models.config

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, OWrites, Reads}

case class PhilipsScene(
  id: Int,
  name: String,
  colors: Seq[String])

object PhilipsScene {

  implicit val reads: Reads[PhilipsScene] = (
    (JsPath \ "id").read[Int] and
    (JsPath \ "name").read[String] and
    (JsPath \ "colors").read[Seq[String]]
  )((id, name, colors) => PhilipsScene.apply(id, name, colors))

  implicit val writes: OWrites[PhilipsScene] = (
    (JsPath \ "id").write[Int] and
    (JsPath \ "name").write[String] and
    (JsPath \ "colors").write[Seq[String]]
  )(unlift(PhilipsScene.unapply))
}
