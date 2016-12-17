package services.wemo

import java.io.{InputStream, OutputStream}
import java.net.{HttpURLConnection, URL}

import play.api.Logger
import services.wemo.ssdp.{SSDPPacket, WemoHTTPMsg}

import scala.io.Source.fromInputStream
import scala.util.{Failure, Success, Try}

case class Device(baseUrl: URL, deviceType: String, name: Option[String], description: Option[String]) {

  val CONNECTION_RETRIES: Int = 3

  def setName(name: String) = this.copy(name = Some(name))

  def setDescription(description: String) = this.copy(description = Some(description))

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
    //val response: String = readStream(is)
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

  //private def readStream(is: InputStream): String = fromInputStream(is).mkString
  /*private def readStream(is: InputStream): String = {
    var ch: Int = 0
    val sb: StringBuilder = new StringBuilder
    try
        while ((ch = is.read) != -1) sb.append(ch.toChar)

    catch {
      case e: IOException => {
        e.printStackTrace()
      }
    }

    return sb.toString
  }*/

  private def parseGetBinaryState(response: String): Boolean = {
    val field: String = "<BinaryState>"
    if (response.contains(field)) {
      val pos: Int = response.indexOf(field) + field.length
      "1" == response.substring(pos, pos + 1)
    } else throw new Exception("Invalid response for GetBinaryState")
  }
}

object Device {

  def apply(packet: SSDPPacket, deviceStr: String): Device = {
    val tagStart: String = "<deviceType>"
    val tagEnd: String = "</deviceType>"

    val posStart: Int = deviceStr.indexOf(tagStart)
    val posEnd = deviceStr.indexOf(tagEnd)

    if (posStart == -1 || posEnd == -1) null
    else {
      val deviceType: String = deviceStr.substring(posStart + tagStart.length(), posEnd)

      val rawUrl: URL = new URL(packet.location)
      val url: URL = new URL(rawUrl.getProtocol, rawUrl.getHost, rawUrl.getPort, "")
      val device: Device = new Device(url, deviceType, None, None)
      if (device.validType()) device
      else null
    }
  }

}