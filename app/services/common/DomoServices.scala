package services.common

import javax.inject.{Inject, Singleton}

import play.api.libs.json.JsValue
import services.philips_hue.LightService
import services.wemo.WemoService

@Singleton
class DomoServices @Inject()(
  lightService: LightService,
  wemoService: WemoService) {

  def services = Map(
    lightService.id -> lightService,
    wemoService.id -> wemoService
  )

  def lightScenes: JsValue = lightService.getScenes
}
