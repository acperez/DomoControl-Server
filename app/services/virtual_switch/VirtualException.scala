package services.virtual_switch

trait VirtualException extends Exception

case class VirtualDeviceNotFoundException(id: String) extends Exception(f"No virtual device with $id available") with VirtualException
case class VirtualDeviceSetException(id: String) extends Exception(f"Failed to set status of switch $id") with VirtualException
