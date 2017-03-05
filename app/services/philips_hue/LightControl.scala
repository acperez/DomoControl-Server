package services.philips_hue

import java.awt.Color
import java.util

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.philips.lighting.hue.listener.PHLightListener
import com.philips.lighting.hue.sdk.{PHAccessPoint, PHBridgeSearchManager, PHHueSDK}
import com.philips.lighting.model.{PHBridgeResource, PHHueError, PHLight}
import models.config.PhilipsConf
import play.api.Logger
import services.database_managers.SceneManager
import services.philips_hue.PhilipsActor.SetColor

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

object LightControl {

  private val phHueSDK = PHHueSDK.create()
  phHueSDK.setDeviceName("DomoControl")

  val actor: ActorRef = ActorSystem("PhilipsSystem").actorOf(Props(new PhilipsActor(phHueSDK)), name = "PhilipsActor")

  def isConnected(lightConfig: PhilipsConf): Boolean = {
    (lightConfig.server, lightConfig.user) match {
      case (Some(server), Some(user)) =>
        val accessPoint = new PHAccessPoint()
        accessPoint.setIpAddress(server)
        accessPoint.setUsername(user)

        phHueSDK.isAccessPointConnected(accessPoint)

      case _ => false
    }
  }

  def connect(lightConfig: PhilipsConf, lightListener: LightListener): Unit = {
    if(!isConnected(lightConfig)) {
      (lightConfig.server, lightConfig.user) match {
        case (Some(server), Some(user)) =>
          lightListener.setFinding(false)
          val accessPoint = new PHAccessPoint()
          accessPoint.setIpAddress(server)
          accessPoint.setUsername(user)

          phHueSDK.getNotificationManager.registerSDKListener(lightListener)
          phHueSDK.connect(accessPoint)

        case _ =>
          lightListener.setFinding(true)
          phHueSDK.getNotificationManager.unregisterSDKListener(lightListener)
          phHueSDK.getNotificationManager.registerSDKListener(lightListener)
          val sm = phHueSDK.getSDKService(PHHueSDK.SEARCH_BRIDGE).asInstanceOf[PHBridgeSearchManager]
          sm.search(true, true);
      }
    }
  }

  def disconnect(lightListener: LightListener): Unit = {
    val bridge = phHueSDK.getSelectedBridge

    if (bridge != null) {
      if (phHueSDK.isHeartbeatEnabled(bridge)) {
        phHueSDK.disableHeartbeat(bridge)
      }

      phHueSDK.disconnect(bridge)
      phHueSDK.getNotificationManager.unregisterSDKListener(lightListener)
    }
  }

  private def internalGetLights(): Map[String, PHLight] = {
    val bridge = phHueSDK.getSelectedBridge
    if (bridge == null) throw BridgeNotAvailableException()
    else bridge.getResourceCache.getLights.asScala.toMap
  }

  def getLights(implicit ec: ExecutionContext): Future[Seq[PhilipsSwitch]] = {
    val promise = Promise[Seq[PhilipsSwitch]]
    Future {
      Try(internalGetLights()) match {
        case Failure(_) => Seq()
        case Success(lights) =>
          val switches = lights.values.map { light =>
            val id = light.getIdentifier
            val status = light.getLastKnownLightState.isOn
            val name = light.getName
            val color = LightUtils.getColor(light)
            val hexColor = "#" + Integer.toHexString(color & 0xffffff)

            PhilipsSwitch(LightService.serviceId, id, status, name, available = true, hexColor)
          }
          promise.success(switches.toSeq)
      }
    }

    promise.future
  }

