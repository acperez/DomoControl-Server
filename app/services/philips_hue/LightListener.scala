package services.philips_hue

import java.util
import scala.collection.JavaConverters._

import com.philips.lighting.hue.sdk.{PHAccessPoint, PHHueSDK, PHSDKListener}
import com.philips.lighting.model.{PHBridge, PHHueParsingError}
import models.config.PhilipsConf
import play.api.Logger

class LightListener(lightService: LightService) extends PHSDKListener {

  private var finding: Boolean = false
  private var bridge: PHBridge = _

  private val phHueSDK = PHHueSDK.create()
  phHueSDK.setDeviceName("DomoControl")

  def setFinding(value: Boolean) = {
    finding = value
  }

  override def onAccessPointsFound(accessPoints: java.util.List[PHAccessPoint]) {
    Logger.info("Light access point found")
    if (accessPoints != null && accessPoints.size() > 0) {
      phHueSDK.getAccessPointsFound.clear()
      phHueSDK.getAccessPointsFound.addAll(accessPoints)

      val accessPoint = accessPoints.get(0)
      accessPoint.setUsername(PhilipsConf.generateUsername())

      val connectedBridge = phHueSDK.getSelectedBridge

      if (connectedBridge != null) {
        val connectedIP = connectedBridge.getResourceCache.getBridgeConfiguration.getIpAddress
        if (connectedIP != null) {
          phHueSDK.disableHeartbeat(connectedBridge)
          phHueSDK.disconnect(connectedBridge)
        }
      }

      phHueSDK.connect(accessPoint)
    }
  }

  override def onBridgeConnected(phBridge: PHBridge, s: String): Unit = {
    Logger.info("Light bridge connected")
    phHueSDK.setSelectedBridge(phBridge)
    phHueSDK.enableHeartbeat(phBridge, PHHueSDK.HB_INTERVAL)
    phHueSDK.getLastHeartbeat.put(phBridge.getResourceCache.getBridgeConfiguration.getIpAddress, System.currentTimeMillis())
    bridge = phBridge

    val config = phBridge.getResourceCache.getBridgeConfiguration
    lightService.setServiceConf(PhilipsConf(Some(config.getIpAddress), Some(config.getUsername)))

    lightService.onConnect()
  }

  override def onAuthenticationRequired(accessPoint: PHAccessPoint): Unit = {
    Logger.info("Light bridge auth required - waiting push button")
    phHueSDK.startPushlinkAuthentication(accessPoint)
  }

  override def onConnectionResumed(bridge: PHBridge): Unit = {
    //Logger.info(f"Light connection resumed ${bridge.getResourceCache.getBridgeConfiguration.getIpAddress}")
    phHueSDK.getLastHeartbeat.put(bridge.getResourceCache.getBridgeConfiguration.getIpAddress,  System.currentTimeMillis)

    phHueSDK.getDisconnectedAccessPoint.asScala.foreach { ap =>
      if (ap.getIpAddress.equals(bridge.getResourceCache.getBridgeConfiguration.getIpAddress)) {
        Logger.info("Light access point removed")
        phHueSDK.getDisconnectedAccessPoint.remove(ap)
      }
    }
  }

  override def onConnectionLost(accessPoint: PHAccessPoint): Unit = {
    Logger.info(s"Light connection lost: ${accessPoint.getIpAddress}")
    if (!phHueSDK.getDisconnectedAccessPoint.contains(accessPoint)) {
      phHueSDK.getDisconnectedAccessPoint.add(accessPoint)
    }
  }

  override def onError(code: Int, message: String): Unit = {
    Logger.error(f"on Error Called: $code: $message")
  }

  override def onCacheUpdated(flags: util.List[Integer], phBridge: PHBridge): Unit = {}

  override def onParsingErrors(list: util.List[PHHueParsingError]): Unit = {
    Logger.error("On parsing errors")
  }
}

object LightListener {
  def apply(lightService: LightService) = new LightListener(lightService)
}
