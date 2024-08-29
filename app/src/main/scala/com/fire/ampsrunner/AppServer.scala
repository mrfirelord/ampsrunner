package com.fire.ampsrunner

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import io.javalin.Javalin
import io.javalin.http.{Context, Handler}

import java.util.UUID

object AppServer {

  case class StartInstanceRequest(xmlInput: String, expireAfterInSeconds: Int)

  case class StartInstanceResponse(xmlInput: String, secret: String, ports: Map[String, String])

  case class StopInstanceRequest(secret: String)

  private val processTracker =
    new ProcessTracker(maxCapacityPerIp = Config.maxCapacityPerIp, totalCapacity = Config.totalCapacity)
  private val objectMapper = new ObjectMapper().registerModule(DefaultScalaModule)
  private val configProcessor = new AmpsConfigProcessor()

  def main(args: Array[String]): Unit = {
    val some = Some(null)
    println(some.nonEmpty)

//    val app = Javalin.create().start(8080)
//
//    app.post("/process/start", new Handler {
//      override def handle(ctx: Context): Unit = {
//        val body = ctx.body().replace("\n", "")
//
//        val request = objectMapper.readValue(body, classOf[StartInstanceRequest])
//        val clientIp = ctx.ip()
//        val secret = UUID.randomUUID().toString
//        val createdDirectory = Config.buildDirectoryInstance(secret)
//
//        val processedXml: ProcessedXml = configProcessor.process(request.xmlInput, createdDirectory.getAbsolutePath)
//        //        processTracker.add(processedXml.xmlOutput, request.expireAfterInSeconds, clientIp, secret = secret)
//        val response = StartInstanceResponse(xmlInput = processedXml.xmlOutput, secret = secret, processedXml.ports)
//        ctx.result(objectMapper.writeValueAsString(response)).contentType("application/json")
//      }
//    })
//
//    app.post("/process/stop", new Handler {
//      override def handle(ctx: Context): Unit = {
//        val request = objectMapper.readValue(ctx.body(), classOf[StopInstanceRequest])
//        try {
//          processTracker.remove(request.secret)
//          ctx.result(s"Process stopped for secret: ${request.secret}")
//        } catch {
//          case e: Exception =>
//            ctx.status(500).result(s"Error stopping process: ${e.getMessage}")
//            e.printStackTrace()
//        }
//      }
//    })
//
//    app.get("/process/instances", new Handler {
//      override def handle(ctx: Context): Unit = {
//        val instances = processTracker.instances().map(AmpsInstanceView.fromInstance)
//        val json = objectMapper.writeValueAsString(instances)
//        ctx.result(json).contentType("application/json")
//      }
//    })
  }
}
