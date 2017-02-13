package request_models.philips_hue

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads}

case class LightsColorRequest(lightIds: Seq[String], r: Int, g: Int, b: Int) {
  def color = Array(r, g, b)
}

object LightsColorRequest {
  implicit val reads: Reads[LightsColorRequest] = (
    (JsPath \ "lightIds").read[Seq[String]] and
    (JsPath \ "color" \ "r").read[Int] and
    (JsPath \ "color" \ "g").read[Int] and
    (JsPath \ "color" \ "b").read[Int]
  )((ids, r, g, b) => LightsColorRequest.apply(ids, r, g, b))
}
