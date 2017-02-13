package services.wemo

import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import models.config.WemoConf
import play.api.Logger
import play.api.inject.ApplicationLifecycle
import play.api.libs.json._
import play.api.mvc.{Result, Results}
import services.common.{ConfigLoader, DomoSwitchService}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WemoService @Inject() (
  configLoader: ConfigLoader,
  appLifecycle: ApplicationLifecycle,
  akkaSystem: ActorSystem) extends DomoSwitchService {

  implicit val ec: ExecutionContext = akkaSystem.dispatchers.lookup("custom-context")

  def getServiceConf: WemoConf = configLoader.getConfig(WemoConf(Seq()))

  def setServiceConf(wemoConf: WemoConf): Unit = configLoader.setConfig(wemoConf)

  // DomoService methods

  override def id: Int = WemoService.serviceId

  override def name: String = WemoService.serviceName

  override def connected: Boolean = WemoControl.isConnected(getServiceConf)

  override def init(): Unit = {
    Logger.info(f"Init $name service")
    val conf = getConf
    Logger.info(f"$name config: " + conf)
    connect()
  }

  def stop(): Unit = {
    Logger.info(f"Stop $name service")
  }

  override def connect(): Unit = WemoControl.connect(this)

  override def disconnect(): Unit = {}

  override def getConf: JsValue = Json.toJson(getServiceConf)

  override def setConf(conf: JsValue): Unit = setServiceConf(conf.as[WemoConf])

  override def getConnectionStatus: JsValue = Json.toJson(connected)

  // DomoSwitchService methods

  override def getSwitches: Future[JsValue] = WemoControl.getDevices(getServiceConf)

  override def getSwitch(id: String): Future[Result] = WemoControl.getSwitchStatus(getServiceConf, id)
    .map(switch => Results.Ok(Json.toJson(switch)))
    .recover { case exception =>
      Logger.error(f"Error get switch: ${exception.getMessage}")
      exception match {
        case _: WemoDeviceNotFoundException => Results.NotFound
        case _ => Results.InternalServerError
      }
    }

  override def setSwitchesStatus(status: Boolean): Future[Result] = WemoControl.setSwitchesStatus(getServiceConf, status)
    .map(_ => Results.Ok)
    .recover { case exception =>
      Logger.error(f"Error updating wemo: ${exception.getMessage}")
      Results.InternalServerError
    }

  override def setSwitchStatus(id: String, status: Boolean): Future[Result] = WemoControl.setSwitchStatus(getServiceConf, id, status)
    .map(_ => Results.Ok)
    .recover { case exception =>
      Logger.error(f"Error updating wemo: ${exception.getMessage}")
      exception match {
        case _: WemoDeviceNotFoundException => Results.NotFound
        case _ => Results.InternalServerError
      }
    }
}

object WemoService {
  val serviceId = 2
  val serviceName = "Wemo"
}