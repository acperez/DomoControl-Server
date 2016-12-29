package services.common

import javax.inject.{Inject, Singleton}

import models.config.PhilipsScene
import net.sf.ehcache.Ehcache
import play.api.{Environment, Logger, Play}
import play.api.cache.CacheApi
import play.api.libs.json._
import play.api.Play.current
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json._
import reactivemongo.api.Cursor
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source._
import scala.util.{Failure, Success}


@Singleton
class SceneManager @Inject()(cache: CacheApi, env: Environment, reactiveMongoApi: ReactiveMongoApi) {

  def collection = reactiveMongoApi.database.map(_.collection[JSONCollection]("scenes"))

  collection.map(_.indexesManager.ensure(Index(Seq("id" -> IndexType.Ascending), unique = true)))

  def bootstrap(): Unit = {
    load().onComplete {
      case Success(result) =>
        val scenes = (if (result.isEmpty) populateScenes() else result)
          .map(scene => (scene.id -> scene)).toMap
        cache.set(s"scenes", scenes)

      case Failure(error) =>
        Logger.error(s"Database error: ${error.getMessage}")
    }
  }

  private def populateScenes(): Seq[PhilipsScene] = {
    Option(env.classLoader.getResourceAsStream("scenes.json")) match {
      case None =>
        Logger.error(s"Failed to load scenes.json")
        Seq()

      case Some(inputStream) =>
        val data = fromInputStream(inputStream).mkString
        val scenes = Json.parse(data).as[Seq[PhilipsScene]]

        scenes.foreach { scene =>
          val result = collection
            .flatMap(_.insert(scene))

          result.onFailure{
            case error => Logger.error(f"$error")
          }
        }

        scenes
    }
  }

  def load()(implicit ec: ExecutionContext, reads: Reads[PhilipsScene]): Future[Seq[PhilipsScene]] = {
    collection
      .flatMap(_.find(Json.obj())
        .cursor[PhilipsScene]()
        .collect[Seq](Int.MaxValue, Cursor.FailOnError[Seq[PhilipsScene]]()))
  }

  def get(index: Int): Option[PhilipsScene] = {
    cache.getOrElse[Map[Int, PhilipsScene]]("scenes")(Map()).get(index)
  }

  def getAll(): Seq[PhilipsScene] = {
    cache
      .getOrElse[Map[Int, PhilipsScene]]("scenes")(Map())
      .values
      .toSeq
      .sortBy(_.id)
  }
  /*
  def count()(implicit ec: ExecutionContext): Future[Int] = {
    collection
      .flatMap(_.count())
  }

  def getScene(id: Int)(implicit ec: ExecutionContext, reads: Reads[PhilipsScene]): Future[Option[PhilipsScene]] = {
    collection
      .flatMap(_.find(Json.obj("id" -> id))
        .one[PhilipsScene])
  }*/
}
