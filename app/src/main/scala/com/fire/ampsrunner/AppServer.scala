package com.fire.ampsrunner

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import io.javalin.Javalin
import io.javalin.http.{Context, Handler}

import java.util.UUID

object AppServer {
  private val START_URI = "/process/start"
  private val STOP_URI = "/process/stop"
  private val INSTANCES_INFO_URI = "/process/instances";

  private case class StartInstanceRequest(xmlInput: String, expireAfterInSeconds: Int)

  private case class StartInstanceResponse(xmlInput: String, secret: String, ports: Map[String, String])

  private case class StopInstanceRequest(secret: String)

  private val config = ServerConfig.instance
  private val processTracker = new ProcessTracker(config)
  private val objectMapper = new ObjectMapper().registerModule(DefaultScalaModule)
  private val configProcessor = new AmpsConfigProcessor()

  def main(args: Array[String]): Unit = {
    val app = Javalin.create().start(8080)

    app.post(START_URI, new Handler {
      override def handle(ctx: Context): Unit = {
        val body = ctx.body().replace("\n", "")

        val request = objectMapper.readValue(body, classOf[StartInstanceRequest])
        val clientIp = ctx.ip()
        val secret = UUID.randomUUID().toString
        val createdDirectory = config.buildDirectoryInstance(secret)

        val processedXml: ProcessedXml = configProcessor.process(request.xmlInput, createdDirectory.getAbsolutePath)
        processTracker.add(processedXml.xmlOutput, request.expireAfterInSeconds, clientIp, secret = secret)
        val response = StartInstanceResponse(xmlInput = processedXml.xmlOutput, secret = secret, processedXml.ports)
        ctx.result(objectMapper.writeValueAsString(response)).contentType("application/json")
      }
    })

    app.post(STOP_URI, new Handler {
      override def handle(ctx: Context): Unit = {
        val request = objectMapper.readValue(ctx.body(), classOf[StopInstanceRequest])
        try {
          processTracker.remove(request.secret)
          ctx.result(s"Process stopped for secret: ${request.secret}")
        } catch {
          case e: Exception =>
            ctx.status(500).result(s"Error stopping process: ${e.getMessage}")
            e.printStackTrace()
        }
      }
    })

    app.get(INSTANCES_INFO_URI, new Handler {
      override def handle(ctx: Context): Unit = {
        val instances = processTracker.instances().map(AmpsInstanceView.fromInstance)
        val json = objectMapper.writeValueAsString(instances)
        ctx.result(json).contentType("application/json")
      }
    })
  }
}
