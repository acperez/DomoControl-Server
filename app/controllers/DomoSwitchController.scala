package controllers

import javax.inject._

import akka.actor.ActorSystem
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc._
import services.common.{DomoServiceManager, DomoSwitchService}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DomoSwitchController @Inject()(akkaSystem: ActorSystem, val serviceManager: DomoServiceManager) extends Controller with DomoAction[DomoSwitchService] {
  implicit val customExecutionContext: ExecutionContext = akkaSystem.dispatchers.lookup("custom-context")

  def getAllSwitches: Action[AnyContent] = Action.async {
    val switchesFuture = serviceManager.services.flatMap { case (_, service: DomoSwitchService) =>
      Some(service.getSwitchesWithId)
    }

    Future.sequence(switchesFuture.toSeq)
      .map(switches => Ok(Json.toJson(switches)))
      .recover { case error =>
        Logger.error(f"Error in getSwitches: ${error.getMessage}")
        InternalServerError
      }
  }

  def getSwitches(id: Int): Action[AnyContent] = DomoAction(id).async { domoRequest =>
    domoRequest.service.asInstanceOf[DomoSwitchService].getSwitches.map(switches => Ok(switches))
  }

  def getSwitch(id: Int, switchId: String): Action[AnyContent] = DomoAction(id).async { domoRequest =>
    domoRequest.service.asInstanceOf[DomoSwitchService].getSwitch(switchId)
  }

  def setSwitchesStatus(id: Int, status: Int): Action[AnyContent] = DomoAction(id).async { domoRequest =>
    domoRequest.service.asInstanceOf[DomoSwitchService].setSwitchesStatus(status > 0)
  }

  def setSwitchStatus(id: Int, switchId: String, status: Int): Action[AnyContent] = DomoAction(id).async { domoRequest =>
    domoRequest.service.asInstanceOf[DomoSwitchService].setSwitchStatus(switchId, status > 0)
  }

  def setSwitchAlias(id: Int, switchId: String, alias: String): Action[AnyContent] = DomoAction(id).async { domoRequest =>
    domoRequest.service.asInstanceOf[DomoSwitchService].setSwitchAlias(switchId, alias)
  }
}
