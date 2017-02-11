package controllers

import akka.actor.ActorSystem
import javax.inject._

import play.api.libs.json.{JsArray, JsObject, Json}
import play.api.mvc._
import services.common.{DomoService, DomoServices}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

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

  val customExecutionContext: ExecutionContext = akkaSystem.dispatchers.lookup("custom-context")

  def systems = Action {
    Ok(Json.toJson(domoServices.services.values.toList))
  }

  case class DomoRequest[A](service: DomoService, request: Request[A]) extends WrappedRequest[A](request)

  def DomoAction(itemId: Int) = new ActionBuilder[DomoRequest] with ActionRefiner[Request, DomoRequest] {
    def refine[A](input: Request[A]) = Future.successful {
      domoServices.services.get(itemId) match {
        case None => Left(NotFound)
        case Some(serviceContainer) =>
          Right(DomoRequest(serviceContainer.service, input))
      }
    }
  }

  def getAllSwitches = Action {
    val switches = domoServices.services.map { case(id, serviceContainer) =>
      Json.obj(
        "id" -> id.toString,
        "switches" -> serviceContainer.service.getSwitches
      )
    }.toSeq

    val v = Json.toJson(switches)
    Ok(v)
  }

  def getConf(id: Int) = DomoAction(id) { domoRequest =>
    Ok(domoRequest.service.getConf)
  }

  def setConf(id: Int) = DomoAction(id) { domoRequest =>
    val conf = domoRequest.request.body.asJson.get
    domoRequest.service.setConf(conf)
    Ok("ok")
  }

  def getConnectionStatus(id: Int) = DomoAction(id) { domoRequest =>
    Ok(domoRequest.service.getConnectionStatus)
  }

  def connect(id: Int) = DomoAction(id) { domoRequest =>
    domoRequest.service.connect()
    Ok("ok")
  }

  def disconnect(id: Int) = DomoAction(id) { domoRequest =>
    domoRequest.service.disconnect()
    Ok("ok")
  }

  def getSwitches(id: Int) = DomoAction(id) { domoRequest =>
    Ok(domoRequest.service.getSwitches)
  }

  def setSwitchesStatus(id: Int, status: Int) = DomoAction(id) { domoRequest =>
    domoRequest.service.setSwitchesStatus(status > 0)
    Ok("ok")
  }

  def setSwitchesExtra(id: Int, switches: String, data: String) = DomoAction(id).async { domoRequest =>
    implicit val ec = customExecutionContext
    val promise = Promise[Result]

    Future {
      val result = domoRequest.service.setSwitchesExtra(switches, data)
      result.onComplete { _ =>
        promise.success(Ok("ok"))
      }
    }

    promise.future
  }

  def getSwitch(id: Int, switchId: String) = DomoAction(id) { domoRequest =>
    Ok(domoRequest.service.getSwitch(switchId))
  }

  def setSwitchStatus(id: Int, switchId: String, status: Int) = DomoAction(id) { domoRequest =>
    domoRequest.service.setSwitchStatus(switchId, status > 0)
    Ok("ok")
  }

  def setSwitchExtra(id: Int, switchId: String, data: String) = DomoAction(id).async { domoRequest =>
    implicit val ec = customExecutionContext
    val promise = Promise[Result]

    Future {
      val result = domoRequest.service.setSwitchExtra(switchId, data)
      result.onComplete { _ =>
        promise.success(Ok("ok"))
      }
    }

    promise.future
  }

  def setSwitchesExtraPost(id: Int) = DomoAction(id).async { domoRequest =>
    implicit val ec = customExecutionContext

    val promise = Promise[Result]
    domoRequest.request.body.asJson match {
      case None =>
        promise.success(BadRequest("Expecting Json data"))

      case Some(json) =>
        Future {
          val result = domoRequest.service.setSwitchesExtraPost(json)
          result.onComplete {
            case Success(status) => promise.success(new Status(status))
            case Failure(e) => promise.success(InternalServerError)
          }
        }
    }

    promise.future
  }

  def setSwitchExtraPost(id: Int, switchId: String) = DomoAction(id) { domoRequest =>
    domoRequest.request.body.asJson match {
      case None =>
        BadRequest("Expecting Json data")
      case Some(json) =>
        domoRequest.service.setSwitchExtraPost(switchId, json)
        Ok("ok")
    }
  }

  def getDomoControlData = Action {
    val services = domoServices.services
    val systemObjects =
      services.map{ case (id, serviceContainer) =>
        val data = Json.obj(
          "name" -> serviceContainer.name,
          "switches" -> serviceContainer.service.getSwitches
        )

        id.toString -> data
      }

    val systems = JsObject(systemObjects)
    val scenes = domoServices.lightScenes.as[JsArray]

    Ok(views.js.domoControlData.render(systems, scenes)).as("text/javascript utf-8")
  }

}
