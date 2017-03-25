package controllers

import javax.inject._

import akka.actor.ActorSystem
import play.api.mvc._
import request_models.virtual.VirtualDeviceAddRequest
import services.common.{DomoServiceManager, DomoVirtualService}
import services.virtual_switch.SwitchMapping

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DomoVirtualController @Inject()(akkaSystem: ActorSystem, val serviceManager: DomoServiceManager) extends Controller with DomoAction[DomoVirtualService] {

  implicit val customExecutionContext: ExecutionContext = akkaSystem.dispatchers.lookup("custom-context")

  def addSwitch(id: Int): Action[AnyContent] = DomoAction(id).async { domoRequest =>
    domoRequest.request.body.asJson match {
      case None => Future.successful(BadRequest("Expecting Json data"))
      case Some(json) =>
        val switchRequest = json.asOpt[VirtualDeviceAddRequest]
        switchRequest match {
          case None => Future.successful(BadRequest("Missing or invalid request params"))
          case Some(request) => domoRequest.service.asInstanceOf[DomoVirtualService].addDevice(request)
        }
    }
  }

  def removeSwitch(id: Int, switchId: String): Action[AnyContent] = DomoAction(id).async { domoRequest =>
    domoRequest.service.asInstanceOf[DomoVirtualService].removeDevice(switchId)
  }

  def updateSwitch(id: Int, switchId: String): Action[AnyContent] = DomoAction(id).async { domoRequest =>
    domoRequest.request.body.asJson match {
      case None => Future.successful(BadRequest("Expecting Json data"))
      case Some(json) =>
        val switchMappings = json.asOpt[Seq[SwitchMapping]]
        switchMappings match {
          case None => Future.successful(BadRequest("Missing or invalid request params"))
          case Some(mappings) => domoRequest.service.asInstanceOf[DomoVirtualService].updateDevice(switchId, mappings)
        }
    }
  }
}
