package services.wemo.ssdp

import java.net.DatagramPacket
import java.net.InetAddress

object WemoHTTPMsg {

	val NEWLINE: String = "\r\n"
	val SL_MSEARCH: String = "M-SEARCH * HTTP/1.1"
	val ADDRESS: String = "239.255.255.250"
  // val ADDRESS_IP6: String = "FF02::C"
	val PORT: Int = 1900
	val MAN: String = "\"ssdp:discover\""
	val MX: Int = 3 // Maximum time (seconds) to wait for the M-SEARCH response
  val ST: String = "upnp:rootdevice"

  lazy val searchRequest: DatagramPacket = {
    val msg: String = s"$SL_MSEARCH$NEWLINE" +
      s"HOST: $ADDRESS:$PORT$NEWLINE" +
      s"MAN: $MAN$NEWLINE" +
      s"MX: $MX$NEWLINE" +
      s"ST: $ST$NEWLINE$NEWLINE"

    val inetAddr: InetAddress = InetAddress.getByName(ADDRESS)
    new DatagramPacket(msg.toString.getBytes(), msg.length, inetAddr, PORT)
  }

  lazy val getState: Array[Byte] =
    ( s"""<?xml version="1.0" encoding="utf-8"?>$NEWLINE""" +
      s"""<s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">$NEWLINE""" +
      s"""   <s:Body>$NEWLINE""" +
      s"""      <u:GetBinaryState xmlns:u=\"urn:Belkin:service:basicevent:1\"></u:GetBinaryState>$NEWLINE""" +
      s"""   </s:Body>$NEWLINE""" +
      s"""</s:Envelope>$NEWLINE$NEWLINE"""
    ).getBytes("UTF-8")

  def setState(state: Boolean): Array[Byte] =
    ( s"""<?xml version="1.0" encoding="utf-8"?>$NEWLINE""" +
      s"""<s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">$NEWLINE""" +
      s"""   <s:Body>$NEWLINE""" +
      s"""      <u:SetBinaryState xmlns:u="urn:Belkin:service:basicevent:1">$NEWLINE""" +
      s"""         <BinaryState>${if (state) 1 else 0}</BinaryState>$NEWLINE""" +
      s"""         <Duration></Duration>$NEWLINE""" +
      s"""         <EndAction></EndAction>$NEWLINE""" +
      s"""         <UDN></UDN>$NEWLINE""" +
      s"""      </u:SetBinaryState>$NEWLINE""" +
      s"""   </s:Body>$NEWLINE""" +
      s"""</s:Envelope>$NEWLINE$NEWLINE"""
    ).getBytes("UTF-8")
}
