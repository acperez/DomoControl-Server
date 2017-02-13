package controllers

import javax.inject.Inject

import akka.actor.ActorSystem
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc._
import services.common.{DomoServices, DomoSwitchService}

import scala.concurrent.{ExecutionContext, Future}

class DomoSwitchController @Inject()(akkaSystem: ActorSystem, domoServices: DomoServices) extends Controller {

  implicit  val customExecutionContext: ExecutionContext = akkaSystem.dispatchers.lookup("custom-context")

  case class DomoRequest[A](service: DomoSwitchService, request: Request[A]) extends WrappedRequest[A](request)

  def DomoAction(itemId: Int) = new ActionBuilder[DomoRequest] with ActionRefiner[Request, DomoRequest] {
    def refine[A](request: Request[A]): Future[Either[Result, DomoRequest[A]]] = Future.successful {
      domoServices.services.get(itemId) match {
        case None => Left(NotFound)
        case Some(service: DomoSwitchService) =>
          Right(DomoRequest(service, request))
        case Some(_) => Left(NotFound)
      }
    }
  }

  def getAllSwitches: Action[AnyContent] = Action.async {
    val switchesFuture = domoServices.services.flatMap { case (_, service: DomoSwitchService) =>
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
    domoRequest.service.getSwitches.map(switches => Ok(switches))
  }

  def getSwitch(id: Int, switchId: String): Action[AnyContent] = DomoAction(id).async { domoRequest =>
    domoRequest.service.getSwitch(switchId)
  }

  def setSwitchesStatus(id: Int, status: Int): Action[AnyContent] = DomoAction(id).async { domoRequest =>
    domoRequest.service.setSwitchesStatus(status > 0)
  }

  def setSwitchStatus(id: Int, switchId: String, status: Int): Action[AnyContent] = DomoAction(id).async { domoRequest =>
    domoRequest.service.setSwitchStatus(switchId, status > 0)
  }
}
