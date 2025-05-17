package com.fire.ampsrunner

import java.io.{File, FileWriter}

/** Reads/writes information about instances to disk */
class StorageHelper(config: ServerConfig) {
  def writeAllInstances(instances: Set[AmpsInstance]): Unit = {
    val fileWriter = new FileWriter(new File(config.runningInstancesInfoPath))

    try {
      for (instance <- instances) {
        fileWriter.write(instance.toString + "\n")
      }
    } catch {
      case e: Exception =>
        // todo: probably repeatable write would be good idea
        println(s"Error while trying to save information about instance on disk")
        e.printStackTrace()
    } finally {
      fileWriter.close()
    }
  }

  def readAllInstances(): Seq[AmpsInstance] = {
    if (!new File(config.runningInstancesInfoPath).exists()) {
      return Seq()
    }

    val source = scala.io.Source.fromFile(config.runningInstancesInfoPath)
    val lines = try source.mkString finally source.close()

    for {
      line <- lines.split("\n")
      if line.nonEmpty
    } yield AmpsInstance.fromString(line)
  }
}
