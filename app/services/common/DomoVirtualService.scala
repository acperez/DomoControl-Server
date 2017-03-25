package services.common

import play.api.mvc.Result
import request_models.virtual.VirtualDeviceAddRequest
import services.virtual_switch.SwitchMapping

import scala.concurrent.Future

trait DomoVirtualService extends DomoSwitchService {
  def addDevice(virtualDeviceRequest: VirtualDeviceAddRequest): Future[Result]
  def removeDevice(switchId: String): Future[Result]
  def updateDevice(switchId: String, virtualDeviceRequest: Seq[SwitchMapping]): Future[Result]
}
