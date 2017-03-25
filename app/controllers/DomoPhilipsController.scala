package controllers

import javax.inject._

import akka.actor.ActorSystem
import models.config.PhilipsScene
import play.api.Logger
import play.api.libs.json.{JsObject, Json}
import play.api.mvc._
import request_models.philips_hue.LightsColorRequest
import services.common.{DomoPhilipsService, DomoServiceManager}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

@Singleton
class DomoPhilipsController @Inject()(akkaSystem: ActorSystem, val serviceManager: DomoServiceManager) extends Controller with DomoAction[DomoPhilipsService] {

  implicit  val customExecutionContext: ExecutionContext = akkaSystem.dispatchers.lookup("custom-context")

  def loadScene(id: Int, sceneId: String): Action[AnyContent] = DomoAction(id).async { domoRequest =>
    domoRequest.service.asInstanceOf[DomoPhilipsService].loadScene(sceneId)
  }

  def removeScene(id: Int, sceneId: String): Action[AnyContent] = DomoAction(id).async { domoRequest =>
    domoRequest.service.asInstanceOf[DomoPhilipsService].removeScene(sceneId)
  }

  def setLightsColor(id: Int): Action[AnyContent] = DomoAction(id).async { domoRequest =>
    val promise = Promise[Result]
    Future {
      domoRequest.request.body.asJson match {
        case None =>
          promise.success(BadRequest("Expecting Json data"))

        case Some(json) =>
          val lightRequest = json.asOpt[LightsColorRequest]

          lightRequest match {
            case None => promise.success(BadRequest("Missing or invalid request params"))
            case Some(request) =>

              domoRequest.service.asInstanceOf[DomoPhilipsService].setLightsColor(request.lightIds, request.color).onComplete {
                case Success(result) => promise.success(result)
                case Failure(ex) =>
                  Logger.error(f"Failed to set lightts color: ${ex.getMessage}")
                  promise.success(InternalServerError)
              }
          }
      }
    }

    promise.future
  }

  def saveScene(id: Int): Action[AnyContent] = DomoAction(id).async { domoRequest =>
    val promise = Promise[Result]
    Future {
      domoRequest.request.body.asJson match {
        case None =>
          promise.success(BadRequest("Expecting Json data"))

        case Some(json) =>
          val sceneValue = (json \ "scene").as[JsObject] ++ Json.obj("default" -> false)

          sceneValue.asOpt[PhilipsScene] match {
            case None => promise.success(BadRequest("Missing or invalid request params"))
            case Some(scene) =>

              domoRequest.service.asInstanceOf[DomoPhilipsService].saveScene(scene).onComplete {
                case Success(result) => promise.success(result)
                case Failure(ex) =>
                  Logger.error(f"Failed to save scene: ${ex.getMessage}")
                  promise.success(InternalServerError)
              }
          }
      }
    }

    promise.future
  }
}
