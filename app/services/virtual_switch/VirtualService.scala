package services.virtual_switch

import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import models.config.VirtualConf
import play.api.Logger
import play.api.inject.ApplicationLifecycle
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Result, Results}
import request_models.virtual.VirtualDeviceAddRequest
import services.common.{ConfigLoader, DomoServices, DomoVirtualService}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VirtualService @Inject() (
    configLoader: ConfigLoader,
    domoServices: DomoServices,
    appLifecycle: ApplicationLifecycle,
    akkaSystem: ActorSystem) extends DomoVirtualService {

  implicit val ec: ExecutionContext = akkaSystem.dispatchers.lookup("custom-context")

  def getServiceConf: VirtualConf = configLoader.getConfig(VirtualConf(Seq()))

  def setServiceConf(conf: VirtualConf): Unit = configLoader.setConfig(conf)

  // DomoService methods

  override def id: Int = VirtualService.serviceId

  override def name: String = VirtualService.serviceName

  override def connected: Boolean = true

  override def init(): Unit = {}

  override def stop(): Unit = {}

  override def cron(): Unit = {}

  override def connect(): Unit = {}

  override def disconnect(): Unit = {}

  override def getConf: JsValue = Json.toJson(getServiceConf)

  override def setConf(conf: JsValue): Unit = setServiceConf(conf.as[VirtualConf])

  override def getConnectionStatus: JsValue = Json.toJson(connected)

  // DomoSwitchService methods

  override def getSwitches: Future[JsValue] = VirtualControl.getDevices(this, domoServices)

  override def getSwitchRaw(id: String): Future[Boolean] = VirtualControl.getSwitch(this, id).map(_.status)

  override def getSwitch(id: String): Future[Result] = VirtualControl.getSwitch(this, id)
    .map(switch => Results.Ok(Json.toJson(switch)))
    .recover { case exception =>
      Logger.error(f"Error get switch: ${exception.getMessage}")
      exception match {
        case _: VirtualDeviceNotFoundException => Results.NotFound
        case _ => Results.InternalServerError
      }
    }

  override def setSwitchesStatus(status: Boolean): Future[Result] = Future.successful(Results.NotImplemented)

  override def setSwitchStatus(id: String, status: Boolean): Future[Result] = VirtualControl.setSwitchStatus(this, domoServices, id, status)
    .map(_ => Results.Ok)
    .recover { case exception =>
      Logger.error(f"Error updating virtual name: ${exception.getMessage}")
      exception match {
        case _: VirtualDeviceNotFoundException => Results.NotFound
        case _ => Results.InternalServerError
      }
    }

  override def setSwitchAlias(id: String, alias: String): Future[Result] = VirtualControl.setSwitchAlias(this, id, alias)
    .map(_ => Results.Ok)
    .recover { case exception =>
      Logger.error(f"Error updating virtual name: ${exception.getMessage}")
      exception match {
        case _: VirtualDeviceNotFoundException => Results.NotFound
        case _ => Results.InternalServerError
      }
    }

  // DomoVirtualService methods

  override def addDevice(request: VirtualDeviceAddRequest): Future[Result] = VirtualControl.addSwitch(this, request)
    .map(_ => Results.Ok)
    .recover { case exception =>
      Logger.error(f"Error adding virtual switch: ${exception.getMessage}")
      exception match {
        case _: VirtualDeviceNotFoundException => Results.NotFound
        case _ => Results.InternalServerError
      }
    }

  override def removeDevice(switchId: String): Future[Result] = VirtualControl.removeSwitch(this, switchId)
    .map(_ => Results.Ok)
    .recover { case exception =>
      Logger.error(f"Error adding virtual switch: ${exception.getMessage}")
      exception match {
        case _: VirtualDeviceNotFoundException => Results.NotFound
        case _ => Results.InternalServerError
      }
    }

  override def updateDevice(switchId: String, mappings: Seq[SwitchMapping]): Future[Result] = VirtualControl.updateDevice(this, switchId, mappings)
    .map(_ => Results.Ok)
    .recover { case exception =>
      Logger.error(f"Error adding virtual switch: ${exception.getMessage}")
      exception match {
        case _: VirtualDeviceNotFoundException => Results.NotFound
        case _ => Results.InternalServerError
      }
    }

  def getGroups: Future[Seq[VirtualDevice]] = VirtualControl.getDetailedDevices(this)
}

object VirtualService {
  val serviceId = 0
  val serviceName = "Virtual"
}