  def getLight(id: String)(implicit ec: ExecutionContext): Future[PhilipsSwitch] = {
    val promise = Promise[PhilipsSwitch]
    Future {
      Try(internalGetLights()) match {
        case Failure(ex) => promise.failure(ex)
        case Success(lights) =>
          lights.get(id) match {
            case None => promise.failure(LightNotFoundException(id))
            case Some(light) =>
              val id = light.getIdentifier
              val status = light.getLastKnownLightState.isOn
              val name = light.getName
              val color = LightUtils.getColor(light)
              val hexColor = "#" + Integer.toHexString(color & 0xffffff)

              promise.success(PhilipsSwitch(LightService.serviceId, id, status, name, available = true, hexColor))
          }
      }
    }

    promise.future
  }

  def setLightStatus(id: String, status: Boolean)(implicit ec: ExecutionContext): Future[Boolean] = {
    val promise = Promise[Boolean]
    Future {
      val bridge = phHueSDK.getSelectedBridge
      if (bridge == null) promise.failure(BridgeNotAvailableException())
      else {
        bridge.updateLightState(id, LightUtils.createSwitchState(status), new PHLightListener {
          override def onReceivingLights(list: util.List[PHBridgeResource]): Unit = {}

          override def onSearchComplete(): Unit = {}

          override def onReceivingLightDetails(phLight: PHLight): Unit = {}

          override def onError(i: Int, s: String): Unit = promise.failure(LightUpdateException(s))

          override def onStateUpdate(map: util.Map[String, String], list: util.List[PHHueError]): Unit = {}

          override def onSuccess(): Unit = promise.success(true)
        })
      }
    }

    promise.future
  }

  def setLightsStatus(status: Boolean)(implicit ec: ExecutionContext): Future[Boolean] = {
    val bridge = phHueSDK.getSelectedBridge

    if (bridge == null) {
      val promise = Promise[Boolean]
      Future {
        throw BridgeNotAvailableException()
      }
      promise.future

    } else {

      val updates = bridge.getResourceCache.getAllLights.asScala.map { light =>
        val promise = Promise[Boolean]
        Future {
          bridge.updateLightState(light, LightUtils.createSwitchState(status), new PHLightListener {
            override def onReceivingLights(list: util.List[PHBridgeResource]): Unit = {}

            override def onSearchComplete(): Unit = {}

            override def onReceivingLightDetails(phLight: PHLight): Unit = {}

            override def onError(i: Int, s: String): Unit = promise.failure(LightUpdateException(s))

            override def onStateUpdate(map: util.Map[String, String], list: util.List[PHHueError]): Unit = {}

            override def onSuccess(): Unit = promise.success(true)
          })
        }
        promise.future
      }

      Future.sequence(updates).map(_ => true)
    }
  }

  def setLightsColor(ids: Seq[String], color: Seq[Int])(implicit ec: ExecutionContext): Future[Boolean] = {
    implicit val timeout = Timeout(500 seconds)
    val result = actor ? SetColor(ids, color)
    result.mapTo[Boolean]
  }

  def loadScene(sceneId: String, sceneManager: SceneManager)(implicit ec: ExecutionContext): Future[Boolean] = {
    val promise = Promise[Boolean]
    Future {
      sceneManager.get(sceneId.toLowerCase()) match {
        case None =>
          Logger.warn(s"No scene with id ${sceneId.toLowerCase()}")
          promise.failure(SceneNotFoundException(sceneId))

        case Some(scene) =>
          val colors = scene.colors.map { colorStr =>
            val colorRGB = Color.decode(colorStr)
            Array(colorRGB.getRed, colorRGB.getGreen, colorRGB.getBlue)
          }

          val lights = internalGetLights().keys.toSeq
          val actions =
            if (colors.size > lights.size) lights.zip(colors)
            else lights.zip(Stream.continually(colors).flatten)

          def sendActions(actions: Seq[(String, Array[Int])]): Unit = {
            if (actions.nonEmpty) {
              val action = actions.head
              val future = setLightsColor(Seq(action._1), action._2)
              future.onComplete(_ => sendActions(actions.tail))
            }
          }

          sendActions(actions)
          promise.success(true)
      }
    }

    promise.future
  }
}
