package services.wemo

import models.config.WemoConf
import play.Logger
import play.api.libs.json.{JsValue, Json}
import services.common.DomoSwitch
import services.wemo.ssdp.SSDPFinder

import scala.concurrent.{Future, Promise}
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

  def getDevices(wemoConfig: WemoConf): Future[JsValue] = {
    val switches = wemoConfig.devices.map { device =>
      device.getState()
        .recover { case ex =>
          Logger.warn(f"Could not connect to device ${device.name}: $ex")
          DomoSwitch(WemoService.serviceId, device.serial, status = false, device.name, available = false)
        }
    }

    Future.sequence(switches)
      .map(switches => Json.toJson(switches))
  }

  def getSwitchStatus(wemoConfig: WemoConf, id: String): Future[DomoSwitch] = {
    wemoConfig.devices.find(_.serial == id) match {
      case Some(device) => device.getState()
      case None =>
        val promise = Promise[DomoSwitch]
        Future {
          promise.failure(WemoDeviceNotFoundException(id))
        }
        promise.future
    }
  }

  def setSwitchesStatus(wemoConfig: WemoConf, status: Boolean): Future[Boolean] = {
    val updates = wemoConfig.devices.map { device =>
      device.setState(status)
    }

    Future.sequence(updates).map(_ => status)
  }

  def setSwitchStatus(wemoConfig: WemoConf, id: String, status: Boolean): Future[Boolean] = {
    wemoConfig.devices.find(_.serial == id) match {
      case Some(device) => device.setState(status)
      case None =>
        val promise = Promise[Boolean]
        Future {
          promise.failure(WemoDeviceNotFoundException(id))
        }
        promise.future
    }
  }
}
