package services.wemo

import models.config.WemoConf
import play.Logger
import services.common.DomoSwitch
import services.wemo.ssdp.SSDPFinder

import scala.concurrent.Future
import scala.util.{Failure, Success}

import scala.concurrent.ExecutionContext.Implicits.global

object WemoControl {

  def isConnected(wemoConfig: WemoConf): Boolean = {
    wemoConfig.device match {
      case None => false
      case Some(device) => true
    }
  }


  def connect(wemoConfig: WemoConf, wemoService: WemoService): Unit = {
    if (wemoConfig.device.isEmpty) {
      val searchRequest: Future[Device] = SSDPFinder.request()
      searchRequest.onComplete{
        case Success(device) =>
          wemoService.setServiceConf(
            WemoConf(device.description, Some(device.baseUrl.toString), Some(device.deviceType), device.name)
          )

        case Failure(error) =>
          Logger.error(s"Failed to connect to WemoDevice: ${error.getMessage}")
      }
    }
  }

  def setSwitchStatus(wemoConfig: WemoConf, id: Int, status: Boolean): Unit = {
    wemoConfig.device match {
      case None => throw new Exception("No wemo device available")
      case Some(device) =>
        device.setState(status)
    }
  }

  def getSwitchStatus(wemoConfig: WemoConf, id: Int): Boolean = {
    wemoConfig.device match {
      case None => throw new Exception("No wemo device available")
      case Some(device) =>
        device.getState()
    }
  }

  def getDevices(wemoConfig: WemoConf): Seq[DomoSwitch] = {
    wemoConfig.device match {
      case None => Seq()
      case Some(device) =>
        Seq(DomoSwitch(WemoService.serviceId, 0, device.getState(), device.name.getOrElse("WemoDevice")))
    }
  }
}
