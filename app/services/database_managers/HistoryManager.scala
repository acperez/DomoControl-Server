package services.database_managers

import java.util.{Calendar, GregorianCalendar}
import javax.inject.{Inject, Singleton}

import play.api.libs.json._
import play.api.Environment
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json._
import reactivemongo.api.Cursor
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.play.json.collection.JSONCollection
import services.wemo.WemoMonitorData

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HistoryManager @Inject()(env: Environment, reactiveMongoApi: ReactiveMongoApi) {

  def collection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection[JSONCollection]("wemo_history"))
  collection.map(_.indexesManager.ensure(Index(Seq("id" -> IndexType.Text, "timestamp" -> IndexType.Text), unique = true)))

  def save(data: WemoMonitorData)(implicit ec: ExecutionContext, writes: OWrites[WemoMonitorData]): Future[Option[WemoMonitorData]] = {
    collection
      .flatMap(_.findAndUpdate(Json.obj("id" -> data.id, "timestamp" -> data.timestamp), data, upsert = true)
        .map(_.result[WemoMonitorData]))
  }

  def remove(id: String)(implicit ec: ExecutionContext, reads: Reads[WemoMonitorData]): Future[WriteResult] = {
    collection
      .flatMap(_.remove(Json.obj("id" -> id)))
  }

  def removeOld(timestamp: Long)(implicit ec: ExecutionContext, reads: Reads[WemoMonitorData]): Future[Option[WemoMonitorData]] = {
    collection.flatMap(_.findAndRemove(Json.obj("timestamp" -> Map("$lt" -> timestamp)))
      .map(_.result[WemoMonitorData]))
  }

  def getMonthHistory(id: String, month: Int): Future[JsValue] = {
    val calendar = new GregorianCalendar()
    val period = new GregorianCalendar(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) - month, 1)

    val monthDays = period.getActualMaximum(Calendar.DAY_OF_MONTH)
    val from = new GregorianCalendar(period.get(Calendar.YEAR), period.get(Calendar.MONTH), 1).getTimeInMillis
    val to = new GregorianCalendar(period.get(Calendar.YEAR), period.get(Calendar.MONTH), monthDays, 23, 59, 59).getTimeInMillis
    find(id, from, to)
      .map { days =>
        val jsonDays = (0 until monthDays).map { day =>
          val timestamp = from + day * (1000 * 60 * 60 * 24)
          days.find(_.timestamp == timestamp) match {
            case Some(usage) => Json.obj(
              "timestamp" -> timestamp,
              "usage" -> Json.toJson(usage))

            case None => Json.obj(
              "timestamp" -> timestamp,
              "usage" -> JsNull)
          }
        }

        Json.toJson(jsonDays)
      }
  }

  def find(id: String, fromTime: Long, toTime: Long)(implicit ec: ExecutionContext, reads: Reads[WemoMonitorData]): Future[Seq[WemoMonitorData]] = {
    collection
      .flatMap(_.find(Json.obj("id" -> id, "timestamp" -> Map("$gt" -> fromTime, "$lt" -> toTime)))
        .cursor[WemoMonitorData]()
        .collect[Seq](Int.MaxValue, Cursor.FailOnError[Seq[WemoMonitorData]]()))
  }

  def generateFakeUsageData(ids: Seq[String]): Unit = {
    val random = new scala.util.Random

    ids.foreach { id =>
      val now = new GregorianCalendar()
      val currentMonth = new GregorianCalendar(now.get(Calendar.YEAR), now.get(Calendar.MONTH), 1)
      val lastMonth = new GregorianCalendar(now.get(Calendar.YEAR), now.get(Calendar.MONTH) - 1, 1)

      val monthDays = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH) + lastMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
      val from = lastMonth.getTimeInMillis

      (0 until monthDays).map { day =>
        val timestamp = from + day * (1000L * 60 * 60 * 24)
        val data = WemoMonitorData(
          id,
          None,
          timestamp,
          random.nextInt(2) == 1,
          random.nextLong(),
          random.nextDouble(),
          random.nextDouble(),
          random.nextDouble(),
          random.nextLong(),
          random.nextDouble(),
          random.nextDouble(),
          random.nextDouble(),
          random.nextDouble(),
          random.nextDouble()
        )
        save(data)
      }
    }
  }
}
