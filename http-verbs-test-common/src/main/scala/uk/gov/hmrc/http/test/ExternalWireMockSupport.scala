/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.http.test

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import org.slf4j.{Logger, LoggerFactory}

trait ExternalWireMockSupport extends BeforeAndAfterAll with BeforeAndAfterEach {
  this: Suite =>

  private val logger: Logger = LoggerFactory.getLogger(getClass)

   // "127.0.0.1" allows us to test locally, but is considered an external host by http-verbs since only "localhost" is
   // registered in `internalServiceHostPatterns` as an internal host.
  lazy val externalWireMockHost  : String         = "127.0.0.1"
  // we lookup a port ourselves rather than using `wireMockConfig().dynamicPort()` since it's simpler to provide
  // it up front (rather than query the running server), and allow overriding.
  lazy val externalWireMockPort  : Int            = PortFinder.findFreePort(portRange = 6001 to 7000)
  lazy val externalWireMockServer: WireMockServer = new WireMockServer(wireMockConfig().port(externalWireMockPort))
  lazy val externalWireMockUrl   : String         = s"http://$externalWireMockHost:$externalWireMockPort"

  /** If true (default) it will clear the wireMock settings before each test */
  lazy val resetExternalWireMockMappings: Boolean = true

  def startExternalWireMock(): Unit =
    if (!externalWireMockServer.isRunning) {
      externalWireMockServer.start()
      // Note, if we use `ExternalWireMockSupport` in addition to `WireMockSupport`, then we can't use WireMock statically
      // since it's ambiguous.
      WireMock.configureFor(externalWireMockHost, externalWireMockServer.port())
      logger.info(s"Started external WireMock server on host: $externalWireMockHost, port: ${externalWireMockServer.port()}")
    }

  def stopExternalWireMock(): Unit =
    if (externalWireMockServer.isRunning) {
      externalWireMockServer.stop()
      logger.info(s"Stopped external WireMock server on host: $externalWireMockHost, port: $externalWireMockPort")
    }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    startExternalWireMock()
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    if (resetExternalWireMockMappings)
      externalWireMockServer.resetMappings()
  }

  override protected def afterAll(): Unit = {
    stopExternalWireMock()
    super.afterAll()
  }
}
