package services.wemo.ssdp

import java.io.InputStream
import java.net._
import java.util.concurrent.Executors

import org.joda.time.{DateTime, DateTimeZone}
import play.api.Logger
import services.wemo.{WemoDevice, WemoService}

import collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.io.Source._

object SSDPFinder {
  val PORT: Int = 8008
  val TIMEOUT: Int = 6 * 1000
  val SOCKET_TIMEOUT: Int = 2 * 1000

  lazy val address: InetAddress = getHostAddress

  val pool = Executors.newFixedThreadPool(1)
  implicit val ec = ExecutionContext.fromExecutorService(pool)

  def request(wemoService: WemoService): Future[Seq[WemoDevice]] = {

    val promise = Promise[Seq[WemoDevice]]

    Future {

      var socket: DatagramSocket = null

      try {

        val startTime: Long = DateTime.now(DateTimeZone.UTC).getMillis

        // Start a server socket
        val bindInetAddr: InetSocketAddress = new InetSocketAddress(address, PORT)

        socket = new DatagramSocket(bindInetAddr)
        socket.setReuseAddress(true)
        socket.setSoTimeout(SOCKET_TIMEOUT)

        // Send search packet
        val searchPacket: DatagramPacket = WemoHTTPMsg.searchRequest
        socket.send(searchPacket)

        // Wait for responses
        val data: Array[Byte] = new Array[Byte](1024)
        val responsePacket: DatagramPacket = new DatagramPacket(data, 1024)

        val devices: ListBuffer[WemoDevice] = ListBuffer.empty[WemoDevice]

        while ((DateTime.now(DateTimeZone.UTC).getMillis - startTime) < TIMEOUT) {
          try {
            socket.receive(responsePacket)
          } catch {
            case e: Exception => //Logger.error(s"Error while waiting for SSDP responses: ${e.getMessage}")
          }

          if (SSDPPacket.validatePacket(responsePacket)) {
            getDevice(SSDPPacket(responsePacket)) match {
              case Some(rawDevice) =>
                val device = WemoDevice(rawDevice.id, None, rawDevice.baseUrl.toString, rawDevice.deviceType, 0)
                if (!wemoService.getServiceConf.devices.exists(_.serial == device.serial)) devices += device
              case None => //Logger.warn("SSDPFinder: not valid device")
            }
          }
        }

        promise.success(devices.toList)
      } catch {
        case e: Exception => promise.failure(e)
      } finally {
        if (socket != null) {
          socket.close()
        }
      }
    }

    promise.future
  }

  private def getHostAddress: InetAddress = {
    val networkInterfaces: Seq[NetworkInterface] = NetworkInterface.getNetworkInterfaces.asScala.toSeq
    val addrs = networkInterfaces.flatMap { networkInterface =>
      val addrs: Seq[InetAddress] = networkInterface.getInetAddresses.asScala.toSeq
      addrs.filterNot(addr => addr.isLoopbackAddress || addr.isInstanceOf[Inet6Address])
    }

    if (addrs.isEmpty) throw new Exception("SSDPFinder could not find a valid address to start a server")
    else addrs.head
  }

  private def getDevice(packet: SSDPPacket): Option[Device] = {
    if (!packet.isRootDevice) None
    else {
      try {
        val locationUrl: URL = new URL(packet.location)
        val deviceInfo: String = getDeviceInfo(locationUrl)
        val deviceUrl: URL = new URL(locationUrl.getProtocol, locationUrl.getHost, locationUrl.getPort, "")

        val tagStart: String = "<deviceType>"
        val tagEnd: String = "</deviceType>"

        val posStart: Int = deviceInfo.indexOf(tagStart)
        val posEnd: Int = deviceInfo.indexOf(tagEnd)

        if (posStart == -1 || posEnd == -1) None
        else {
          val deviceType: String = deviceInfo.substring(posStart + tagStart.length, posEnd)
          val device: Device = Device(packet.serialNumber, deviceUrl, deviceType)

          if (device.validType()) Some(device)
          else None
        }
      } catch {
        case e: Exception =>
          Logger.error("Error getting device from SSDPPacket: " + e.getMessage)
          None
      }
    }
  }

  private def getDeviceInfo(locationUrl: URL): String = {
    val connection: HttpURLConnection = locationUrl.openConnection.asInstanceOf[HttpURLConnection]

    connection.setRequestProperty("Content-Length", "0")
    connection.setRequestMethod("GET")
    if (locationUrl.getHost.nonEmpty) connection.setRequestProperty("HOST", locationUrl.getHost)

    val is: InputStream = connection.getInputStream
    val response: String = fromInputStream(is).mkString
    //val response: String = readStream(is)

    is.close()
    connection.disconnect()

    response
  }
}
