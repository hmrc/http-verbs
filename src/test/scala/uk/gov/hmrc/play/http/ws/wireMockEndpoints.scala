package uk.gov.hmrc.play.http.ws

import java.net.ServerSocket

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._

import scala.util.Try


trait WireMockEndpoints {

  val host: String = "localhost"

  val endpointPort: Int = PortTester.findPort()
  val endpointMock = new WireMock(host, endpointPort)
  val endpointServer: WireMockServer = new WireMockServer(wireMockConfig().port(endpointPort))

  val proxyPort: Int = PortTester.findPort(endpointPort)
  val proxyMock: WireMock = new WireMock(host, proxyPort)
  val proxyServer: WireMockServer = new WireMockServer(wireMockConfig().port(proxyPort))

  def withServers(test: => Unit) {
    endpointServer.start()
    proxyServer.start()

    try {
      test
    } finally {
      Try(endpointServer.stop())
      Try(proxyServer.stop())
    }
  }
}

object PortTester {

  def findPort(excluded: Int*): Int = {
    (6000 to 7000).find(port => !excluded.contains(port) && isFree(port)).getOrElse(throw new Exception("No free port"))
  }

  private def isFree(port: Int): Boolean = {
    val triedSocket = Try {
      val serverSocket = new ServerSocket(port)
      Try(serverSocket.close())
      serverSocket
    }
    triedSocket.isSuccess
  }
}