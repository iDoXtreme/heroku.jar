package com.heroku.api.connection

import java.lang.String
import com.heroku.api.http._
import com.twitter.finagle.Service
import org.jboss.netty.handler.codec.http._
import com.heroku.api.command.{LoginCommand, Command}
import collection.JavaConversions._
import java.net.{InetSocketAddress, URL}
import com.twitter.finagle.http.Http
import org.jboss.netty.buffer.ChannelBuffers
import com.twitter.util.{Base64StringEncoder, Future}
import java.nio.charset.Charset
import com.twitter.finagle.builder.ClientBuilder
import com.heroku.api.http.Http.Method
import com.heroku.api.Heroku.ApiVersion
import com.heroku.api.Heroku


class FinagleConnection(val config: Either[LoginCommand, String]) extends Connection[Future[_]] {

  type HttpService = Service[HttpRequest, HttpResponse]

  val client = newClient()

  val hostHeader = getHostHeader

  val apiKey = config match {
    case Left(login) => executeCommand(login).api_key()
    case Right(key) => key
  }

  def executeCommand[T](command: Command[T]): T = executeCommandAsync(command).get()

  def executeCommandAsync[T](command: Command[T]): Future[T] = {
    client(toReq(command)).map {
      resp =>
        command.getResponse(resp.getContent.toByteBuffer.array(), resp.getStatus.getCode)
    }
  }

  def toReq(cmd: Command[_]): HttpRequest = {
    val method = cmd.getHttpMethod match {
      case Method.GET => HttpMethod.GET
      case Method.PUT => HttpMethod.PUT
      case Method.POST => HttpMethod.POST
      case Method.DELETE => HttpMethod.DELETE
    }
    val req = new DefaultHttpRequest(HttpVersion.HTTP_1_1, method, cmd.getEndpoint)
    req.addHeader(cmd.getResponseType.getHeaderName, cmd.getResponseType.getHeaderValue)
    req.addHeader(ApiVersion.HEADER, ApiVersion.v2.getHeaderValue)
    req.addHeader(HttpHeaders.Names.HOST, hostHeader)

    if (apiKey != null) {
      req.addHeader(HttpHeaders.Names.AUTHORIZATION, "Basic " + Base64StringEncoder.encode((":" + apiKey).getBytes("UTF-8")))
    }

    cmd.getHeaders.foreach {
      _ match {
        case (k, v) => req.addHeader(k, v)
      }
    }
    if (cmd.hasBody) {
      val body = ChannelBuffers.copiedBuffer(cmd.getBody, Charset.forName("UTF-8"))
      req.setContent(body)
      req.setHeader(HttpHeaders.Names.CONTENT_LENGTH, body.readableBytes().toString)
    }

    req
  }

  def getPort(url: URL): Int = {
    if (url.getPort == -1) url.getDefaultPort
    else url.getPort
  }

  def getHostHeader: String = {
    val url = HttpUtil.toURL(Heroku.Config.ENDPOINT.value)
    val host = url.getHost
    var port = ""
    if (url.getPort != url.getDefaultPort) port = ":" + url.getPort.toString
    host + port
  }

  def newClient(): HttpService = {
    val endpoint = HttpUtil.toURL(Heroku.Config.ENDPOINT.value)
    var builder = ClientBuilder()
      .codec(Http())
      .hosts(new InetSocketAddress(endpoint.getHost, getPort(endpoint)))
      .hostConnectionLimit(10)
    if (endpoint.getProtocol.equals("https")) {
      if (Heroku.Config.ENDPOINT.isDefault) {
        builder = builder.tls(endpoint.getHost)
      } else {
        builder = builder.tlsWithoutValidation()
      }
    }
    builder.build()
  }

  def close() {
    client.release()
  }
}


object FinagleConnection {
  def apply(cmd: LoginCommand): FinagleConnection = {
    new FinagleConnection(Left(cmd))
  }

  def apply(apiKey: String): FinagleConnection = {
    new FinagleConnection(Right(apiKey))
  }

}