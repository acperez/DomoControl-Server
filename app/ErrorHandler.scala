import play.api.http.HttpErrorHandler
import play.api.mvc._
import play.api.mvc.Results._

import scala.concurrent._
import javax.inject.Singleton

import play.api.Logger;

@Singleton
class ErrorHandler extends HttpErrorHandler {

  def onClientError(request: RequestHeader, statusCode: Int, message: String) = {
    Logger.error(f"HTTP error $statusCode for request $request")
    Future.successful(
      Status(statusCode)
    )
  }

  def onServerError(request: RequestHeader, exception: Throwable) = {
    Logger.error(f"Internal server error for for request $request - ${exception.getMessage}")
    Future.successful(
      InternalServerError
    )
  }
}