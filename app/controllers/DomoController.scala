package controllers

import akka.actor.ActorSystem
import javax.inject._

import play.api.Logger
import play.api.libs.json.{JsArray, JsObject, Json}
import play.api.mvc._
import services.common.{DomoServiceManager, DomoSwitchService}
import services.virtual_switch.VirtualService
import services.wemo.WemoService

import scala.concurrent.{ExecutionContext, Future, Promise}

@Singleton
class DomoController @Inject()(akkaSystem: ActorSystem, val serviceManager: DomoServiceManager) extends Controller with DomoAction[DomoSwitchService] {

  implicit val customExecutionContext: ExecutionContext = akkaSystem.dispatchers.lookup("custom-context")

  def systems = Action {
    Ok(Json.toJson(serviceManager.services.values.toList))
  }

  def getConf(id: Int): Action[AnyContent] = DomoAction(id).async { domoRequest =>
    val conf = domoRequest.service.getConf
    Future.successful(Ok(conf))
  }

  def setConf(id: Int): Action[AnyContent] = DomoAction(id).async { domoRequest =>
    val conf = domoRequest.request.body.asJson.get
    domoRequest.service.setConf(conf)
    Future.successful(Ok)
  }

  def getConnectionStatus(id: Int): Action[AnyContent] = DomoAction(id).async { domoRequest =>
    val status = domoRequest.service.getConnectionStatus
    Future.successful(Ok(status))
  }

  def connect(id: Int): Action[AnyContent] = DomoAction(id).async { domoRequest =>
    domoRequest.service.connect()
    Future.successful(Ok)
  }

  def disconnect(id: Int): Action[AnyContent] = DomoAction(id).async { domoRequest =>
    domoRequest.service.disconnect()
    Future.successful(Ok)
  }

  def getDomoControlData: Action[AnyContent] = Action.async {
    val promise = Promise[Result]
    Future {

      val services = serviceManager.services

      val systemFutures = services.map { case (id, service) =>
          service.getSwitches.map { switches =>
            id.toString -> Json.obj(
              "name" -> service.name,
              "switches" -> switches
            )
          }
        }

      val monitorFutures = services.values.flatMap {
        case service: WemoService => Some(service.getCurrentUsageForAll)
        case _ => None
      }

      val virtualFutures = services.values.flatMap {
        case service: VirtualService => Some(service.getGroups)
        case _ => None
      }

      val monitorsRequest = Future.sequence(monitorFutures)
      val groupsRequest = Future.sequence(virtualFutures).map { data =>
        data.flatten.toSeq.map (group => group.id -> Json.toJson(group))
      }
      val systemRequest = Future.sequence(systemFutures).map { data =>
        JsObject(data.toMap)
      }

      val futures = for{
        monitors <- monitorsRequest
        systems <- systemRequest
        groups <- groupsRequest
      } yield (monitors, systems, groups)

      futures.map { case (monitors, systems, groups) =>
        val scenes = serviceManager.lightScenes.as[JsArray]
        val data = Json.toJson(monitors.flatten).as[JsArray]
        val groupData = JsObject(groups)
        val result = Ok(views.js.domoControlData.render(systems, scenes, data, groupData)).as("text/javascript utf-8")
        promise.success(result)
      }.recover { case exception =>
        Logger.error(f"Error getting systems js: ${exception.getMessage}")
        promise.success(NotFound)
      }
    }

    promise.future
  }
}
