package services.database_managers

import javax.inject.{Inject, Singleton}

import models.config.PhilipsScene
import play.api.cache.CacheApi
import play.api.libs.json._
import play.api.{Environment, Logger}
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json._
import reactivemongo.api.Cursor
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.play.json.collection.JSONCollection
import services.philips_hue.{SceneDeleteForbidenException, SceneNotFoundException}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.io.Source._
import scala.util.{Failure, Success}

@Singleton
class SceneManager @Inject()(cache: CacheApi, env: Environment, reactiveMongoApi: ReactiveMongoApi) {

  def collection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection[JSONCollection]("scenes"))
  collection.map(_.indexesManager.ensure(Index(Seq("name" -> IndexType.Text), unique = true)))

  def bootstrap(): Unit = {
    load().onComplete {
      case Success(result) =>
        val scenes = (if (result.isEmpty) populateScenes() else result)
          .map(scene => scene.name -> scene).toMap
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

  def get(index: String): Option[PhilipsScene] = {
    cache.getOrElse[Map[String, PhilipsScene]]("scenes")(Map()).get(index)
  }

  def getAll: Seq[PhilipsScene] = {
    cache
      .getOrElse[Map[String, PhilipsScene]]("scenes")(Map())
      .values
      .toSeq
      .sortBy(scene => (!scene.default, scene.name))
  }

  def save(scene: PhilipsScene): Future[Boolean] = {
    saveToDb(scene)
      .map { _ =>
        saveToCache(scene)
        true
      }
  }

  private def saveToDb(scene: PhilipsScene)(implicit ec: ExecutionContext, writes: OWrites[PhilipsScene]): Future[WriteResult] = {
    collection
      .flatMap( x =>
        x.insert(scene)
      )
  }

  private def saveToCache(scene: PhilipsScene): Unit = {
    val scenes = cache.getOrElse[Map[String, PhilipsScene]]("scenes")(Map()) + (scene.name -> scene)
    cache.set("scenes", scenes)
  }

  def remove(sceneId: String): Future[Boolean] = {
    val promise = Promise[Boolean]
    Future {
      get(sceneId.toLowerCase()) match {
        case None => promise.failure(SceneNotFoundException(sceneId))
        case Some(scene) if scene.default => promise.failure(SceneDeleteForbidenException(sceneId))
        case Some(_) =>
          removeFromDb(sceneId.toLowerCase()).onComplete {
            case Success(result) if result.isEmpty => promise.failure(SceneNotFoundException(sceneId))
            case Success(result) =>
              removeFromCache(result.get)
              promise.success(true)
            case Failure(e) => promise.failure(e)
          }
      }
    }

    promise.future
  }

  private def removeFromDb(sceneId: String)(implicit ec: ExecutionContext, reads: Reads[PhilipsScene]): Future[Option[PhilipsScene]] = {
    collection.flatMap(_.findAndRemove(Json.obj("name" -> sceneId))
      .map(_.result[PhilipsScene]))
  }

  private def removeFromCache(scene: PhilipsScene): Unit = {
    val scenes = cache.getOrElse[Map[String, PhilipsScene]]("scenes")(Map()) - scene.name
    cache.set("scenes", scenes)
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
