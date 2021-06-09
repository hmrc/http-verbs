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
import org.scalatest.{BeforeAndAfterAll, Suite}

trait WireMockSupport extends BeforeAndAfterAll {
  this: Suite =>

  lazy val stubHost      : String         = "localhost"
  lazy val stubPort      : Int            = PortFinder.findFreePort(portRange = 6001 to 7000)
  lazy val wireMockServer: WireMockServer = new WireMockServer(wireMockConfig().port(stubPort))

  lazy val stubUrl = s"http://$stubHost:$stubPort"

  def startWriteMock(): Unit =
    if (!wireMockServer.isRunning) {
      wireMockServer.start()
      WireMock.configureFor(stubHost, stubPort)
    }

  def stopWireMock(): Unit =
    wireMockServer.stop()

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    startWriteMock()
  }

  override protected def afterAll(): Unit = {
    stopWireMock()
    super.afterAll()
  }
}
