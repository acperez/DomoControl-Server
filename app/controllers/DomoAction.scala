package controllers

import play.api.mvc.ActionBuilder
import services.common.{DomoRequest, DomoService, DomoServiceManager}

import scala.reflect.ClassTag

trait DomoAction [T <: DomoService] {

  def serviceManager: DomoServiceManager

  def DomoAction(id: Int)(implicit c: ClassTag[T]): ActionBuilder[DomoRequest] = {
    serviceManager.serviceRoute[T](id)
  }
}
