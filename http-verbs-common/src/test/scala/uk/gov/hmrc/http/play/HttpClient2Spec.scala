/*
 * Copyright 2022 HM Revenue & Customs
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
import com.github.tomakehurst.wiremock.client.WireMock.{verify => _, _}
import com.typesafe.config.ConfigFactory
import org.mockito.ArgumentMatchersSugar
import org.mockito.captor.ArgCaptor
import org.mockito.scalatest.MockitoSugar
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.Configuration
import play.api.libs.functional.syntax._
import play.api.libs.json.{Json, Reads, Writes}
import play.api.libs.ws.ahc.{AhcWSClient, AhcWSClientConfigFactory}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpReadsInstances, HttpResponse, Retries, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.http.hooks.{HookData, HttpHook}
import uk.gov.hmrc.http.test.WireMockSupport

import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

class HttpClient2Spec
  extends AnyWordSpecLike
     with Matchers
     with WireMockSupport
     with ScalaFutures
     with IntegrationPatience
     with MockitoSugar
     with ArgumentMatchersSugar {

  "HttpClient2" should {
    "work with json" in new Setup {
      implicit val hc = HeaderCarrier()

      wireMockServer.stubFor(
        WireMock.put(urlEqualTo("/"))
          .willReturn(aResponse().withBody("\"res\"").withStatus(200))
      )

      val res: Future[ResDomain] =
        httpClient2
          .put(url"$wireMockUrl/")
          .withBody(Json.toJson(ReqDomain("req")))
          .execute(fromJson[ResDomain])

      res.futureValue shouldBe ResDomain("res")

      wireMockServer.verify(
        putRequestedFor(urlEqualTo("/"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalTo("\"req\""))
          .withHeader("User-Agent", equalTo("myapp"))
      )

      val headersCaptor  = ArgCaptor[Seq[(String, String)]]
      val responseCaptor = ArgCaptor[Future[HttpResponse]]

      verify(mockHttpHook)
        .apply(
          verb      = eqTo("PUT"),
          url       = eqTo(url"$wireMockUrl/"),
          headers   = headersCaptor,
          body      = eqTo(Some(HookData.FromString("\"req\""))),
          responseF = responseCaptor
        )(any[HeaderCarrier], any[ExecutionContext])

      headersCaptor.value should contain ("User-Agent" -> "myapp")
      headersCaptor.value should contain ("Content-Type" -> "application/json")
      val auditedResponse = responseCaptor.value.futureValue
      auditedResponse.status shouldBe 200
      auditedResponse.body   shouldBe "\"res\""
    }

    "work with streams" in new Setup {
      implicit val hc = HeaderCarrier()

      val requestBody  = Random.alphanumeric.take(maxAuditBodyLength - 1).mkString
      val responseBody = Random.alphanumeric.take(maxAuditBodyLength - 1).mkString

      wireMockServer.stubFor(
        WireMock.put(urlEqualTo("/"))
          .willReturn(aResponse().withBody(responseBody).withStatus(200))
      )

      val srcStream: Source[ByteString, _] =
        Source.single(ByteString(requestBody))

      val res: Future[Source[ByteString, _]] =
        httpClient2
          .put(url"$wireMockUrl/")
          .withBody(srcStream)
          .stream(fromStream)

      res.futureValue.map(_.utf8String).runReduce(_ + _).futureValue shouldBe responseBody

      wireMockServer.verify(
        putRequestedFor(urlEqualTo("/"))
          .withHeader("Content-Type", equalTo("application/octet-stream"))
          .withRequestBody(equalTo(requestBody))
          .withHeader("User-Agent", equalTo("myapp"))
      )

      val headersCaptor  = ArgCaptor[Seq[(String, String)]]
      val responseCaptor = ArgCaptor[Future[HttpResponse]]

      verify(mockHttpHook)
        .apply(
          verb      = eqTo("PUT"),
          url       = eqTo(url"$wireMockUrl/"),
          headers   = headersCaptor,
          body      = eqTo(Some(HookData.FromString(requestBody))),
          responseF = responseCaptor
        )(any[HeaderCarrier], any[ExecutionContext])

      headersCaptor.value should contain ("User-Agent" -> "myapp")
      headersCaptor.value should contain ("Content-Type" -> "application/octet-stream")
      val auditedResponse = responseCaptor.value.futureValue
      auditedResponse.status shouldBe 200
      auditedResponse.body   shouldBe responseBody
    }

    "truncate stream payloads if too long" in new Setup {
      implicit val hc = HeaderCarrier()

      val requestBody  = Random.alphanumeric.take(maxAuditBodyLength * 2).mkString
      val responseBody = Random.alphanumeric.take(maxAuditBodyLength * 2).mkString

      wireMockServer.stubFor(
        WireMock.put(urlEqualTo("/"))
          .willReturn(aResponse().withBody(responseBody).withStatus(200))
      )

      val srcStream: Source[ByteString, _] =
        Source.single(ByteString(requestBody))

      val res: Future[Source[ByteString, _]] =
        httpClient2
          .put(url"$wireMockUrl/")
          .withBody(srcStream)
          .stream(fromStream)

      res.futureValue.map(_.utf8String).runReduce(_ + _).futureValue shouldBe responseBody

      wireMockServer.verify(
        putRequestedFor(urlEqualTo("/"))
          .withHeader("Content-Type", equalTo("application/octet-stream"))
          .withRequestBody(equalTo(requestBody))
          .withHeader("User-Agent", equalTo("myapp"))
      )

      val headersCaptor  = ArgCaptor[Seq[(String, String)]]
      val responseCaptor = ArgCaptor[Future[HttpResponse]]

      verify(mockHttpHook)
        .apply(
          verb      = eqTo("PUT"),
          url       = eqTo(url"$wireMockUrl/"),
          headers   = headersCaptor,
          body      = eqTo(Some(HookData.FromString(requestBody.take(maxAuditBodyLength)))),
          responseF = responseCaptor
        )(any[HeaderCarrier], any[ExecutionContext])

      headersCaptor.value should contain ("User-Agent" -> "myapp")
      headersCaptor.value should contain ("Content-Type" -> "application/octet-stream")
      val auditedResponse = responseCaptor.value.futureValue
      auditedResponse.status shouldBe 200
      auditedResponse.body   shouldBe responseBody.take(maxAuditBodyLength)
    }

    "work with form data" in new Setup {
      implicit val hc = HeaderCarrier()

      wireMockServer.stubFor(
        WireMock.post(urlEqualTo("/"))
          .willReturn(aResponse().withBody("\"res\"").withStatus(200))
      )

      val body: Map[String, Seq[String]] =
        Map(
          "k1" -> Seq("v1", "v2"),
          "k2" -> Seq("v3")
        )

      val res: Future[ResDomain] =
          httpClient2
            .post(url"$wireMockUrl/")
            .withBody(body)
            .execute(fromJson[ResDomain])

      res.futureValue shouldBe ResDomain("res")

      wireMockServer.verify(
        postRequestedFor(urlEqualTo("/"))
          .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded"))
          .withRequestBody(equalTo("k1=v1&k1=v2&k2=v3"))
          .withHeader("User-Agent", equalTo("myapp"))
      )

      val headersCaptor  = ArgCaptor[Seq[(String, String)]]
      val responseCaptor = ArgCaptor[Future[HttpResponse]]

      verify(mockHttpHook)
        .apply(
          verb      = eqTo("POST"),
          url       = eqTo(url"$wireMockUrl/"),
          headers   = headersCaptor,
          body      = eqTo(Some(HookData.FromMap(body))),
          responseF = responseCaptor
        )(any[HeaderCarrier], any[ExecutionContext])

      headersCaptor.value should contain ("User-Agent" -> "myapp")
      headersCaptor.value should contain ("Content-Type" -> "application/x-www-form-urlencoded")
      val auditedResponse = responseCaptor.value.futureValue
      auditedResponse.status shouldBe 200
      auditedResponse.body   shouldBe "\"res\""
    }

    "work with form data - custom writeable for content-type" in new Setup {
      implicit val hc = HeaderCarrier()

      wireMockServer.stubFor(
        WireMock.post(urlEqualTo("/"))
          .willReturn(aResponse().withBody("\"res\"").withStatus(200))
      )

      import play.api.libs.ws.{BodyWritable, InMemoryBody}
      implicit val writeableOf_urlEncodedForm: BodyWritable[Map[String, Seq[String]]] = {
        import java.net.URLEncoder
        BodyWritable(
          formData =>
            InMemoryBody(
              ByteString.fromString(
                formData.flatMap(item => item._2.map(c => s"${item._1}=${URLEncoder.encode(c, "UTF-8")}")).mkString("&")
              )
            ),
          "nonstandard/x-www-form-urlencoded"
        )
      }

      val body: Map[String, Seq[String]] =
        Map(
          "k1" -> Seq("v1", "v2"),
          "k2" -> Seq("v3")
        )

      val res: Future[ResDomain] =
          httpClient2
            .post(url"$wireMockUrl/")
            .withBody(body)
            .execute(fromJson[ResDomain])

      res.futureValue shouldBe ResDomain("res")

      wireMockServer.verify(
        postRequestedFor(urlEqualTo("/"))
          .withHeader("Content-Type", equalTo("nonstandard/x-www-form-urlencoded"))
          .withRequestBody(equalTo("k1=v1&k1=v2&k2=v3"))
          .withHeader("User-Agent", equalTo("myapp"))
      )

      val headersCaptor  = ArgCaptor[Seq[(String, String)]]
      val responseCaptor = ArgCaptor[Future[HttpResponse]]

      verify(mockHttpHook)
        .apply(
          verb      = eqTo("POST"),
          url       = eqTo(url"$wireMockUrl/"),
          headers   = headersCaptor,
          body      = eqTo(Some(HookData.FromMap(body))),
          responseF = responseCaptor
        )(any[HeaderCarrier], any[ExecutionContext])

      headersCaptor.value should contain ("User-Agent" -> "myapp")
      headersCaptor.value should contain ("Content-Type" -> "nonstandard/x-www-form-urlencoded")
      val auditedResponse = responseCaptor.value.futureValue
      auditedResponse.status shouldBe 200
      auditedResponse.body   shouldBe "\"res\""
    }

    "work with form data - custom writeable for map type" in new Setup {
      implicit val hc = HeaderCarrier()

      wireMockServer.stubFor(
        WireMock.post(urlEqualTo("/"))
          .willReturn(aResponse().withBody("\"res\"").withStatus(200))
      )

      import play.api.libs.ws.{BodyWritable, InMemoryBody}
      implicit val writeableOf_urlEncodedForm: BodyWritable[scala.collection.mutable.Map[String, Seq[String]]] = {
        import java.net.URLEncoder
        BodyWritable(
          formData =>
            InMemoryBody(
              ByteString.fromString(
                formData.flatMap(item => item._2.map(c => s"${item._1}=${URLEncoder.encode(c, "UTF-8")}")).mkString("&")
              )
            ),
          "application/x-www-form-urlencoded"
        )
      }

      val body: scala.collection.mutable.Map[String, Seq[String]] =
        scala.collection.mutable.Map(
          "k1" -> Seq("v1", "v2"),
          "k2" -> Seq("v3")
        )

      val res: Future[ResDomain] =
          httpClient2
            .post(url"$wireMockUrl/")
            .withBody(body)
            .execute(fromJson[ResDomain])

      res.futureValue shouldBe ResDomain("res")

      wireMockServer.verify(
        postRequestedFor(urlEqualTo("/"))
          .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded"))
          .withRequestBody(equalTo("k1=v1&k1=v2&k2=v3"))
          .withHeader("User-Agent", equalTo("myapp"))
      )

      val headersCaptor  = ArgCaptor[Seq[(String, String)]]
      val responseCaptor = ArgCaptor[Future[HttpResponse]]

      verify(mockHttpHook)
        .apply(
          verb      = eqTo("POST"),
          url       = eqTo(url"$wireMockUrl/"),
          headers   = headersCaptor,
          body      = eqTo(Some(HookData.FromMap(body.toMap))),
          responseF = responseCaptor
        )(any[HeaderCarrier], any[ExecutionContext])

      headersCaptor.value should contain ("User-Agent" -> "myapp")
      headersCaptor.value should contain ("Content-Type" -> "application/x-www-form-urlencoded")
      val auditedResponse = responseCaptor.value.futureValue
      auditedResponse.status shouldBe 200
      auditedResponse.body   shouldBe "\"res\""
    }

    /* Note, using non-form-encoding and a non immutable Map implementation will not be escaped properly
    "work with any form data" in new Setup {
      implicit val hc = HeaderCarrier()

      wireMockServer.stubFor(
        WireMock.post(urlEqualTo("/"))
          .willReturn(aResponse().withBody("\"res\"").withStatus(200))
      )

      import play.api.libs.ws.{BodyWritable, InMemoryBody}
      implicit val writeableOf_urlEncodedForm: BodyWritable[scala.collection.mutable.Map[String, Seq[String]]] = {
        import java.net.URLEncoder
        BodyWritable(
          formData =>
            InMemoryBody(
              ByteString.fromString(
                formData.flatMap(item => item._2.map(c => s"${item._1}=${URLEncoder.encode(c, "UTF-8")}")).mkString("&")
              )
            ),
          "non-standard/x-www-form-urlencoded"
        )
      }

      val body: scala.collection.mutable.Map[String, Seq[String]] =
        scala.collection.mutable.Map(
          "k1" -> Seq("v1", "v2"),
          "k2" -> Seq("v3")
        )

      val res: Future[ResDomain] =
          httpClient2
            .post(url"$wireMockUrl/")
            .withBody(body)
            .execute(fromJson[ResDomain])

      res.futureValue shouldBe ResDomain("res")

      wireMockServer.verify(
        postRequestedFor(urlEqualTo("/"))
          .withHeader("Content-Type", equalTo("nonstandard/x-www-form-urlencoded"))
          .withRequestBody(equalTo("k1=v1&k1=v2&k2=v3"))
          .withHeader("User-Agent", equalTo("myapp"))
      )

      val headersCaptor  = ArgCaptor[Seq[(String, String)]]
      val responseCaptor = ArgCaptor[Future[HttpResponse]]

      verify(mockHttpHook)
        .apply(
          verb      = eqTo("POST"),
          url       = eqTo(url"$wireMockUrl/"),
          headers   = headersCaptor,
          body      = eqTo(Some(HookData.FromMap(body.toMap))),
          responseF = responseCaptor
        )(any[HeaderCarrier], any[ExecutionContext])

      headersCaptor.value should contain ("User-Agent" -> "myapp")
      headersCaptor.value should contain ("Content-Type" -> "application/x-www-form-urlencoded")
      val auditedResponse = responseCaptor.value.futureValue
      auditedResponse.status shouldBe 200
      auditedResponse.body   shouldBe "\"res\""
    }
    */

    "fail if call withBody on the wsRequest itself" in new Setup {
      implicit val hc = HeaderCarrier()

      a[RuntimeException] should be thrownBy
        httpClient2
          .put(url"$wireMockUrl/")
          .transform(_.withBody(toJson(ReqDomain("req"))))
          .replaceHeader("User-Agent" -> "ua2")
          .execute(fromJson[ResDomain])
    }

    "allow overriding user-agent" in new Setup {
      implicit val hc = HeaderCarrier()

      wireMockServer.stubFor(
        WireMock.put(urlEqualTo("/"))
          .willReturn(aResponse().withBody("\"res\"").withStatus(200))
      )

      val res: Future[ResDomain] =
        httpClient2
          .put(url"$wireMockUrl/")
          .withBody(Json.toJson(ReqDomain("req")))
          .replaceHeader("User-Agent" -> "ua2")
          .execute(fromJson[ResDomain])

      res.futureValue shouldBe ResDomain("res")

      wireMockServer.verify(
        putRequestedFor(urlEqualTo("/"))
          .withRequestBody(equalTo("\"req\""))
          .withHeader("User-Agent", equalTo("ua2"))
      )
    }

    "allow custom retries" in new Setup {
      val retries = new Retries {
        override val actorSystem   = as
        override val configuration = ConfigFactory
                                        .parseString("http-verbs.retries.intervals = [ 100.ms, 200.ms, 300.ms ]")
                                        .withFallback(ConfigFactory.load())
      }

      implicit val hc = HeaderCarrier()

      wireMockServer.stubFor(
        WireMock.put(urlEqualTo("/"))
          .willReturn(aResponse().withBody("\"res\"").withStatus(502))
      )

      val count = new AtomicInteger(0)

      val res: Future[ResDomain] =
        retries.retryFor("get reqdomain"){ case UpstreamErrorResponse.WithStatusCode(502) => true }{
          count.incrementAndGet
          httpClient2
            .put(url"$wireMockUrl/")
            .withBody(Json.toJson(ReqDomain("req")))
            .execute(fromJson[ResDomain])
        }

      res.failed.futureValue shouldBe a[UpstreamErrorResponse]
      count.get shouldBe 4
    }
  }

  trait Setup {
    implicit val as: ActorSystem = ActorSystem("test-actor-system")
    implicit val mat: Materializer = ActorMaterializer() // explicitly required for play-26

    val mockHttpHook = mock[HttpHook](withSettings.lenient)

    val maxAuditBodyLength = 30

    val httpClient2: HttpClient2 = {
      val config =
        Configuration(
          ConfigFactory.parseString(
            s"""|appName = myapp
                |http-verbs.auditing.maxBodyLength = $maxAuditBodyLength
                |""".stripMargin
          ).withFallback(ConfigFactory.load())
        )
      new HttpClient2Impl(
        wsClient = AhcWSClient(AhcWSClientConfigFactory.forConfig(config.underlying)),
        as,
        config,
        hooks = Seq(mockHttpHook),
      )
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
