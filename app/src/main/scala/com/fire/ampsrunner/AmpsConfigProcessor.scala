package com.fire.ampsrunner

import java.net.ServerSocket
import scala.collection.mutable
import scala.xml.NodeSeq

class AmpsConfigProcessor {
  /** Replaces template ports and paths in xml template with real ones */
  def process(xmlInput: String, instanceDirectory: String): ProcessedXml = {
    val xml: NodeSeq = scala.xml.XML.loadString(xmlInput)

    val error: Option[String] = checkConfig(xml)
    if (error.isDefined) {
      throw new IllegalArgumentException(error.get)
    }

    val ports: mutable.Map[String, String] = mutable.Map()
    var resultXml = xmlInput

    (xml \\ "InetAddress").map(_.text.trim).foreach(address => {
      val availablePort = findAvailablePort().toString
      ports += (address -> availablePort)
      resultXml = resultXml.replace(address, availablePort)
    })

    ProcessedXml(resultXml.replace("$instanceDir", instanceDirectory), ports.toMap)
  }

  private def checkConfig(xml: NodeSeq): Option[String] = {
    val inetAddresses = (xml \\ "InetAddress").map(_.text.trim)
    if (inetAddresses.size != inetAddresses.toSet.size) {
      return Some("InetAddress should be unique")
    }

    val notProperlyNamedPort = inetAddresses
      .find(text => !(text.startsWith("$") && text.endsWith("Port") && !text.contains(" ")))

    if (notProperlyNamedPort.isDefined) {
      return Some("Port name should start with $ and end with Port and not contain space: " + notProperlyNamedPort.get)
    }

    val fileNames = (xml \\ "FileName").map(_.text.trim) ++
      (xml \\ "JournalDirectory").map(_.text.trim) ++
      (xml \\ "JournalArchiveDirectory").map(_.text.trim)

    fileNames.find(text => !text.startsWith("$instanceDir"))
      .map(fileName => "FileName should start with $instanceDir: " + fileName)
  }

  private def findAvailablePort(): Int = {
    val socket = new ServerSocket(0)
    try {
      socket.getLocalPort
    } finally {
      socket.close()
    }
  }
}
