package services.common

import models.config.PhilipsScene
import play.api.mvc.Result

import scala.concurrent.Future

trait DomoPhilipsService extends DomoSwitchService {

  def setLightsColor(lights: Seq[String], color: Seq[Int]): Future[Result]

  def loadScene(sceneId: String): Future[Result]

  def saveScene(scene: PhilipsScene): Future[Result]

  def removeScene(sceneId: String): Future[Result]

}
