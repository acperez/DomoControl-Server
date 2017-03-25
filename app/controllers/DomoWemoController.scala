package controllers

import javax.inject._

import akka.actor.ActorSystem
import play.api.mvc._
import services.common.{DomoServiceManager, DomoWemoService}

import scala.concurrent.ExecutionContext

@Singleton
class DomoWemoController @Inject()(akkaSystem: ActorSystem, val serviceManager: DomoServiceManager) extends Controller with DomoAction[DomoWemoService] {

  implicit  val customExecutionContext: ExecutionContext = akkaSystem.dispatchers.lookup("custom-context")

  def usageForAll(id: Int): Action[AnyContent] = DomoAction(id).async { domoRequest =>
    domoRequest.service.asInstanceOf[DomoWemoService].getUsageForAll
  }

  def usage(id: Int, plugId: String): Action[AnyContent] = DomoAction(id).async { domoRequest =>
    domoRequest.service.asInstanceOf[DomoWemoService].getUsage(plugId)
  }

  def history(id: Int, plugId: String, month: Int): Action[AnyContent] = DomoAction(id).async { domoRequest =>
    domoRequest.service.asInstanceOf[DomoWemoService].getHistory(plugId, month)
  }

  def clearHistory(id: Int, plugId: String): Action[AnyContent] = DomoAction(id).async { domoRequest =>
    domoRequest.service.asInstanceOf[DomoWemoService].clearHistory(plugId)
  }
}
