package services.philips_hue

import java.util
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}

import akka.actor.{Actor, ActorRef, Props}
import com.philips.lighting.hue.listener.PHLightListener
import com.philips.lighting.hue.sdk.PHHueSDK
import com.philips.lighting.model.{PHBridgeResource, PHHueError, PHLight}
import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.language.postfixOps

case class ColorRequest(sender: ActorRef, ids: Seq[String], color: Seq[Int])

class PhilipsActor(phHueSDK: PHHueSDK) extends Actor {
  import services.philips_hue.PhilipsActor._

  def receive: Actor.Receive = {
    case SetColor(id: Seq[String], color: Seq[Int]) =>
      val oldValue = buffer.getAndSet(ColorRequest(sender, id, color))
      if (oldValue != null) oldValue.sender ! true
      else execute()

    case _ => Logger.info("Philips Actor received unknown message")
  }

  def execute(): Unit = {
    if (running.compareAndSet(false, true)) {
      Future {
        val request = buffer.getAndSet(null)
        if (request != null) {
          Await.result(setLightsColor(request.ids, request.color), 5 seconds)
          request.sender ! true
          running.set(false)
          execute()
        }
        else running.set(false)
      }
    }
  }


  def setLightsColor(ids: Seq[String], color: Seq[Int])(implicit ec: ExecutionContext): Future[Seq[Boolean]] = {
    val tasks = ids.map { id =>
      setLightColor(id, color)
    }

    Future.sequence(tasks)
  }

  def setLightColor(id: String, color: Seq[Int])(implicit ec: ExecutionContext): Future[Boolean] = {
    val promise = Promise[Boolean]
    Future {
      val bridge = phHueSDK.getSelectedBridge
      if (bridge == null) {
        Logger.error("Philips Hue bridge not available")
        promise.success(true)
      }
      else {
        bridge.updateLightState(id, LightUtils.createColorStateFromRGB(color), new PHLightListener {
          override def onReceivingLights(list: util.List[PHBridgeResource]): Unit = {}

          override def onSearchComplete(): Unit = {}

          override def onReceivingLightDetails(phLight: PHLight): Unit = {}

          override def onError(i: Int, s: String): Unit = {
            promise.success(true)
          }

          override def onStateUpdate(map: util.Map[String, String], list: util.List[PHHueError]): Unit = {}

          override def onSuccess(): Unit = {
            promise.success(true)
          }
        })
      }
    }

    promise.future
  }
}

object PhilipsActor {
  def props(phHueSDK: PHHueSDK): Props = Props(new PhilipsActor(phHueSDK))

  case class SetColor(id: Seq[String], color: Seq[Int])

  val buffer = new AtomicReference[ColorRequest]()
  val running = new AtomicBoolean(false)
}
