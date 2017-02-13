package services.common

import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.Result

import scala.concurrent.{ExecutionContext, Future}

trait DomoSwitchService extends DomoService {

  def getSwitchesWithId()(implicit ec: ExecutionContext): Future[JsObject] = {
    getSwitches
      .map { switches =>
        Json.obj(
          "id" -> id.toString,
          "switches" -> switches
        )}
  }

  def getSwitches: Future[JsValue]
  def getSwitch(id: String): Future[Result]

  def setSwitchesStatus(status: Boolean): Future[Result]
  def setSwitchStatus(id: String, status: Boolean): Future[Result]
}

