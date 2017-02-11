package services.philips_hue

import java.util

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.philips.lighting.hue.listener.PHLightListener
import com.philips.lighting.hue.sdk.{PHAccessPoint, PHBridgeSearchManager, PHHueSDK}
import com.philips.lighting.model.{PHBridgeResource, PHHueError, PHLight}
import models.config.PhilipsConf
import services.common.DomoSwitch
import services.philips_hue.PhilipsActor.SetColor

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

object LightControl {

  private val phHueSDK = PHHueSDK.create()
  phHueSDK.setDeviceName("DomoControl")

  val actor = ActorSystem("PhilipsSystem").actorOf(Props(new PhilipsActor(phHueSDK)), name = "PhilipsActor")

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

  def getLights: Seq[DomoSwitch] = {
    val bridge = phHueSDK.getSelectedBridge
    if (bridge == null) Seq()
    else {
      val lights = bridge.getResourceCache.getAllLights.asScala
      lights.map { light =>
        val id = light.getIdentifier
        val status = light.getLastKnownLightState.isOn
        val name = light.getName

        DomoSwitch(LightService.serviceId, id, status, name, available = true)
      }
    }
  }

  def getLightsWithColor: Seq[PhilipsSwitch] = {
    val bridge = phHueSDK.getSelectedBridge
    if (bridge == null) Seq()
    else {
      val lights = bridge.getResourceCache.getAllLights.asScala
      lights.map { light =>
        val id = light.getIdentifier
        val status = light.getLastKnownLightState.isOn
        val name = light.getName
        val color = LightUtils.getColor(light)
        val hexColor = "#" + Integer.toHexString(color & 0xffffff)

        PhilipsSwitch(LightService.serviceId, id, status, name, available = true, hexColor)
      }
    }
  }

  def getLightStatus(id: String): Boolean = {
    val bridge = phHueSDK.getSelectedBridge
    if (bridge == null) throw new Exception("Philips Hue bridge not available")
    else {
      val lights = bridge.getResourceCache.getLights.asScala
      lights.getOrElse(id, throw new Exception("Invalid light id"))
        .getLastKnownLightState
        .isOn
    }
  }

  def setLightStatus(id: String, status: Boolean) = {
    val bridge = phHueSDK.getSelectedBridge
    if (bridge == null) throw new Exception("Philips Hue bridge not available")
    else {
      bridge.updateLightState(id, LightUtils.createSwitchState(status), new PHLightListener {
        override def onReceivingLights(list: util.List[PHBridgeResource]): Unit = {}

        override def onSearchComplete(): Unit = {}

        override def onReceivingLightDetails(phLight: PHLight): Unit = {}

        override def onError(i: Int, s: String): Unit = {}

        override def onStateUpdate(map: util.Map[String, String], list: util.List[PHHueError]): Unit = {}

        override def onSuccess(): Unit = {}
      })


      val lights = bridge.getResourceCache.getLights.asScala
      lights.getOrElse(id.toString, throw new Exception("Invalid light id"))
        .getLastKnownLightState
        .isOn
    }
  }

  def setLightColor(ids: Seq[String], color: Seq[Int])(implicit ec: ExecutionContext): Future[Boolean] = {
    implicit val timeout = Timeout(500 seconds)
    val result = actor ? SetColor(ids, color)
    result.mapTo[Boolean]
  }
}
