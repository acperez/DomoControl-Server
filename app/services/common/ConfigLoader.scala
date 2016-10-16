package services.common

import javax.inject.{Inject, Singleton}

import models.config.{PhilipsConf, PhilipsHue}
import play.api.cache.CacheApi
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json._
import reactivemongo.api.commands.WriteResult
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

abstract class Config(id: String) {
  def getId = id
}

@Singleton
class ConfigLoader @Inject()(cache: CacheApi, reactiveMongoApi: ReactiveMongoApi) {

  def collection = reactiveMongoApi.database.map(_.collection[JSONCollection]("config"))

  def getConfig(defaultConf: Config): Config = {
    val id = defaultConf.getId
    cache.getOrElse(f"conf-$id") {
      val result = Await.result(load(id), Duration.apply(5, "seconds"))
      result.getOrElse{
        save(defaultConf)
        cache.set(s"conf-$id", defaultConf)
        defaultConf
      }
    }
  }

  def load(id: String)(implicit ec: ExecutionContext): Future[Option[Config]] = {
    id match {
      case PhilipsHue.id =>
        collection
          .flatMap(_.find(Json.obj("id" -> id))
            .one[PhilipsConf])
    }
  }

  def save(conf: Config)(implicit ec: ExecutionContext): Future[WriteResult] = {
    collection
      .flatMap(_.insert(conf))
  }

  implicit val configLoaderWrites: OWrites[Config] =
    new OWrites[Config] {
      def writes(o: Config): JsObject = o match {
        case s: PhilipsConf => PhilipsConf.writes.writes(s)
      }
    }
}
