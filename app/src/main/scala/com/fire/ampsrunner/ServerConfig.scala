package com.fire.ampsrunner

import com.typesafe.config.{Config, ConfigFactory}

import java.io.File
import java.time.Duration

case class ServerConfig(ampsPath: String,
                        tempDirectory: String,
                        maxCapacityPerIp: Int,
                        totalCapacity: Int,
                        runningInstancesInfoPath: String,
                        serverPort: Int,
                        ampsServerStoppingTimeout: Duration) {
  def buildDirectoryInstance(secret: String): File = new File(s"$tempDirectory/$secret")
}

object ServerConfig {
  private val appConfig: Config = ConfigFactory.load().getConfig("application")
  val instance: ServerConfig = ServerConfig(
    ampsPath = appConfig.getString("ampsPath"),
    tempDirectory = appConfig.getString("tempDirectory"),
    maxCapacityPerIp = appConfig.getInt("maxCapacityPerIp"),
    totalCapacity = appConfig.getInt("totalCapacity"),
    runningInstancesInfoPath = appConfig.getString("runningInstancesInfoPath"),
    serverPort = appConfig.getInt("serverPort"),
    ampsServerStoppingTimeout = appConfig.getDuration("ampsServerStoppingTimeout")
  )
}
