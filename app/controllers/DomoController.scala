package controllers

import akka.actor.ActorSystem
import javax.inject._

import play.api.libs.json.{JsArray, JsObject, Json}
import play.api.mvc._
import services.common.DomoServices
import services.philips_hue.LightService
import views.js

import scala.concurrent.ExecutionContext

/**
 * This controller creates an `Action` that demonstrates how to write
 * simple asynchronous code in a controller. It uses a timer to
 * asynchronously delay sending a response for 1 second.
 *
 * @param actorSystem We need the `ActorSystem`'s `Scheduler` to
 * run code after a delay.
 * @param exec We need an `ExecutionContext` to execute our
 * asynchronous code.
 */
@Singleton
class DomoController @Inject()(actorSystem: ActorSystem, domoServices: DomoServices)(implicit exec: ExecutionContext) extends Controller {

  def systems = Action {
    Ok(Json.toJson(domoServices.services.values.toList))
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

  def getConf(id: Int) = Action {
    domoServices.services.get(id) match {
      case None => NotFound
      case Some(serviceContainer) => Ok(serviceContainer.service.getConf)
    }
  }

  def setConf(id: Int) = Action { request =>
    domoServices.services.get(id) match {
      case None => NotFound
      case Some(serviceContainer) =>
        val conf = request.body.asJson.get
        serviceContainer.service.setConf(conf)
        Ok("ok")
    }
  }

  def getConnectionStatus(id: Int) = Action {
    domoServices.services.get(id) match {
      case None => NotFound
      case Some(serviceContainer) => Ok(serviceContainer.service.getConnectionStatus)
    }
  }

  def connect(id: Int) = Action {
    domoServices.services.get(id) match {
      case None => NotFound
      case Some(serviceContainer) =>
        serviceContainer.service.connect()
        Ok("ok")
    }
  }

  def disconnect(id: Int) = Action {
    domoServices.services.get(id) match {
      case None => NotFound
      case Some(serviceContainer) =>
        serviceContainer.service.disconnect()
        Ok("ok")
    }
  }

  def getSwitches(id: Int) = Action {
    domoServices.services.get(id) match {
      case None => NotFound
      case Some(serviceContainer) => Ok(serviceContainer.service.getSwitches)
    }
  }

  def setSwitchesStatus(id: Int, status: Int) = Action {
    domoServices.services.get(id) match {
      case None => NotFound
      case Some(serviceContainer) =>
        serviceContainer.service.setSwitchesStatus(status > 0)
        Ok("ok")
    }
  }

  def setSwitchesExtra(id: Int, data: Long) = Action {
    domoServices.services.get(id) match {
      case None => NotFound
      case Some(serviceContainer) =>
        serviceContainer.service.setSwitchesExtra(data)
        Ok("ok")
    }
  }

  def getSwitch(id: Int, switchId: String) = Action {
    domoServices.services.get(id) match {
      case None => NotFound
      case Some(serviceContainer) => Ok(serviceContainer.service.getSwitch(switchId))
    }
  }

  def setSwitchStatus(id: Int, switchId: String, status: Int) = Action {
    domoServices.services.get(id) match {
      case None => NotFound
      case Some(serviceContainer) =>
        serviceContainer.service.setSwitchStatus(switchId, status > 0)
        Ok("ok")
    }
  }

  def setSwitchExtra(id: Int, switchId: String, data: Long) = Action {
    domoServices.services.get(id) match {
      case None => NotFound
      case Some(serviceContainer) =>
        serviceContainer.service.setSwitchExtra(switchId, data)
        Ok("ok")
    }
  }

  def setSwitchesExtraPost(id: Int) = Action { request =>
    domoServices.services.get(id) match {
      case None => NotFound
      case Some(serviceContainer) =>
        request.body.asJson match {
          case None =>
            BadRequest("Expecting Json data")
          case Some(json) =>
            serviceContainer.service.setSwitchesExtraPost(json)
            Ok("ok")
        }
    }
  }

  def setSwitchExtraPost(id: Int, switchId: String) = Action { request =>
    domoServices.services.get(id) match {
      case None => NotFound
      case Some(serviceContainer) =>
        request.body.asJson match {
          case None =>
            BadRequest("Expecting Json data")
          case Some(json) =>
            serviceContainer.service.setSwitchExtraPost(switchId, json)
            Ok("ok")
        }
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

  /**
   * Create an Action that returns a plain text message after a delay
   * of 1 second.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/message`.
   */
/*  def message = Action.async {
    getFutureMessage(1.second).map { msg => Ok(msg) }
  }

  private def getFutureMessage(delayTime: FiniteDuration): Future[String] = {
    val promise: Promise[String] = Promise[String]()
    actorSystem.scheduler.scheduleOnce(delayTime) { promise.success("Hi!") }
    promise.future
  }
*/
}
