package com.fire.ampsrunner

import java.io.File

object Config {
  val ampsPath = "/Users/alimagomedov/dev/amps/fake-amps.sh"
  val tempDirectory = "/Users/alimagomedov/tmp/amps_instances/"
  val maxCapacityPerIp = 5
  val totalCapacity = 10
  val runningInstancesInfoPath = "/Users/alimagomedov/tmp/amps_instances/running_instances.txt"

  def buildDirectoryInstance(secret: String): File = new File(s"$tempDirectory/$secret")
}
