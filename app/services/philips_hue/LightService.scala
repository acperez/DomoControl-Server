package services.philips_hue

import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import models.config.{PhilipsConf, PhilipsScene}
import org.apache.commons.codec.binary.Base64
import services.common._
import play.api.{Environment, Logger}
import play.api.inject.ApplicationLifecycle
import play.api.libs.json._
import play.api.mvc.{Result, Results}
import reactivemongo.core.errors.DatabaseException
import services.database_managers.SceneManager
import sun.misc.BASE64Decoder

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LightService @Inject() (
  configLoader: ConfigLoader,
  sceneManager: SceneManager,
  env: Environment,
  appLifecycle: ApplicationLifecycle,
  akkaSystem: ActorSystem) extends DomoPhilipsService {

  implicit val ec: ExecutionContext = akkaSystem.dispatchers.lookup("custom-context")

  lazy val decoder: BASE64Decoder = new BASE64Decoder()

  def decode(value: String): String = {
    val decoded = Base64.decodeBase64(value)
    new String(decoded, "UTF-8")
  }

  val lightListener = LightListener(this)

  def onConnect(): Unit = {
    Logger.info("Philips Hue service ready")
  }

  def getServiceConf: PhilipsConf = configLoader.getConfig(PhilipsConf(None, None))

  def setServiceConf(philipsConf: PhilipsConf): Unit = configLoader.setConfig(philipsConf)

  // DomoService methods

  override def id: Int = LightService.serviceId

  override def name: String = LightService.serviceName

  override def connected: Boolean = LightControl.isConnected(getServiceConf)

  override def init(): Unit = {
    Logger.info("Init Philips Hue service")
    sceneManager.bootstrap()
    val conf = getConf
    Logger.info("Lights config: " + conf)
    connect()
  }

  override def stop(): Unit = {
    Logger.info("Stop Philips Hue service")
    disconnect()
  }

  override def cron(): Unit = {}

  override def connect(): Unit = LightControl.connect(getServiceConf, lightListener)

  override def disconnect(): Unit = LightControl.disconnect(lightListener)

  override def getConf: JsValue = Json.toJson(getServiceConf)

  override def setConf(conf: JsValue): Unit = setServiceConf(conf.as[PhilipsConf])

  override def getConnectionStatus: JsValue = Json.toJson(connected)

  // DomoSwitchService methods

  override def getSwitches: Future[JsValue] = LightControl.getLights.map(lights => Json.toJson(lights))

  override def getSwitch(id: String): Future[Result] = LightControl.getLight(id)
    .map(switch => Results.Ok(Json.toJson(switch.status)))
    .recover {
      case exception =>
        Logger.error(f"Error get switch: ${exception.getMessage}")
        exception match {
          case _: BridgeNotAvailableException => Results.ServiceUnavailable
          case _: LightNotFoundException => Results.NotFound
          case _ => Results.InternalServerError
        }
    }

  override def setSwitchesStatus(status: Boolean): Future[Result] = LightControl.setLightsStatus(status)
    .map(_ => Results.Ok)
    .recover {
      case exception =>
        Logger.error(f"Error updating lights: ${exception.getMessage}")
        exception match {
          case _: BridgeNotAvailableException => Results.ServiceUnavailable
          case _: LightUpdateException => Results.InternalServerError
          case _ => Results.InternalServerError
        }
    }

  override def setSwitchStatus(id: String, status: Boolean): Future[Result] = LightControl.setLightStatus(id, status)
    .map(_ => Results.Ok)
    .recover {
      case exception =>
        Logger.error(f"Error updating light: ${exception.getMessage}")
        exception match {
          case _: BridgeNotAvailableException => Results.ServiceUnavailable
          case _: LightUpdateException => Results.InternalServerError
          case _ => Results.InternalServerError
        }
    }

  override def setSwitchAlias(id: String, alias: String): Future[Result] = LightControl.setLightName(id, alias)
    .map(_ => Results.Ok)
    .recover {
      case exception =>
        Logger.error(f"Error setting light name: ${exception.getMessage}")
        exception match {
          case _: BridgeNotAvailableException => Results.ServiceUnavailable
          case _: LightNotFoundException => Results.NotFound
          case _ => Results.InternalServerError
        }
    }

  // DomoPhilipsService methods

  override def setLightsColor(lights: Seq[String], color: Seq[Int]): Future[Result] = LightControl.setLightsColor(lights, color)
    .map(_ => Results.Ok)

  override def loadScene(sceneId: String): Future[Result] = LightControl.loadScene(sceneId, sceneManager)
    .map(_ => Results.Ok)
    .recover {
      case exception =>
        Logger.error(f"Error loading light scene: ${exception.getMessage}")
        exception match {
          case _: SceneNotFoundException => Results.NotFound
          case _ => Results.InternalServerError
        }
    }

  override def saveScene(scene: PhilipsScene): Future[Result] = sceneManager.save(scene)
    .map(_ => Results.Ok)
    .recover {
      case ex: DatabaseException if ex.code.getOrElse(0) == 11000 => Results.Conflict
      case ex =>
        Logger.error(f"Faled to save a scene: ${ex.getMessage}")
        Results.InternalServerError
    }

  override def removeScene(sceneId: String): Future[Result] = sceneManager.remove(sceneId)
    .map(_ => Results.Ok)
    .recover { case exceptions =>
      Results.InternalServerError
    }

  def getScenes: JsValue = {
    Json.toJson(sceneManager.getAll)
  }
}

object LightService {
  val serviceId = 1
  val serviceName = "Philips Hue"
}
