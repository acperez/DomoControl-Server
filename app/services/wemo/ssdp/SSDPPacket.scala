package services.wemo.ssdp

import java.io.{LineNumberReader, StringReader}
import java.net.DatagramPacket

import play.Logger

import scala.collection.immutable.HashMap


case class SSDPPacket(NT: String, ST: String, USN: String, UDN: String, serialNumber: String, location: String) {
  val ROOTDEVICE: String = "upnp:rootdevice"
  val ROOTDEVICE2: String = "\"upnp:rootdevice\""
  val EVENT: String = "upnp:event"

  lazy val isRootDevice = {
    if ((NT.nonEmpty && NT.startsWith(ROOTDEVICE)) ||
      (ST.nonEmpty && (ST.equals(ROOTDEVICE) || ST.equals(ROOTDEVICE2))) ||
      (USN.nonEmpty && USN.startsWith(ROOTDEVICE))) true
    else false
  }
}

object SSDPPacket {

  def apply(dataPacket: DatagramPacket) = {
    val reader: LineNumberReader = getReader(dataPacket)
    val packet: Map[String, String] = readPacket(reader)

    val NT = packet.getOrElse("NT", "")
    val ST = packet.getOrElse("ST", "")
    val USN = packet.getOrElse("USN", "")
    val UDN = getUDN(USN)
    val serialNumber = getSerialNumber(UDN)
    val location = packet.getOrElse("LOCATION", "")
    new SSDPPacket(NT, ST, USN, UDN, serialNumber, location)
  }


  def validatePacket(dataPacket: DatagramPacket): Boolean = {
    val reader: LineNumberReader = getReader(dataPacket)
    readValidator(reader)
  }

  private def getReader(packet: DatagramPacket): LineNumberReader = {
    val data: String = new String(packet.getData, 0, packet.getLength)

    val strReader: StringReader = new StringReader(data)
    new LineNumberReader(strReader)
  }

  private def readPacket(reader: LineNumberReader, accum: Map[String, String] = new HashMap()): Map[String, String] = {
    val buffer: String = reader.readLine()
    if (buffer == null || buffer.length <= 0) accum
    else {
      headerReader(buffer) match {
        case None => readPacket(reader)
        case Some(header) =>
          readPacket(reader, accum + header)
      }
    }
  }

  private def readValidator(reader: LineNumberReader): Boolean = {
    val buffer: String = reader.readLine()
    if (buffer == null || buffer.length <= 0) false
    else {
      headerReader(buffer) match {
        case None => readValidator(reader)
        case Some(header) =>
          if (header._1.equals("USN") && (header._2.contains("Controlee") || header._2.contains("Socket") ||
            header._2.contains("Sensor") || header._2.contains("Lightswitch") ||
            header._2.contains("NetCamSensor") || header._2.contains("Insight"))) true
          else readValidator(reader)
      }
    }
  }

  private def headerReader(buffer: String): Option[(String, String)] = {
    val colonPos: Int = buffer.indexOf(58)
    if (colonPos < 0) None // Not the expected header eg HTTP/1.1 200 OK
    else {
      val name: String = new String(buffer.getBytes, 0, colonPos)
      val value: String = new String(buffer.getBytes, colonPos + 1, buffer.length - colonPos - 1)
      Some(name.trim.toUpperCase, value.trim)
    }
  }

  private def getUDN(USN: String): String = {
    if (USN.isEmpty) ""
    else {
      val idx: Int = USN.indexOf("::")
      if (idx < 0) USN.trim
      else new String(USN.getBytes(), 0, idx).trim
    }
  }

  private def getSerialNumber(UDN: String): String = {
    if (UDN.isEmpty) ""
    else UDN.substring(UDN.lastIndexOf("-") + 1, UDN.length())
  }

}

