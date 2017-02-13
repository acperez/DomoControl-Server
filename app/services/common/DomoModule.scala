package services.common

import javax.inject.{Inject, Singleton}

import com.google.inject.AbstractModule
import play.api.inject.ApplicationLifecycle

import scala.concurrent.Future

@Singleton
class DomoModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[DomoModuleProxy]).asEagerSingleton()
  }
}

@Singleton
class DomoModuleProxy @Inject() (appLifecycle: ApplicationLifecycle, domoServices: DomoServices) {
  domoServices.services.values.foreach { service =>
    service.init()
  }

  appLifecycle.addStopHook { () =>
    domoServices.services.values.foreach { service =>
      service.stop()
    }
    Future.successful(())
  }
}
