package services.philips_hue

import java.util

import com.philips.lighting.hue.listener.PHLightListener
import com.philips.lighting.hue.sdk.{PHAccessPoint, PHBridgeSearchManager, PHHueSDK}
import com.philips.lighting.model.{PHBridgeResource, PHHueError, PHLight}
import models.config.PhilipsConf
import services.common.DomoSwitch

import scala.collection.JavaConverters._

object LightControl {

  private val phHueSDK = PHHueSDK.create()
  phHueSDK.setDeviceName("DomoControl")

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
        val id = light.getIdentifier.toInt
        val status = light.getLastKnownLightState.isOn
        val name = light.getName

        DomoSwitch(LightService.serviceId, id, status, name)
      }
    }
  }

  def getLightStatus(id: Int): Boolean = {
    val bridge = phHueSDK.getSelectedBridge
    if (bridge == null) throw new Exception("Philips Hue bridge not available")
    else {
      val lights = bridge.getResourceCache.getLights.asScala
      lights.getOrElse(id.toString, throw new Exception("Invalid light id"))
        .getLastKnownLightState
        .isOn
    }
  }

  def setLightStatus(id: Int, status: Boolean) = {
    val bridge = phHueSDK.getSelectedBridge
    if (bridge == null) throw new Exception("Philips Hue bridge not available")
    else {
      bridge.updateLightState(id.toString, LightUtils.createSwitchState(status), new PHLightListener {
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

  def setLightColor(id: Int, color: Seq[Float]) = {
    val bridge = phHueSDK.getSelectedBridge
    if (bridge == null) throw new Exception("Philips Hue bridge not available")
    else {
      bridge.updateLightState(id.toString, LightUtils.createColorState(color), new PHLightListener {
        override def onReceivingLights(list: util.List[PHBridgeResource]): Unit = {}

        override def onSearchComplete(): Unit = {}

        override def onReceivingLightDetails(phLight: PHLight): Unit = {}

        override def onError(i: Int, s: String): Unit = {}

        override def onStateUpdate(map: util.Map[String, String], list: util.List[PHHueError]): Unit = {}

        override def onSuccess(): Unit = {}
      })
    }
  }
}
