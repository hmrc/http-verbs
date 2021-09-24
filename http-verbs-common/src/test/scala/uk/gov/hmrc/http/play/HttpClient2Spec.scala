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

package uk.gov.hmrc.http.play

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.functional.syntax._
import play.api.libs.json.{Reads, Writes}
import play.api.libs.ws.ahc.{AhcWSClient, AhcWSClientConfigFactory}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpReadsInstances, Retries, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.http.test.WireMockSupport

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Configuration
import java.util.concurrent.atomic.AtomicInteger

class HttpClient2Spec
  extends AnyWordSpecLike
     with Matchers
     with WireMockSupport
     with ScalaFutures
     with IntegrationPatience {

  implicit val as: ActorSystem = ActorSystem("test-actor-system")
  implicit val mat: Materializer = ActorMaterializer() // explicitly required for play-26

  // this would be injected
  val httpClient2: HttpClient2 = {
    val config =
      Configuration(
        ConfigFactory.parseString(
          """appName = myapp"""
        ).withFallback(ConfigFactory.load())
      )
    new HttpClient2Impl(
      wsClient = AhcWSClient(AhcWSClientConfigFactory.forConfig(config.underlying)),
      as,
      config,
      hooks = Seq.empty, // this would be wired up to play-auditing
    )
  }

  "api" when {
    "with json" should {
      "provide body with transformRequest" in {
        implicit val hc = HeaderCarrier()

        stubFor(
          WireMock.put(urlEqualTo("/"))
            .willReturn(aResponse().withBody("\"res\"").withStatus(200))
        )

        val res: Future[ResDomain] =
          httpClient2
            .put(url"$wireMockUrl/")
            .transformRequest(_.withBody(toJson(ReqDomain("req"))))
            .execute(fromJson[ResDomain])

        res.futureValue shouldBe ResDomain("res")

        verify(
          putRequestedFor(urlEqualTo("/"))
            .withRequestBody(equalTo("\"req\""))
            .withHeader("User-Agent", equalTo("myapp"))
        )
      }

      "provide body to put" in {
        implicit val hc = HeaderCarrier()

        stubFor(
          WireMock.put(urlEqualTo("/"))
            .willReturn(aResponse().withBody("\"res\"").withStatus(200))
        )

        val res: Future[ResDomain] =
          httpClient2
            .put(url"$wireMockUrl/", toJson(ReqDomain("req")))
            .execute(fromJson[ResDomain])

        res.futureValue shouldBe ResDomain("res")

        verify(
          putRequestedFor(urlEqualTo("/"))
            .withRequestBody(equalTo("\"req\""))
            .withHeader("User-Agent", equalTo("myapp"))
        )
      }

      "override user-agent" in {
        implicit val hc = HeaderCarrier()

        stubFor(
          WireMock.put(urlEqualTo("/"))
            .willReturn(aResponse().withBody("\"res\"").withStatus(200))
        )

        val res: Future[ResDomain] =
          httpClient2
            .put(url"$wireMockUrl/", toJson(ReqDomain("req")))
            .replaceHeader("User-Agent" -> "ua2")
            .execute(fromJson[ResDomain])

        res.futureValue shouldBe ResDomain("res")

        verify(
          putRequestedFor(urlEqualTo("/"))
            .withRequestBody(equalTo("\"req\""))
            .withHeader("User-Agent", equalTo("ua2"))
        )
      }
    }

    "with stream" should {
      "work" in {
        implicit val hc = HeaderCarrier()

        stubFor(
          WireMock.put(urlEqualTo("/"))
            .willReturn(aResponse().withBody("\"res\"").withStatus(200))
        )

        val srcStream: Source[ByteString, _] =
          Source.single(ByteString("source"))

        val res: Future[Source[ByteString, _]] =
          httpClient2
            .put(url"$wireMockUrl/")
            .transformRequest(_.withBody(srcStream))
            .stream(fromStream)

        res.futureValue.map(_.utf8String).runReduce(_ + _).futureValue shouldBe "\"res\""

        verify(
          putRequestedFor(urlEqualTo("/"))
            .withRequestBody(equalTo("source"))
            .withHeader("User-Agent", equalTo("myapp"))
        )
      }
    }

    "with custom retries" should {
      "work" in {
        val retries = new Retries {
          override val actorSystem   = as
          override val configuration = ConfigFactory
                                         .parseString("http-verbs.retries.intervals = [ 100.ms, 200.ms, 300.ms ]")
                                         .withFallback(ConfigFactory.load())
        }

        implicit val hc = HeaderCarrier()

        stubFor(
          WireMock.put(urlEqualTo("/"))
            .willReturn(aResponse().withBody("\"res\"").withStatus(502))
        )

        val count = new AtomicInteger(0)

        val res: Future[ResDomain] =
          retries.retryFor("get reqdomain"){ case UpstreamErrorResponse.WithStatusCode(502) => true }{
            count.incrementAndGet
            httpClient2
              .put(url"$wireMockUrl/", toJson(ReqDomain("req")))
              .execute(fromJson[ResDomain])
          }

        res.failed.futureValue shouldBe a[UpstreamErrorResponse]
        count.get shouldBe 4
      }
    }
  }
}

case class ReqDomain(
  field: String
)

object ReqDomain {
  implicit val w: Writes[ReqDomain] =
    implicitly[Writes[String]].contramap(_.field)
}

case class ResDomain(
  field: String
)

object ResDomain extends HttpReadsInstances {
  implicit val r: Reads[ResDomain] =
    implicitly[Reads[String]].map(ResDomain.apply)

  implicit val hr: HttpReads[ResDomain] = readFromJson[ResDomain]
}
