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

package uk.gov.hmrc.http.examples.utils

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpecLike


trait WiremockTestServer extends AnyWordSpecLike with BeforeAndAfterEach {

  val wireMockServer = new WireMockServer(20001)

  override protected def beforeEach(): Unit = {
    wireMockServer.start()
    WireMock.configureFor("localhost", 20001)
  }

  override protected def afterEach(): Unit = {
    wireMockServer.stop()
  }
}
