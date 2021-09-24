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

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import com.github.tomakehurst.wiremock._
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.slf4j.{Logger, LoggerFactory}

trait WireMockSupport
  extends BeforeAndAfterAll
     with BeforeAndAfterEach {
  this: Suite =>

  private val logger: Logger = LoggerFactory.getLogger(getClass)

  lazy val wireMockHost: String  =
    "localhost"

  lazy val wireMockPort: Int =
    // we lookup a port ourselves rather than using `wireMockConfig().dynamicPort()` since it's simpler to provide
    // it up front (rather than query the running server), and allow overriding.
    PortFinder.findFreePort(portRange = 6001 to 7000)

  lazy val wireMockUrl: String =
    s"http://$wireMockHost:$wireMockPort"

  lazy val wireMockServer =
    new WireMockServer(WireMockConfiguration.wireMockConfig().port(wireMockPort))

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    wireMockServer.start()
    WireMock.configureFor(wireMockHost, wireMockServer.port())
    logger.info(s"Started WireMock server on host: $wireMockHost, port: ${wireMockServer.port()}")
  }

  override protected def afterAll(): Unit = {
    wireMockServer.stop()
    super.afterAll()
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    wireMockServer.resetMappings()
  }
}
