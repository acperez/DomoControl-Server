package services.common

import java.util.Calendar
import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import com.google.inject.AbstractModule
import play.api.Logger
import play.api.inject.ApplicationLifecycle

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps

@Singleton
class DomoModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[DomoModuleProxy]).asEagerSingleton()
  }
}

@Singleton
class DomoModuleProxy @Inject() (appLifecycle: ApplicationLifecycle, serviceManager: DomoServiceManager, system: ActorSystem) {
  serviceManager.services.values.foreach { service =>
    service.init()
  }

  appLifecycle.addStopHook { () =>
    serviceManager.services.values.foreach { service =>
      service.stop()
    }
    Future.successful(())
  }

  registerDailyJob( () => {
    Logger.info("Execute daily scheduled operations.")
    serviceManager.services.values.foreach { service =>
      service.cron()
    }
  }, 23, 0)

  def registerDailyJob(job: () => Unit, hour:Int, minute:Int) {
    val minMs = 60 * 1000
    val hourMs = minMs * 60
    val dayMs = hourMs * 24

    val initialDelayMs = getNextInitialDelayMs(Calendar.getInstance(), hour, minute)

    Logger.info(f"Register daily job at $hour%02d:$minute%02d")
    system.scheduler.schedule(initialDelayMs milliseconds, dayMs milliseconds) {
      job()
    }
  }


  def getNextInitialDelayMs(from: Calendar, hour: Int, minute: Int): Long = {
    val next:Calendar = from.clone().asInstanceOf[Calendar]
    next.set(Calendar.HOUR_OF_DAY, hour)
    next.set(Calendar.MINUTE, minute)
    if (next.compareTo(from) < 0)
      next.add(Calendar.DATE, 1)

    next.getTimeInMillis - from.getTimeInMillis
  }
}
