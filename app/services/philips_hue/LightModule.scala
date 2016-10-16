package services.philips_hue

import javax.inject.{Inject, Singleton}

import com.google.inject.AbstractModule
import models.config.PhilipsConf
import play.api.inject.ApplicationLifecycle
import services.common.{ConfigLoader, DomoService}
import play.api.Logger

import scala.concurrent.Future

@Singleton
class LightModule extends AbstractModule {
  override def configure() = {
    bind(classOf[LightService]).asEagerSingleton()
  }
}

class LightService @Inject()(
    appLifecycle: ApplicationLifecycle,
    configLoader: ConfigLoader) extends DomoService {


  override def serviceId: Int = 1

  override def connected: Boolean = false

  private def init(): Unit = {
    Logger.info("Init Philips Hue Module")

    val conf = configLoader.getConfig(PhilipsConf(None, None))
      .asInstanceOf[PhilipsConf]

    Logger.info("Lights config: " + conf)
  }

  init()

  appLifecycle.addStopHook { () =>
    Logger.info(s"Stopping LightService")
    Future.successful(())
  }
}
