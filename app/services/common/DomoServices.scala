package services.common

import javax.inject.{Inject, Singleton}

import play.api.libs.json.{Json, Writes}
import services.philips_hue.LightService
import services.wemo.WemoService

case class DomoServiceContainer(id: Int, name: String, service: DomoService)

object DomoServiceContainer {
  implicit val serviceWrites = new Writes[DomoServiceContainer] {
    def writes(domoServiceContainer: DomoServiceContainer) = Json.obj(
      "id" -> domoServiceContainer.id,
      "name" -> domoServiceContainer.name
    )
  }
}

@Singleton
class DomoServices @Inject()(
  lightService: LightService,
  wemoService: WemoService) {

  def services = Map(
    lightService.serviceId -> DomoServiceContainer(lightService.serviceId, lightService.serviceName, lightService),
    wemoService.serviceId -> DomoServiceContainer(wemoService.serviceId, wemoService.serviceName, wemoService)
  )

  def lightScenes = lightService.getScenes()
}
