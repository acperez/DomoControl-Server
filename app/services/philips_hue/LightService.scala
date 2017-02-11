package services.philips_hue

import java.awt.Color
import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import models.config.{PhilipsConf, PhilipsScene}
import org.apache.commons.codec.binary.Base64
import play.api.http.Status
import services.common.{ConfigLoader, DomoService, DomoSwitch, SceneManager}
import play.api.{Environment, Logger}
import play.api.inject.ApplicationLifecycle
import play.api.libs.json._
import reactivemongo.core.errors.DatabaseException
import sun.misc.BASE64Decoder

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

@Singleton
class LightService @Inject() (
  configLoader: ConfigLoader,
  sceneManager: SceneManager,
  env: Environment,
  appLifecycle: ApplicationLifecycle,
  akkaSystem: ActorSystem) extends DomoService {

  implicit val ec: ExecutionContext = akkaSystem.dispatchers.lookup("custom-context")

  lazy val decoder: BASE64Decoder = new BASE64Decoder()

  def decode(value: String): String = {
    val decoded = Base64.decodeBase64(value)
    new String(decoded, "UTF-8")
  }

  val lightListener = LightListener(this)

  override def serviceId: Int = LightService.serviceId

  override def serviceName: String = LightService.serviceName

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

  def onConnect(): Unit = {
    val lights = getSwitches
    Logger.info(f"Light list: $lights")
  }

  def getServiceConf: PhilipsConf = configLoader.getConfig(PhilipsConf(None, None))

  override def getConf: JsValue = Json.toJson(getServiceConf)

  def setServiceConf(philipsConf: PhilipsConf): Unit = configLoader.setConfig(philipsConf)

  override def setConf(conf: JsValue): Unit = setServiceConf(conf.as[PhilipsConf])

  override def connected: Boolean = LightControl.isConnected(getServiceConf)

  override def getConnectionStatus: JsValue = Json.toJson(connected)

  override def connect(): Unit = LightControl.connect(getServiceConf, lightListener)

  override def disconnect(): Unit = LightControl.disconnect(lightListener)

  def getLights: Seq[DomoSwitch] = LightControl.getLights

  def getLightStatus(id: String): Boolean = LightControl.getLightStatus(id)

  override def getSwitches: JsValue = Json.toJson(LightControl.getLightsWithColor)

  override def getSwitch(id: String): JsValue = Json.toJson(getLightStatus(id))

  override def setSwitchesStatus(status: Boolean): Unit = getLights.foreach( light => LightControl.setLightStatus(light.id, status))

  override def setSwitchesExtra(switches: String, data: String): Future[Boolean] = {
    val ids = decode(switches).split(',')

    decode(data).split(',') match {
      case Array(r, g, b) =>
        LightControl.setLightColor(ids, Seq(r.toInt, g.toInt, b.toInt))
    }
  }

  override def setSwitchStatus(id: String, status: Boolean): Unit = LightControl.setLightStatus(id, status)

  override def setSwitchExtra(id: String, data: String): Future[Boolean] = ???

  def setLightColor(id: String, color: Long): Unit = {
    val r = (color >> 16).toInt
    val g = ((color >> 8) & 255).toInt
    val b = (color & 255).toInt
    val rgb = Color.RGBtoHSB(r, g, b, null)
    //LightControl.setLightColor(id, rgb)
  }

  override def setSwitchesExtraPost(data: JsValue): Future[Int] = {
    val action = (data \ "action").as[Int]
    action match {
      case 0 => loadScene(data)
      case 1 => saveScene(data)
      case 2 => removeScene(data)
      case _ => Future { Status.NOT_FOUND }
    }
  }

  def loadScene(data: JsValue): Future[Int] = {
    (data \ "sceneId").asOpt[String] match{
      case None => Future { Status.BAD_REQUEST }
      case Some(sceneId) =>

        def sendActions(actions: Seq[(DomoSwitch, Array[Int])]): Unit = {
          if (actions.nonEmpty) {
            val action = actions.head
            val future = LightControl.setLightColor(Seq(action._1.id), action._2)
            future.onComplete(_ => sendActions(actions.tail))
          }
        }

        sceneManager.get(sceneId.toLowerCase()) match {
          case None =>
            Logger.warn(s"No scene with id ${sceneId.toLowerCase()}")
            Future {
              Status.NOT_FOUND
            }

          case Some(scene) =>
            val colors = scene.colors.map { colorStr =>
              val colorRGB = Color.decode(colorStr)
              Array(colorRGB.getRed, colorRGB.getGreen, colorRGB.getBlue)
            }

            val lights = getLights
            val actions =
              if (colors.size > lights.size) lights.zip(colors)
              else lights.zip(Stream.continually(colors).flatten)

            sendActions(actions)
            Future {
              Status.OK
            }
        }
    }
  }

  def saveScene(data: JsValue): Future[Int] = {

    val sceneValue = (data \ "scene").as[JsObject] ++ Json.obj("default" -> false)
    sceneValue.asOpt[PhilipsScene] match {
      case None => Future { Status.BAD_REQUEST }
      case Some(scene) =>

        val promise = Promise[Int]
        Future {
          val result = sceneManager.save(scene)
          result.onComplete {
            case Success(_) =>
              sceneManager.addToCache(scene)
              promise.success(Status.OK)
            case Failure(e: DatabaseException) if e.code.getOrElse(0) == 11000 =>
              promise.success(Status.CONFLICT)
            case Failure(e) =>
              Logger.error(f"Faled to save a scene: ${e.getMessage}")
              promise.success(Status.INTERNAL_SERVER_ERROR)
          }
        }

        promise.future
    }
  }

  def removeScene(data: JsValue): Future[Int] = {
    (data \ "sceneId").asOpt[String] match{
      case None => Future { Status.BAD_REQUEST }
      case Some(sceneId) =>

        sceneManager.get(sceneId.toLowerCase()) match {
          case None =>
            Logger.warn(s"No scene with id ${sceneId.toLowerCase()}")
            Future { Status.NOT_FOUND }

          case Some(scene) if scene.default => Future { Status.FORBIDDEN }

          case Some(_) =>
            val promise = Promise[Int]

            sceneManager.remove(sceneId.toLowerCase()).onComplete {
              case Success(result) if result.isEmpty => promise.success(Status.NOT_FOUND)
              case Success(result) =>
                sceneManager.removeFromCache(result.get)
                promise.success(Status.OK)
              case Failure(e) =>
                Logger.error(f"Failed to remove scene: ${e.getMessage}")
                promise.success(Status.INTERNAL_SERVER_ERROR)
            }

            promise.future
        }
    }
  }

  def getScenes: JsValue = {
    Json.toJson(sceneManager.getAll)
  }

  override def setSwitchExtraPost(id: String, data: JsValue): Unit = ???
}

object LightService {
  val serviceId = 1
  val serviceName = "Philips Hue"
}
