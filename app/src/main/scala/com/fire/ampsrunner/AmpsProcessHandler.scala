package com.fire.ampsrunner

import java.io.{File, PrintWriter}
import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}
import scala.collection.mutable
import scala.jdk.OptionConverters.RichOptional

/** System process manager */
class AmpsProcessHandler(private val ampsExecFile: File,
                         private val processTracker: ProcessTracker) {
  private val processByPid: mutable.Map[Long, ProcessHandle] = mutable.Map.empty
  /** pid -> directory for amps instance */
  private val createdDirectoryByPid: mutable.Map[Long, File] = mutable.Map.empty

  /** Returns true if the process exists and can be tracked. False otherwise. */
  def addExistingInstance(instance: AmpsInstance): Boolean = {
    ProcessHandle.of(instance.pid).map { processHandle =>
      processByPid(instance.pid) = processHandle
      createdDirectoryByPid(instance.pid) = Config.buildDirectoryInstance(instance.secret)
      true
    }.toScala.getOrElse(false)
  }

  /** Runs system process and returns PID */
  def startProcess(secretKey: String, inputXml: String): Long = {
    val instanceDirectory: File = createInstanceDirectory(secretKey)

    try {
      val ampsConfigFile: File = writeTextToFile(instanceDirectory, inputXml)
      val ampsProcess: Process = runAmpsProcess(instanceDirectory, ampsConfigFile)

      processByPid(ampsProcess.pid()) = ampsProcess.toHandle
      createdDirectoryByPid(ampsProcess.pid()) = instanceDirectory
      println(s"Amps started with PID: ${ampsProcess.pid()}")
      ampsProcess.pid()
    } catch {
      case e: Exception =>
        deleteDirectory(instanceDirectory)
        throw e
    }
  }

  private def createInstanceDirectory(secretKey: String) = {
    val directory = Config.buildDirectoryInstance(secretKey)
    try {
      directory.mkdirs()
      directory
    } catch {
      case e: Exception =>
        deleteDirectory(directory)
        throw new RuntimeException("Can not create directory for process", e)
    }
  }

  private def writeTextToFile(instanceDirectory: File, text: String): File = {
    val ampsConfigFile = new File(instanceDirectory, "amps.xml")
    val writer = new PrintWriter(ampsConfigFile)
    try {
      writer.write(text)
      ampsConfigFile
    }
    finally {
      ampsConfigFile.delete()
      writer.close()
    }
  }

  private def runAmpsProcess(instanceDirectory: File, ampsConfigFile: File) = {
    val processBuilder = new ProcessBuilder(ampsExecFile.getAbsolutePath, ampsConfigFile.getAbsolutePath)
    val logFile = new File(instanceDirectory, "amps.log")
    processBuilder.redirectOutput(logFile)
    processBuilder.redirectError(logFile)
    val process = processBuilder.start()
    process
  }

  def stopProcess(pid: Long): Unit = {
    val processHandle = processByPid.getOrElse(pid, return)

    try {
      processHandle.destroy()

      val stillAlive = processHandle.onExit().get(5, TimeUnit.SECONDS).isAlive
      if (stillAlive) {
        processHandle.destroyForcibly()
        println(s"Forcibly terminated AMPS with PID: $pid")
      } else {
        println(s"AMPS with PID: $pid terminated successfully")
      }
    } catch {
      case e: Exception =>
        println(s"Error while stopping AMPS with PID: $pid")
        e.printStackTrace()
    } finally {
      processByPid -= pid
      createdDirectoryByPid.get(pid).foreach(deleteDirectory)
    }
  }

  /** For checking if processes are alive */
  private val processMonitorScheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1)

  def checkExternallyKilledProcessesPeriodically(): Unit = {
    processMonitorScheduler.scheduleAtFixedRate(() => {
      val externallyStoppedProcessPids = processByPid.filterNot { case (_, handle) => handle.isAlive }.keys
      if (externallyStoppedProcessPids.nonEmpty) {
        println(s"Process with PIDs $externallyStoppedProcessPids were terminated externally.")
        externallyStoppedProcessPids.foreach { pid =>
          try {
            processTracker.remove(pid)
          } catch {
            case e: Exception =>
              println(s"Error while stopping AMPS with PID: $pid")
              e.printStackTrace()
          }
        }
      }
    }, 0, 10, TimeUnit.SECONDS)
  }

  private def deleteDirectory(directory: File): Unit = {
    if (directory.exists) {
      for (file <- Option(directory.listFiles).getOrElse(Array.empty)) {
        if (file.isDirectory)
          deleteDirectory(file)
        else
          file.delete
      }
      directory.delete
    }
  }
}
