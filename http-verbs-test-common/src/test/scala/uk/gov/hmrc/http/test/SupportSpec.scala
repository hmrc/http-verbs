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

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.concurrent.ExecutionContext.Implicits.global

class SupportSpec
  extends AnyWordSpecLike
     with ScalaFutures
     with IntegrationPatience
     with HttpClientSupport
     with WireMockSupport
     with ExternalWireMockSupport
     with Matchers {

  "WireMockSupport" should {
    "allow the user to simulate internal calls" in {
      implicit val hc: HeaderCarrier = HeaderCarrier(authorization = Some(Authorization("Basic dXNlcjoxMjM=")))

      wireMockServer.stubFor(
        post(urlEqualTo("/create-user"))
          .willReturn(aResponse().withStatus(200))
      )

      // auth header is forwarded on for internal calls
      httpClient.POST[String, HttpResponse](
        url     = url"$stubUrl/create-user",
        body    = "body",
        headers = Seq.empty
      ).futureValue.status shouldBe 200

      wireMockServer.verify(
        postRequestedFor(urlEqualTo("/create-user"))
          .withHeader("Authorization", equalTo("Basic dXNlcjoxMjM="))
      )
    }
  }

  "ExternalWireMockSupport" should {
    "allow the user to simulate external calls" in {
      implicit val hc: HeaderCarrier = HeaderCarrier(authorization = Some(Authorization("Basic dXNlcjoxMjM=")))

      externalWireMockServer.stubFor(
        post(urlEqualTo("/create-user"))
          .willReturn(aResponse().withStatus(200))
      )

      // auth header is *not* forwarded on for external calls
      httpClient.POST[String, HttpResponse](
        url     = url"$externalStubUrl/create-user",
        body    = "body",
        headers = Seq.empty
      ).futureValue.status shouldBe 200

      externalWireMockServer.verify(
        postRequestedFor(urlEqualTo("/create-user"))
          .withoutHeader("Authorization")
      )

      // auth header can be forwarded explicitly
      httpClient.POST[String, HttpResponse](
        url     = url"$externalStubUrl/create-user",
        body    = "body",
        headers = hc.headers(Seq(hc.names.authorisation))
      ).futureValue.status shouldBe 200

      externalWireMockServer.verify(
        postRequestedFor(urlEqualTo("/create-user"))
          .withHeader("Authorization", equalTo("Basic dXNlcjoxMjM="))
      )
    }
  }
}
