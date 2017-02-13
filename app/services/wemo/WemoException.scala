package services.wemo

trait WemoException extends Exception

case class MaxRetriesException(error: String) extends Exception(f"Max number of retries connecting to Wemo device. Last error $error") with WemoException

case class WemoDeviceNotFoundException(id: String) extends Exception(f"No wemo device with $id available") with WemoException
