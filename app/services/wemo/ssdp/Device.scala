package services.wemo.ssdp

import java.io.{InputStream, OutputStream}
import java.net.{HttpURLConnection, URL}

import play.api.Logger

import scala.io.Source.fromInputStream
import scala.util.{Failure, Success, Try}

case class Device(id: String, baseUrl: URL, deviceType: String) {

  val CONNECTION_RETRIES: Int = 3

  def validType(): Boolean = deviceType.equalsIgnoreCase("urn:Belkin:device:socket:1") ||
    deviceType.equalsIgnoreCase("urn:Belkin:device:sensor:1") ||
    deviceType.equalsIgnoreCase("urn:Belkin:device:lightswitch:1") ||
    deviceType.equalsIgnoreCase("urn:Belkin:device:controllee:1") ||
    deviceType.equalsIgnoreCase("urn:Belkin:device:NetCamSensor:1") ||
    deviceType.equalsIgnoreCase("urn:Belkin:device:insight:1")

  def getState(retries: Int = CONNECTION_RETRIES): Boolean = {
    if (retries < 0) throw new Exception("Max number of retries to getState of Wemo")

    Try(internalGetState()) match {
      case Success(result) => result
      case Failure(error) =>
        Logger.info(f"Wemo getState error: ${error.getMessage}")
        getState(retries - 1)
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

  def setState(state: Boolean, retries: Int = CONNECTION_RETRIES): Unit = {
    if (retries < 0) throw new Exception("Max number of retries to setState of Wemo")

    val result = Try(internalSetState(state))
    if (result.isFailure) {
      Logger.info(f"Wemo setState error: ${result.failed.get.getMessage}")
      setState(state, retries - 1)
    }
  }

  private def internalSetState(state: Boolean) = {
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
}
