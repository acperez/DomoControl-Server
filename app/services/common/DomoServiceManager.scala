package services.common

import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import play.api.libs.json.JsValue
import play.api.mvc._
import services.virtual_switch.VirtualService

import scala.concurrent.Future
import scala.reflect.ClassTag

case class DomoRequest[A](service: DomoService, request: Request[A]) extends WrappedRequest[A](request)

@Singleton
class DomoServiceManager @Inject()(akkaSystem: ActorSystem, domoServices: DomoServices, virtualService: VirtualService) {

  def serviceRoute[T <: DomoService](itemId: Int)(implicit c: ClassTag[T]) = new ActionBuilder[DomoRequest] with ActionRefiner[Request, DomoRequest] {
    def refine[A](request: Request[A]): Future[Either[Result, DomoRequest[A]]] = Future.successful {
      domoServices.services.get(itemId) match {
        case Some(service: T) =>
          Right(DomoRequest(service, request))
        case None if itemId == virtualService.id =>
          Right(DomoRequest(virtualService, request))
        case _ =>
          Left(Results.NotFound)
      }
    }
  }

  def services: Map[Int, DomoSwitchService] = domoServices.services + (VirtualService.serviceId -> virtualService)

  def lightScenes: JsValue = domoServices.lightScenes
}
