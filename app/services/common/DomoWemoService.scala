package services.common

import play.api.mvc.Result

import scala.concurrent.Future

trait DomoWemoService extends DomoSwitchService {

  def getUsageForAll: Future[Result]

  def getUsage(id: String): Future[Result]

  def getHistory(id: String, month: Int): Future[Result]

  def clearHistory(id: String): Future[Result]
}
