package services.wemo.ssdp

import java.io.{InputStream, OutputStream}
import java.net.{HttpURLConnection, URL}
import java.util.{Calendar, GregorianCalendar}

import services.wemo.{MaxRetriesException, WemoMonitorData}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.io.Source.fromInputStream
import scala.util.{Failure, Success, Try}

case class Device(id: String, baseUrl: URL, deviceType: String) {

  def validType(): Boolean = deviceType.equalsIgnoreCase("urn:Belkin:device:socket:1") ||
    deviceType.equalsIgnoreCase("urn:Belkin:device:sensor:1") ||
    deviceType.equalsIgnoreCase("urn:Belkin:device:lightswitch:1") ||
    deviceType.equalsIgnoreCase("urn:Belkin:device:controllee:1") ||
    deviceType.equalsIgnoreCase("urn:Belkin:device:NetCamSensor:1") ||
    deviceType.equalsIgnoreCase("urn:Belkin:device:insight:1")

  def getState(retries: Int)(implicit ec: ExecutionContext): Future[Boolean] = {
    val promise = Promise[Boolean]
    Future {
      internalGetStateManager(retries, promise)
    }
    promise.future
  }

  private def internalGetStateManager(retries: Int, promise: Promise[Boolean], lastError: String = ""): Unit = {
    if (retries < 0) promise.failure(MaxRetriesException(lastError))
    else {
      Try(internalGetState()) match {
        case Success(result) => promise.success(result)
        case Failure(error) => internalGetStateManager(retries - 1, promise, error.getMessage)
      }
    }
  }

  private def internalGetState(): Boolean = {
    val msg: Array[Byte] = WemoHTTPMsg.getState
    val url: URL = new URL(baseUrl.getProtocol, baseUrl.getHost, baseUrl.getPort, "/upnp/control/basicevent1")
    val connection: HttpURLConnection = url.openConnection.asInstanceOf[HttpURLConnection]

    connection.setReadTimeout(6000)
    connection.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"")
    connection.setRequestProperty("HOST", baseUrl.getHost)
    connection.setRequestProperty("Content-Length", "" + msg.length)
    connection.setRequestProperty("SOAPACTION", "\"urn:Belkin:service:basicevent:1#GetBinaryState\"")

    connection.setRequestMethod("POST")
    connection.setDoOutput(true)
    connection.setDoInput(true)

    val os: OutputStream = connection.getOutputStream
    os.write(msg, 0, msg.length)
    os.flush()
    os.close()

    val is: InputStream = connection.getInputStream
    val response: String = fromInputStream(is).mkString
    is.close()
    connection.disconnect()
    parseGetBinaryState(response)
  }

  def setState(state: Boolean, retries: Int)(implicit ec: ExecutionContext): Future[Boolean] = {
    val promise = Promise[Boolean]
    Future {
      internalSetStateManager(state, retries, promise)
    }
    promise.future
  }

  private def internalSetStateManager(state: Boolean, retries: Int, promise: Promise[Boolean], lastError: String = ""): Unit = {
    if (retries < 0) promise.failure(MaxRetriesException(lastError))
    else {
      Try(internalSetState(state)) match {
        case Success(_) => promise.success(state)
        case Failure(error) => internalSetStateManager(state, retries - 1, promise, error.getMessage)
      }
    }
  }

  private def internalSetState(state: Boolean): Unit = {
    val msg: Array[Byte] = WemoHTTPMsg.setState(state)
    val url: URL = new URL(baseUrl.getProtocol, baseUrl.getHost, baseUrl.getPort, "/upnp/control/basicevent1")
    val connection: HttpURLConnection = url.openConnection.asInstanceOf[HttpURLConnection]

    connection.setReadTimeout(6000)
    connection.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"")
    connection.setRequestProperty("HOST", baseUrl.getHost)
    connection.setRequestProperty("Content-Length", "" + msg.length)
    connection.setRequestProperty("SOAPACTION", "\"urn:Belkin:service:basicevent:1#SetBinaryState\"")

    connection.setRequestMethod("POST")
    connection.setDoOutput(true)
    connection.setDoInput(true)

    val os: OutputStream = connection.getOutputStream
    os.write(msg, 0, msg.length)
    os.flush()
    os.close()

    val is: InputStream = connection.getInputStream
    is.close()
    connection.disconnect()
  }

  private def parseGetBinaryState(response: String): Boolean = {
    val field: String = "<BinaryState>"
    if (response.contains(field)) {
      val pos: Int = response.indexOf(field) + field.length
      "1" == response.substring(pos, pos + 1)
    } else throw new Exception("Invalid response for GetBinaryState")
  }

  def getUsage(retries: Int)(implicit ec: ExecutionContext): Future[Option[WemoMonitorData]] = {
    val promise = Promise[Option[WemoMonitorData]]
    Future {
      internalGetUsageManager(retries, promise)
    }
    promise.future
  }

  private def internalGetUsageManager(retries: Int, promise: Promise[Option[WemoMonitorData]], lastError: String = ""): Unit = {
    if (retries < 0) promise.failure(MaxRetriesException(lastError))
    else {
      Try(internalGetUsage()) match {
        case Success(result) => promise.success(result)
        case Failure(error) => internalGetUsageManager(retries - 1, promise, error.getMessage)
      }
    }
  }

  private def internalGetUsage(): Option[WemoMonitorData] = {
    val msg: Array[Byte] = WemoHTTPMsg.getMonitorData
    val url: URL = new URL(baseUrl.getProtocol, baseUrl.getHost, baseUrl.getPort, "/upnp/control/insight1")
    val connection: HttpURLConnection = url.openConnection.asInstanceOf[HttpURLConnection]

    connection.setReadTimeout(6000)
    connection.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"")
    connection.setRequestProperty("HOST", baseUrl.getHost)
    connection.setRequestProperty("Content-Length", "" + msg.length)
    connection.setRequestProperty("SOAPACTION", "\"urn:Belkin:service:insight:1#GetInsightParams\"")

    connection.setRequestMethod("POST")
    connection.setDoOutput(true)
    connection.setDoInput(true)

    val os: OutputStream = connection.getOutputStream
    os.write(msg, 0, msg.length)
    os.flush()
    os.close()

    val is: InputStream = connection.getInputStream
    val response: String = fromInputStream(is).mkString
    is.close()
    connection.disconnect()

    parseMonitorUsage(response)
  }

  private def parseMonitorUsage(response: String): Option[WemoMonitorData] = {
    val fieldStart: String = "<InsightParams>"
    val fieldEnd: String = "</InsightParams>"
    val indexStart = response.indexOf(fieldStart)
    val indexEnd = response.indexOf(fieldEnd)

    if (indexStart >= 0 && indexEnd >= 0) {
      val data = response.substring(indexStart + fieldStart.length, indexEnd).split("\\|")


      val now = new GregorianCalendar()
      val timestamp = new GregorianCalendar(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH))
        .getTimeInMillis

      Some(WemoMonitorData(
        id = id,
        timestamp = timestamp,
        state = data(0).toInt > 0,
        lastStateChange = data(1).toLong * 1000,
        lastOnFor = data(2).toDouble,
        onToday = data(3).toDouble,
        onTotal = data(4).toDouble,
        timeSpan = data(5).toLong,
        averagePowerW = data(6).toDouble,
        currentW = Math.round(data(7).toDouble / 1000),
        energyTodayWh = Math.round(data(8).toDouble / 60000),
        energyTotalWh = Math.round(data(9).toDouble / 60000),
        standbyLimitW = Math.round(data(10).toDouble / 1000)))
    } else None
  }
}
