package controllers

import javax.inject.Inject

import akka.actor.ActorSystem
import models.config.PhilipsScene
import play.api.Logger
import play.api.libs.json.{JsObject, JsPath, Json}
import play.api.mvc._
import request_models.philips_hue.LightsColorRequest
import services.common.{DomoPhilipsService, DomoServices}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

class DomoPhilipsController @Inject()(akkaSystem: ActorSystem, domoServices: DomoServices) extends Controller {

  implicit  val customExecutionContext: ExecutionContext = akkaSystem.dispatchers.lookup("custom-context")

  case class DomoRequest[A](service: DomoPhilipsService, request: Request[A]) extends WrappedRequest[A](request)

  def DomoAction(itemId: Int) = new ActionBuilder[DomoRequest] with ActionRefiner[Request, DomoRequest] {
    def refine[A](request: Request[A]): Future[Either[Result, DomoRequest[A]]] = Future.successful {
      domoServices.services.get(itemId) match {
        case None => Left(NotFound)
        case Some(service: DomoPhilipsService) =>
          Right(DomoRequest(service, request))
        case Some(_) => Left(NotFound)
      }
    }
  }

  def loadScene(id: Int, sceneId: String): Action[AnyContent] = DomoAction(id).async { domoRequest =>
    domoRequest.service.loadScene(sceneId)
  }

  def removeScene(id: Int, sceneId: String): Action[AnyContent] = DomoAction(id).async { domoRequest =>
    domoRequest.service.removeScene(sceneId)
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

              domoRequest.service.setLightsColor(request.lightIds, request.color).onComplete {
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

              domoRequest.service.saveScene(scene).onComplete {
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
