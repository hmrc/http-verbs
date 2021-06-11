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

trait WireMockSupport extends BeforeAndAfterAll with BeforeAndAfterEach {
  this: Suite =>

  private val logger: Logger = LoggerFactory.getLogger(getClass)

  lazy val wireMockHost: String  =
    // this has to match the configuration in `internalServiceHostPatterns`
    "localhost"
  lazy val wireMockPort: Int =
    // we lookup a port ourselves rather than using `wireMockConfig().dynamicPort()` since it's simpler to provide
    // it up front (rather than query the running server), and allow overriding.
    PortFinder.findFreePort(portRange = 6001 to 7000)

  lazy val wireMockRootDirectory: String =
    // wiremock doesn't look in the classpath, it uses src/test/resources by default.
    // since play projects use the non-standard `test/resources` we should attempt to identify the path
    java.lang.ClassLoader.getSystemClassLoader.asInstanceOf[java.net.URLClassLoader]
      .getURLs.head.getPath

  lazy val wireMockServer: WireMockServer =
    new WireMockServer(
      wireMockConfig()
        .port(wireMockPort)
        .withRootDirectory(wireMockRootDirectory)
    )

  lazy val wireMockUrl: String =
    s"http://$wireMockHost:$wireMockPort"

  /** If true (default) it will clear the wireMock settings before each test */
  lazy val resetWireMockMappings: Boolean = true

  def startWireMock(): Unit =
    if (!wireMockServer.isRunning) {
      wireMockServer.start()
      // this initialises static access to `WireMock` rather than calling functions on the wireMockServer instance itself
      WireMock.configureFor(wireMockHost, wireMockServer.port())
      logger.info(s"Started WireMock server on host: $wireMockHost, port: ${wireMockServer.port()}, rootDirectory: $wireMockRootDirectory")
    }

  def stopWireMock(): Unit =
    if (wireMockServer.isRunning) {
      wireMockServer.stop()
      logger.info(s"Stopped WireMock server on host: $wireMockHost, port: $wireMockPort")
    }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    startWireMock()
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    if (resetWireMockMappings)
      wireMockServer.resetMappings()
  }

  override protected def afterAll(): Unit = {
    stopWireMock()
    super.afterAll()
  }
}
