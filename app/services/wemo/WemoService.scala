package services.wemo

import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import models.config.WemoConf
import play.api.Logger
import play.api.inject.ApplicationLifecycle
import play.api.libs.json._
import play.api.mvc.{Result, Results}
import services.common.{ConfigLoader, DomoWemoService}
import services.database_managers.HistoryManager

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WemoService @Inject() (
  configLoader: ConfigLoader,
  historyManager: HistoryManager,
  appLifecycle: ApplicationLifecycle,
  akkaSystem: ActorSystem) extends DomoWemoService {

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

  override def stop(): Unit = {
    Logger.info(f"Stop $name service")
  }

  override def cron(): Unit = {
    // Update usage history
    val result = WemoControl.getWemoDevicesUsage(getServiceConf)
      .flatMap { devicesUsage =>
        val futures = devicesUsage.map { usage =>
          historyManager.save(usage)
        }
        Future.sequence(futures)
      }

    result.map(
      res => Logger.info("--> " + res)
    ).recover { case error =>
      Logger.error(s"failed to save wemom usage history: ${error.getMessage}")
    }

    // Remove old history
    val time = System.currentTimeMillis()
    val timestamp = (time - time % (1000L * 60 * 60 * 24)) - 2 * (1000L * 60 * 60 * 24 * 365)
    historyManager.removeOld(timestamp)
  }

  override def connect(): Unit = WemoControl.connect(this)

  override def disconnect(): Unit = {}

  override def getConf: JsValue = Json.toJson(getServiceConf)

  override def setConf(conf: JsValue): Unit = setServiceConf(conf.as[WemoConf])

  override def getConnectionStatus: JsValue = Json.toJson(connected)

  // DomoSwitchService methods

  override def getSwitches: Future[JsValue] = WemoControl.getDevices(this)

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

  // DomoWemoService methods

  override def getUsage(id: String): Future[Result] = getCurrentUsage(id)
    .map(data => Results.Ok(Json.toJson(data)))
    .recover { case exception =>
      Logger.error(f"Error geting wemo monitor data: ${exception.getMessage}")
        Results.InternalServerError
    }

  override def getUsageForAll: Future[Result] = getCurrentUsageForAll
    .map(data => Results.Ok(Json.toJson(data)))
    .recover { case exception =>
      Logger.error(f"Error geting wemo monitor data: ${exception.getMessage}")
      Results.InternalServerError
    }

  def getCurrentUsage(id: String): Future[Option[WemoMonitorData]] = WemoControl.getWemoUsage(id, getServiceConf)

  def getCurrentUsageForAll: Future[Seq[WemoMonitorData]] = WemoControl.getWemoDevicesUsage(getServiceConf)

  override def getHistory(id: String, month: Int): Future[Result] = historyManager.getMonthHistory(id, month)
    .map(result => Results.Ok(result))
    .recover {
      case exception =>
        Logger.error(f"Error deleting history: ${exception.getMessage}")
        Results.InternalServerError
    }

  /*
  override def getHistory(id: String, month: Int): Future[Result] = {
    generateFakeUsageData()
    Future.successful(Results.Ok)
  }
*/

  override def clearHistory(id: String): Future[Result] = historyManager.remove(id)
    .map(_ => Results.Ok)
    .recover {
      case exception =>
        Logger.error(f"Error deleting history: ${exception.getMessage}")
        Results.InternalServerError
    }

  def generateFakeUsageData(): Unit =
    historyManager.generateFakeUsageData(getServiceConf.devices.filter(_.deviceType == WemoDeviceType.Monitor).map(_.name))
}

object WemoService {
  val serviceId = 2
  val serviceName = "Wemo"
}