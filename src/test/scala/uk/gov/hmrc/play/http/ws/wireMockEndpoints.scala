/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
