package services.philips_hue

trait LightException extends Exception

case class BridgeNotAvailableException() extends Exception("Philips Hue bridge not available") with LightException

case class LightUpdateException(error: String) extends Exception(error) with LightException

case class LightNotFoundException(lightId: String) extends Exception(f"Id '$lightId' not found in light service") with LightException

case class SceneNotFoundException(sceneId: String) extends Exception(f"Scene with id '$sceneId' not found in light service") with LightException

case class SceneDeleteForbidenException(sceneId: String) extends Exception(f"Default Scene '$sceneId' can not be deleted") with LightException
