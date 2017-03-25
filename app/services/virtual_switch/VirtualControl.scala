package services.virtual_switch

import models.config.VirtualConf
import play.api.libs.json.{JsValue, Json}
import request_models.virtual.VirtualDeviceAddRequest
import services.common.{DomoServices, DomoSwitch}
import java.util.UUID

import play.api.mvc.Results

import scala.concurrent.{ExecutionContext, Future}

object VirtualControl {

  def getDevices(service: VirtualService): Future[JsValue] = {
    val switches = service.getServiceConf.devices.map { device =>
      DomoSwitch(service.id, device.id, status = false, device.name, device.alias, available = true)
    }

    Future.successful(Json.toJson(switches))
  }

  def getDetailedDevices(service: VirtualService): Future[Seq[VirtualDevice]] = {
    val switches = service.getServiceConf.devices
    Future.successful(switches)
  }

  def getSwitch(service: VirtualService, id: String): Future[DomoSwitch] = {
    service.getServiceConf.devices.find(_.id == id) match {
      case Some(device) =>
        val switch = DomoSwitch(service.id, device.id, status = false, device.name, device.alias, available = true)
        Future.successful(switch)
      case None => Future.failed(VirtualDeviceNotFoundException(id))
    }
  }

  def setSwitchStatus(service: VirtualService, domoServices: DomoServices, id: String, status: Boolean)(implicit ec: ExecutionContext): Future[Unit] = {
    service.getServiceConf.devices.find(_.id == id) match {
      case None => Future.failed(VirtualDeviceNotFoundException(id))
      case Some(device) =>
        val futures = device.switches.map { mapping =>
          domoServices.services.get(mapping.serviceId) match {
            case None => Future.failed(VirtualDeviceNotFoundException(id))
            case Some(serviceMapping) => serviceMapping.setSwitchStatus(mapping.switchId, status)
          }
        }

        Future.sequence(futures).map { results =>
          val failed = results.find(_ != Results.Ok)
          if (failed.nonEmpty) Future.failed(VirtualDeviceSetException(id))
        }
    }
  }

  def setSwitchAlias(service: VirtualService, id: String, alias: String): Future[Unit] = {
    service.getServiceConf.devices.find(_.id == id) match {
      case None => Future.failed(VirtualDeviceNotFoundException(id))
      case Some(device) =>
        val devices = service.getServiceConf.devices.filter(_.name != device.name) :+ device.setAlias(alias)
        service.setServiceConf(VirtualConf(devices))
        Future.successful(Unit)
    }
  }

  def addSwitch(service: VirtualService, request: VirtualDeviceAddRequest): Future[Unit] = {
    val id = UUID.randomUUID.toString
    val device = VirtualDevice(id, request.name, Some(request.name), request.switches)
    val devices = service.getServiceConf.devices :+ device
    service.setServiceConf(VirtualConf(devices))
    Future.successful(Unit)
  }

  def removeSwitch(service: VirtualService, switchId: String): Future[Unit] = {
    val devices = service.getServiceConf.devices
    if (devices.exists(_.id == switchId)) {
      val filteredDevices = service.getServiceConf.devices.filterNot(_.id == switchId)
      service.setServiceConf(VirtualConf(filteredDevices))
      Future.successful(Unit)
    } else Future.failed(VirtualDeviceNotFoundException(switchId))
  }

  def updateDevice(service: VirtualService, switchId: String, mappings: Seq[SwitchMapping]): Future[Unit] = {
    val deviceOption = service.getServiceConf.devices.find(_.id == switchId)
    deviceOption match {
      case None => Future.failed(VirtualDeviceNotFoundException(switchId))
      case Some(device) =>
        val updated = VirtualDevice(device.id, device.name, device.alias, mappings)
        val filteredDevices = service.getServiceConf.devices.filterNot(_.id == switchId)
        service.setServiceConf(VirtualConf(filteredDevices :+ updated))
        Future.successful(Unit)
    }
  }

}
