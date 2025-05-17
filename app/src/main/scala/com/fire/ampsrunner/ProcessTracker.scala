package com.fire.ampsrunner

import java.io.File
import java.util
import java.util.concurrent.{Executors, ScheduledExecutorService}
import scala.collection.mutable

class ProcessTracker(config: ServerConfig) {
  private val storageHelper = new StorageHelper(config)

  private val processHandler = new AmpsProcessHandler(
    ampsExecFile = new File(config.ampsPath),
    processTracker = this,
    config = config
  )

  private var currentInstanceCapacity: Int = 0
  private val instanceBySecretKey: mutable.Map[String, AmpsInstance] = mutable.Map.empty
  private val instancesPerIp: mutable.Map[String, AmpsInstanceQueue] = mutable.Map.empty

  processHandler.checkExternallyKilledProcessesPeriodically()

  /** For terminating expired processes */
  private val expiredProcessRemover: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
  expiredProcessRemover.scheduleAtFixedRate(() => {
    val now = System.currentTimeMillis()
    instanceBySecretKey.values.filter(_.expirationTimestamp < now).foreach { instance =>
      remove(instance.secret)
    }
  }, 0, 5, java.util.concurrent.TimeUnit.SECONDS)

  private val existingInstances: Seq[AmpsInstance] = storageHelper.readAllInstances()
  for (instance <- existingInstances) {
    if (instance.expirationTimestamp > System.currentTimeMillis()) {
      if (processHandler.addExistingInstance(instance)) {
        if (instancesPerIp.contains(instance.ip)) {
          instancesPerIp(instance.ip).add(instance)
        } else {
          val queue = new AmpsInstanceQueue()
          queue.add(instance)
          instancesPerIp.put(instance.ip, queue)
        }

        instanceBySecretKey.put(instance.secret, instance)
        currentInstanceCapacity += 1
      }
    }
  }

  /** Starts AMPS instance based on xml config */
  def add(xmlInput: String, expireAfterInSeconds: Int, ip: String, secret: String): String = {
    if (currentInstanceCapacity >= config.totalCapacity)
      throw new IllegalStateException("Too many instances")

    val instanceQueue = instancesPerIp.getOrElseUpdate(ip, new AmpsInstanceQueue)
    if (instanceQueue.size() >= config.maxCapacityPerIp)
      throw new IllegalStateException("Too many instances")

    val pid = processHandler.startProcess(secretKey = secret, inputXml = xmlInput)
    val instance = AmpsInstance(
      pid = pid,
      secret = secret,
      ip = ip,
      startTimestamp = System.currentTimeMillis(),
      expirationTimestamp = System.currentTimeMillis() + expireAfterInSeconds * 1000
    )
    instanceQueue.add(instance)
    instanceBySecretKey(secret) = instance
    currentInstanceCapacity += 1

    storageHelper.writeAllInstances(instanceBySecretKey.values.toSet)
    secret
  }

  def remove(pid: Long): Unit = {
    instanceBySecretKey.values.find(instance => instance.pid == pid).foreach(instance => remove(instance.secret))
  }

  def remove(secret: String): Unit = {
    instanceBySecretKey.get(secret).foreach(instance => {
      processHandler.stopProcess(instance.pid)

      instanceBySecretKey.values.find(_.pid == instance.pid).foreach { instance =>
        currentInstanceCapacity -= 1
        instancesPerIp.get(instance.ip).foreach(_.remove(instance.secret))
        instanceBySecretKey.remove(instance.secret)
        storageHelper.writeAllInstances(instanceBySecretKey.values.toSet)
      }
    })
  }

  def instances(): List[AmpsInstance] = instanceBySecretKey.values.toList.sortBy(_.startTimestamp)
}

class AmpsInstanceQueue {
  private val queue = new util.LinkedList[AmpsInstance]()

  def add(instance: AmpsInstance): Unit = {
    queue.add(instance)
  }

  def remove(secret: String): Unit = {
    queue.removeIf(_.secret == secret)
  }

  def size(): Int = queue.size()
}

