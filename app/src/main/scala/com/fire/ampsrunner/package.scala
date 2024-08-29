package com.fire

import java.time.Instant

package object ampsrunner {
  case class AmpsInstance(pid: Long,
                          secret: String,
                          ip: String,
                          startTimestamp: Long,
                          expirationTimestamp: Long) {
    override def toString: String =
      s"pid=$pid;ip=$ip;secret=$secret;startTimestamp=$startTimestamp;expirationTimestamp=$expirationTimestamp"

    private def isValid: Boolean = pid > 0 && secret.nonEmpty && ip.nonEmpty && startTimestamp > 0 && expirationTimestamp > 0
  }

  object AmpsInstance {
    def fromString(line: String): AmpsInstance = {
      val fields = line.split(';')
      var instance = AmpsInstance(pid = 0, secret = "", ip = "", startTimestamp = 0L, expirationTimestamp = 0L)

      for (field <- fields) {
        val keyValue = field.split("=")
        val key = keyValue(0)
        val value = keyValue(1)
        key match {
          case "pid" => instance = instance.copy(pid = value.toLong)
          case "secret" => instance = instance.copy(secret = value)
          case "ip" => instance = instance.copy(ip = value)
          case "startTimestamp" => instance = instance.copy(startTimestamp = value.toLong)
          case "expirationTimestamp" => instance = instance.copy(expirationTimestamp = value.toLong)
          case _ => throw new IllegalArgumentException(s"Unknown field: $key")
        }
      }

      if (instance.isValid)
        instance
      else
        throw new IllegalArgumentException(s"Invalid instance $line")
    }
  }

  case class AmpsInstanceView(pid: Long, ip: String, startTime: String, expirationTime: String)

  object AmpsInstanceView {
    def fromInstance(instance: AmpsInstance): AmpsInstanceView = {
      new AmpsInstanceView(
        pid = instance.pid,
        ip = instance.ip,
        startTime = Instant.ofEpochMilli(instance.startTimestamp).toString,
        expirationTime = Instant.ofEpochMilli(instance.expirationTimestamp).toString
      )
    }
  }

  case class ProcessedXml(xmlOutput: String, ports: Map[String, String])
}
