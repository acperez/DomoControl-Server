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

  def getDevices(wemoService: WemoService): Future[JsValue] = {
    val switches = wemoService.getServiceConf.devices.filter(_.failedConnections < 1).map { device =>
      device.getState()
        .recover { case ex =>
          Logger.warn(f"Could not connect to device ${device.name}: $ex")

          val devices = wemoService.getServiceConf.devices.filter(_.name != device.name) :+ device.setFailedConnections(device.failedConnections + 1)
          wemoService.setServiceConf(WemoConf(devices))

          DomoSwitch(WemoService.serviceId, device.serial, status = false, device.name, device.alias, available = false)
        }
    }

    Future.sequence(switches)
      .map(switches => Json.toJson(switches))

    /*
    // Reset failed connections while config not implemented
    val switchess = wemoService.getServiceConf.devices.map(device => device.setFailedConnections(0))
    wemoService.setServiceConf(WemoConf(switchess))
     */
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

  def setSwitchAlias(wemoService: WemoService, id: String, alias: String): Future[Unit] = {
    wemoService.getServiceConf.devices.find(_.serial == id) match {
      case None => Future.failed(WemoDeviceNotFoundException(id))
      case Some(device) =>
        val devices = wemoService.getServiceConf.devices.filter(_.name != device.name) :+ device.setAlias(alias)
        wemoService.setServiceConf(WemoConf(devices))
        Future.successful(Unit)
    }
  }

  def getWemoUsage(id: String, wemoConfig: WemoConf): Future[Option[WemoMonitorData]] = {
    wemoConfig.devices
      .find(device => device.name == id && device.deviceType == WemoDeviceType.Monitor) match {
      case Some(device) =>
        val usage = device.getUsage()
          .recover { case ex =>
            Logger.warn(f"Could not connect to device ${device.name}: $ex")
            None
          }
        usage

      case _ =>
        Logger.error(s"Device $id not found or it is not a Monitor device")
        Future(None)
    }
  }

  def getWemoDevicesUsage(wemoConfig: WemoConf): Future[Seq[WemoMonitorData]] = {
    val futures = wemoConfig.devices.filter(_.deviceType == WemoDeviceType.Monitor).map { device =>
      device.getUsage()
        .recover { case ex =>
          Logger.warn(f"Could not connect to device ${device.name}: $ex")
          None
        }
    }

    Future.sequence(futures).map(_.flatten)
  }
}
