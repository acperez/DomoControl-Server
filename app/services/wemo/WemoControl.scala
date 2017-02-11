package services.wemo

import models.config.WemoConf
import play.Logger
import services.common.DomoSwitch
import services.wemo.ssdp.SSDPFinder

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

object WemoControl {

  def isConnected(wemoConfig: WemoConf): Boolean =
    wemoConfig.devices.nonEmpty

  def connect(wemoService: WemoService): Unit = {
    val searchRequest: Future[Seq[WemoDevice]] = SSDPFinder.request(wemoService)
    searchRequest.onComplete {
      case Success(device) =>
        wemoService.setServiceConf(
          WemoConf((wemoService.getServiceConf.devices ++ device).distinct)
        )

      case Failure(error) =>
        Logger.warn(s"Failed to connect to WemoDevice: ${error.getMessage}")
    }
  }

  def setSwitchStatus(wemoConfig: WemoConf, id: String, status: Boolean): Unit = {
    wemoConfig.devices.find(_.serial == id) match {
      case None => throw new Exception(s"No wemo device with id $id available")
      case Some(device) =>
        device.setState(status)
    }
  }

  def getSwitchStatus(wemoConfig: WemoConf, id: String): Option[Boolean] = {
    wemoConfig.devices.find(_.serial == id) match {
      case None => throw new Exception(s"No wemo device with $id available")
      case Some(device) =>
        device.getState()
    }
  }

  def getDevices(wemoConfig: WemoConf): Seq[DomoSwitch] = {
    wemoConfig.devices.map{ device =>
      val state = device.getState()
      if (state.isEmpty)
        DomoSwitch(WemoService.serviceId, device.serial, status = false, device.name, available = false)
      else
        DomoSwitch(WemoService.serviceId, device.serial, state.get, device.name, available = true)
    }
  }
}
