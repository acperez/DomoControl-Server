package controllers

import javax.inject.Inject

import akka.actor.ActorSystem
import play.api.mvc._
import services.common.{DomoServices, DomoWemoService}

import scala.concurrent.{ExecutionContext, Future}

class DomoWemoController @Inject()(akkaSystem: ActorSystem, domoServices: DomoServices) extends Controller {

  implicit  val customExecutionContext: ExecutionContext = akkaSystem.dispatchers.lookup("custom-context")

  case class DomoRequest[A](service: DomoWemoService, request: Request[A]) extends WrappedRequest[A](request)

  def DomoAction(itemId: Int) = new ActionBuilder[DomoRequest] with ActionRefiner[Request, DomoRequest] {
    def refine[A](request: Request[A]): Future[Either[Result, DomoRequest[A]]] = Future.successful {
      domoServices.services.get(itemId) match {
        case None => Left(NotFound)
        case Some(service: DomoWemoService) =>
          Right(DomoRequest(service, request))
        case Some(_) => Left(NotFound)
      }
    }
  }

  def usageForAll(id: Int): Action[AnyContent] = DomoAction(id).async { domoRequest =>
    domoRequest.service.getUsageForAll
  }

  def usage(id: Int, plugId: String): Action[AnyContent] = DomoAction(id).async { domoRequest =>
    domoRequest.service.getUsage(plugId)
  }

  def history(id: Int, plugId: String, month: Int): Action[AnyContent] = DomoAction(id).async { domoRequest =>
    domoRequest.service.getHistory(plugId, month)
  }

  def clearHistory(id: Int, plugId: String): Action[AnyContent] = DomoAction(id).async { domoRequest =>
    domoRequest.service.clearHistory(plugId)
  }
}
