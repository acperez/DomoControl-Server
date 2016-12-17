package services.common

import javax.inject.{Inject, Singleton}

import com.google.inject.AbstractModule
import play.api.Logger
import play.api.inject.ApplicationLifecycle
import sun.misc.{Signal, SignalHandler}

import scala.concurrent.Future

@Singleton
class DomoModule extends AbstractModule {
  override def configure() = {
    bind(classOf[DomoModuleProxy]).asEagerSingleton()
  }
}

@Singleton
class DomoModuleProxy @Inject() (appLifecycle: ApplicationLifecycle, domoServices: DomoServices) {
  domoServices.services.values.foreach { serviceContainer =>
    serviceContainer.service.init()
  }

  appLifecycle.addStopHook { () =>
    domoServices.services.values.foreach { serviceContainer =>
      serviceContainer.service.stop()
    }
    Future.successful(())
  }
}
