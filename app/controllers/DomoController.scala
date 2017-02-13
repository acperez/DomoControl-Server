package controllers

import akka.actor.ActorSystem
import javax.inject._

import play.api.Logger
import play.api.libs.json.{JsArray, JsObject, Json}
import play.api.mvc._
import services.common.{DomoService, DomoServices}

import scala.concurrent.{ExecutionContext, Future, Promise}

/**
 * This controller creates an `Action` that demonstrates how to write
 * simple asynchronous code in a controller. It uses a timer to
 * asynchronously delay sending a response for 1 second.
 *
 * @param akkaSystem We need the `ActorSystem`'s `Scheduler` to
 * run code after a delay.
 */
@Singleton
class DomoController @Inject()(akkaSystem: ActorSystem, domoServices: DomoServices) extends Controller {

  implicit val customExecutionContext: ExecutionContext = akkaSystem.dispatchers.lookup("custom-context")

  def systems = Action {
    Ok(Json.toJson(domoServices.services.values.toList))
  }

  case class DomoRequest[A](service: DomoService, request: Request[A]) extends WrappedRequest[A](request)

  def DomoAction(itemId: Int) = new ActionBuilder[DomoRequest] with ActionRefiner[Request, DomoRequest] {
    def refine[A](request: Request[A]): Future[Either[Result, DomoRequest[A]]] = Future.successful {
      domoServices.services.get(itemId) match {
        case None => Left(NotFound)
        case Some(service) =>
          Right(DomoRequest(service, request))
      }
    }
  }

  def getConf(id: Int): Action[AnyContent] = DomoAction(id) { domoRequest =>
    Ok(domoRequest.service.getConf)
  }

  def setConf(id: Int): Action[AnyContent] = DomoAction(id) { domoRequest =>
    val conf = domoRequest.request.body.asJson.get
    domoRequest.service.setConf(conf)
    Ok("ok")
  }

  def getConnectionStatus(id: Int): Action[AnyContent] = DomoAction(id) { domoRequest =>
    Ok(domoRequest.service.getConnectionStatus)
  }

  def connect(id: Int): Action[AnyContent] = DomoAction(id) { domoRequest =>
    domoRequest.service.connect()
    Ok("ok")
  }

  def disconnect(id: Int): Action[AnyContent] = DomoAction(id) { domoRequest =>
    domoRequest.service.disconnect()
    Ok("ok")
  }

  def getDomoControlData: Action[AnyContent] = Action.async {
    val promise = Promise[Result]
    Future {
      val services = domoServices.services
      val systemObjects = services.map { case (id, service) =>
          service.getSwitches.map { switches =>
            id.toString -> Json.obj(
              "name" -> service.name,
              "switches" -> switches
            )
          }
        }

      Future.sequence(systemObjects).map { data =>
        val systems = JsObject(data.toMap)
        val scenes = domoServices.lightScenes.as[JsArray]
        val result = Ok(views.js.domoControlData.render(systems, scenes)).as("text/javascript utf-8")
        promise.success(result)
      }.recover { case exception =>
        Logger.error(f"Error getting systems js: ${exception.getMessage}")
        promise.success(NotFound)
      }
    }

    promise.future
  }

}
