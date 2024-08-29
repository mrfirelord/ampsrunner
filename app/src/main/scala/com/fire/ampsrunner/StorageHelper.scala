package com.fire.ampsrunner

import java.io.{File, FileWriter}

/** Responsible for reading/writing information about instances to disk */
class StorageHelper {
  def writeAllInstances(instances: Set[AmpsInstance]): Unit = {
    val fileWriter = new FileWriter(new File(Config.runningInstancesInfoPath))

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
    if (!new File(Config.runningInstancesInfoPath).exists()) {
      return Seq()
    }

    val source = scala.io.Source.fromFile(Config.runningInstancesInfoPath)
    val lines = try source.mkString finally source.close()

    for {
      line <- lines.split("\n")
      if line.nonEmpty
    } yield AmpsInstance.fromString(line)
  }
}
