package services.common

import javax.inject.{Inject, Singleton}

import models.config.DomoConfiguration
import play.api.Logger
import play.api.cache.CacheApi
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json._
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

@Singleton
class ConfigLoader @Inject()(cache: CacheApi, reactiveMongoApi: ReactiveMongoApi) {

  def collection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection[JSONCollection]("config"))
  collection.map(_.indexesManager.ensure(Index(Seq("id" -> IndexType.Ascending), unique = true)))

  def getConfig[T <: DomoConfiguration: ClassTag](defaultConf: T)(implicit reads: Reads[T], writes: OWrites[T]): T = {
    val id = defaultConf.getId
    cache.getOrElse(f"conf-$id") {

      Try(Await.result(load(id), Duration.apply(500, "seconds"))) match {
        case Success(result) =>

          val conf = if (result.isEmpty) {
            updateOrSave(defaultConf)
            defaultConf
          } else result.get

          cache.set(s"conf-$id", conf)
          conf

        case Failure(e) =>
          Logger.error(s"Database error: ${e.getMessage}")
          defaultConf
      }
    }
  }

  def setConfig[T <: DomoConfiguration: ClassTag](conf: T)(implicit writes: OWrites[T]): Unit = {
    val currentConfOption = cache.get(f"conf-${conf.getId}")
    if ((currentConfOption.nonEmpty && !currentConfOption.get.equals(conf)) || currentConfOption.isEmpty) {
      updateOrSave(conf)
      cache.set(s"conf-${conf.getId}", conf)
    }
  }

  def load[T <: DomoConfiguration](id: Int)(implicit ec: ExecutionContext, reads: Reads[T]): Future[Option[T]] = {
    collection
      .flatMap(_.find(Json.obj("id" -> id))
       .one[T])
  }

  def save[T <: DomoConfiguration](conf: T)(implicit ec: ExecutionContext, writes: OWrites[T]): Unit = {
    val result = collection
      .flatMap(
        _.insert(conf)
      )

    result.onFailure{
      case error => Logger.error(f"$error")
    }
  }

  def updateOrSave[T <: DomoConfiguration](conf: T)(implicit ec: ExecutionContext, writes: OWrites[T]): Unit = {
    Logger.info(f"Save $conf")
    val result = collection
      .map(
        _.findAndUpdate(Json.obj("id" -> conf.getId), conf, upsert = true)
      )

    result.onFailure{
      case error => Logger.error(f"$error")
    }
  }
}
