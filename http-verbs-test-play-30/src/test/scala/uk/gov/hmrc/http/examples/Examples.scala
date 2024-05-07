/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.http.examples


import com.github.tomakehurst.wiremock.client.WireMock._
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{Json, Reads, Writes, __}
import play.api.libs.functional.syntax._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.test.{HttpClientSupport, WireMockSupport}
import uk.gov.hmrc.http.HttpReads.Implicits._

import java.time.LocalDate
import java.util.Base64
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import scala.util.{Failure, Success, Try}
import scala.xml.XML

class Examples
  extends AnyWordSpec
     with Matchers
     with ScalaFutures
     with IntegrationPatience
     with HttpClientSupport
     with WireMockSupport {

  class CustomException(message: String) extends Exception(message)

  case class BankHoliday(
    title: String,
    date : LocalDate
  )

  object BankHoliday {
    val reads: Reads[BankHoliday] =
      ( (__ \ "title").read[String]
      ~ (__ \ "date" ).read[LocalDate]
      )(BankHoliday.apply _)
  }

  case class BankHolidays(
    events: Seq[BankHoliday]
  )


  object BankHolidays {
    val reads: Reads[BankHolidays] = {
      implicit val bhr: Reads[BankHoliday] = BankHoliday.reads
      (__ \ "events").read[Seq[BankHoliday]].map(BankHolidays.apply)
    }
  }

  case class User(
    email   : String,
    fullName: String
  )

  object User {
    val writes: Writes[User] =
      ( (__ \ "email"   ).write[String]
      ~ (__ \ "fullName").write[String]
      )(u => (u.email, u.fullName))
  }

  case class UserIdentifier(id: String)

  object UserIdentifier {
    val reads = (__ \ "id").read[String].map(UserIdentifier.apply)
  }

  private implicit val uw : Writes[User]          = User.writes
  private implicit val uir: Reads[UserIdentifier] = UserIdentifier.reads
  private implicit val bhr: Reads[BankHolidays]   = BankHolidays.reads

  "A verb" should {
    "allow the user to set additional headers" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()

      stubFor(
        get(urlEqualTo("/bank-holidays.json"))
          .willReturn(aResponse().withStatus(200).withBodyFile("bankHolidays.json"))
      )

      httpClient.GET[BankHolidays](
        url         = url"$wireMockUrl/bank-holidays.json",
        headers     = Seq("some-header" -> "header value")
      ).futureValue

      verify(getRequestedFor(urlEqualTo("/bank-holidays.json"))
        .withHeader("some-header", equalTo("header value"))
      )
    }

    "allow the user to set an authorization header using the header carrier for internal hosts" in {
      val username = "user"
      val password = "123"
      val encodedAuthHeader = Base64.getEncoder.encodeToString(s"$username:$password".getBytes("UTF-8"))
      implicit val hc: HeaderCarrier = HeaderCarrier(authorization = Some(Authorization(s"Basic $encodedAuthHeader")))

      stubFor(
        get(urlEqualTo("/bank-holidays.json"))
          .willReturn(aResponse().withStatus(200).withBodyFile("bankHolidays.json"))
      )

      httpClient.GET[BankHolidays](url"$wireMockUrl/bank-holidays.json").futureValue

      verify(
        getRequestedFor(urlEqualTo("/bank-holidays.json"))
          .withHeader("Authorization", equalTo(s"Basic $encodedAuthHeader"))
      )
    }

    "allow the user to set an authorization header as part of a header in the POST and override the Authorization header in the headerCarrier" in {
      implicit val hc: HeaderCarrier = HeaderCarrier(authorization = None)

      stubFor(
        post(urlEqualTo("/create-user"))
          .willReturn(aResponse().withStatus(200))
      )

      val response: HttpResponse =
        httpClient.POST[User, HttpResponse](
          url     = url"$wireMockUrl/create-user",
          body    = User("me@mail.com", "John Smith"),
          headers = Seq("Authorization" -> "Basic dXNlcjoxMjM=")
        ).futureValue
      response.status shouldBe 200

      verify(
        postRequestedFor(urlEqualTo("/create-user"))
          .withHeader("Authorization", equalTo("Basic dXNlcjoxMjM="))
      )
    }

    "allow the user to explicitly forward headers to external hosts" in {
      implicit val hc: HeaderCarrier = HeaderCarrier(authorization = Some(Authorization("Basic dXNlcjoxMjM=")))

      // for demonstration, we're initialising a client which considers `localhost` as an external host
      val httpClient: HttpClient = mkHttpClient(
        config =
          ConfigFactory.parseString(
            """|internalServiceHostPatterns = []
               |""".stripMargin
          ).withFallback(ConfigFactory.load())
      )

      stubFor(
        post(urlEqualTo("/create-user"))
          .willReturn(aResponse().withStatus(200))
      )

      val response: HttpResponse =
        httpClient.POST[User, HttpResponse](
          url     = url"$wireMockUrl/create-user",
          body    = User("me@mail.com", "John Smith"),
          headers = hc.headers(Seq(hc.names.authorisation))
        ).futureValue
      response.status shouldBe 200

      verify(
        postRequestedFor(urlEqualTo("/create-user"))
          .withHeader("Authorization", equalTo("Basic dXNlcjoxMjM="))
      )
    }

    "allow the user to specify a string url (not recommended)" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()

      stubFor(
        get(urlEqualTo("/bank-holidays.json"))
          .willReturn(aResponse().withStatus(200).withBodyFile("bankHolidays.json"))
      )

      httpClient.GET[BankHolidays](s"$wireMockUrl/bank-holidays.json", queryParams = Seq.empty, headers = Seq.empty).futureValue

      verify(getRequestedFor(urlEqualTo("/bank-holidays.json")))
    }
  }

  "A GET" should {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    "read some json and return a case class" in {
      stubFor(
        get(urlEqualTo("/bank-holidays.json"))
          .willReturn(aResponse().withStatus(200).withBodyFile("bankHolidays.json"))
      )

      val bankHolidays: BankHolidays = httpClient.GET[BankHolidays](url"$wireMockUrl/bank-holidays.json").futureValue
      bankHolidays.events.head shouldBe BankHoliday("New Year's Day", LocalDate.of(2017, 1, 2))
    }

    "read some json and return a raw http response" in {
      stubFor(
        get(urlEqualTo("/bank-holidays.json"))
          .willReturn(aResponse().withStatus(200).withBodyFile("bankHolidays.json"))
      )

      val response: HttpResponse = httpClient.GET[HttpResponse](url"$wireMockUrl/bank-holidays.json").futureValue
      response.status shouldBe 200
      Json.parse(response.body) shouldBe Json.parse(Source.fromResource("__files/bankHolidays.json").mkString)
    }

    "be able to handle a 404 without throwing an exception" in {
      stubFor(
        get(urlEqualTo("/404.json"))
          .willReturn(aResponse().withStatus(404))
      )

      // By adding an Option to your case class, the 404 is translated into None
      val bankHolidays: Option[BankHolidays] = httpClient.GET[Option[BankHolidays]](url"$wireMockUrl/404.json").futureValue
      bankHolidays shouldBe None
    }

    "throw an Upstream4xxResponse for 4xx errors" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()

      stubFor(
        get(urlEqualTo("/401.json"))
          .willReturn(aResponse().withStatus(401))
      )

      httpClient.GET[Option[BankHolidays]](url"$wireMockUrl/401.json")
        .recover {
          case UpstreamErrorResponse.Upstream4xxResponse(e) => // handle here 4xx errors
        }.futureValue
    }

    "throw an Upstream5xxResponse for 5xx errors" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()

      stubFor(
        get(urlEqualTo("/500.json"))
          .willReturn(aResponse().withStatus(500))
      )

      httpClient.GET[Option[BankHolidays]](url"$wireMockUrl/500.json")
        .recover {
          case UpstreamErrorResponse.Upstream5xxResponse(e) => // handle here 5xx errors
        }.futureValue
    }
  }

  "A POST" should {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    "write a case class to json body and return a response" in {
      stubFor(
        post(urlEqualTo("/create-user"))
          .willReturn(aResponse().withStatus(204))
      )

      val user = User("me@mail.com", "John Smith")

      // Use HttpResponse when the API always returns an empty body
      val response: HttpResponse = httpClient.POST[User, HttpResponse](url"$wireMockUrl/create-user", user).futureValue
      response.status shouldBe 204
    }

    "read the response body of the POST into a case class" in {
      stubFor(
        post(urlEqualTo("/create-user"))
          .willReturn(aResponse().withStatus(200).withBodyFile("userId.json"))
      )

      val user = User("me@mail.com", "John Smith")

      // Use a case class when the API returns a json body
      val userId: UserIdentifier = httpClient.POST[User, UserIdentifier](url"$wireMockUrl/create-user", user).futureValue
      userId.id shouldBe "123"
    }
  }

  "HttpResponse" should {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    def fromXml(xml: String): BankHolidays =
      BankHolidays(
        (XML.loadString(xml) \ "event").map { event =>
          BankHoliday((event \ "title").text, LocalDate.parse((event \ "date").text))
        }
      )

    def responseHandler(response: HttpResponse): Option[BankHolidays] =
      response.status match {
        case 200 => Try(fromXml(response.body)) match {
          case Success(data) => Some(data)
          case Failure(e)    => throw new CustomException("Unable to parse response")
        }
        case other => throw new CustomException(s"Unexpected status code $other")
      }

    "Return some data when getting a 200 back" in {
      stubFor(
        get(urlEqualTo("/bank-holidays.xml"))
          .willReturn(aResponse().withStatus(200).withBodyFile("bankHolidays.xml"))
      )

      val bankHolidays = httpClient.GET[HttpResponse](url"$wireMockUrl/bank-holidays.xml")
        .map(responseHandler).futureValue

      bankHolidays.get.events.head shouldBe BankHoliday("New Year's Day", LocalDate.of(2017, 1, 2))
    }

    "Fail when the response payload cannot be deserialised" in {
      stubFor(
        get(urlEqualTo("/bank-holidays.xml"))
          .willReturn(aResponse().withStatus(200).withBody("Not XML"))
      )

      httpClient.GET[HttpResponse](url"$wireMockUrl/bank-holidays.xml").map(responseHandler)
        .failed.futureValue shouldBe a [CustomException]
    }
  }

  "HttpReads" should {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    implicit val responseHandler: HttpReads[Option[BankHolidays]] = new HttpReads[Option[BankHolidays]] {
      override def read(method: String, url: String, response: HttpResponse): Option[BankHolidays] =
        response.status match {
          case 200 => Try(response.json.as[BankHolidays]) match {
            case Success(data) => Some(data)
            case Failure(e)    => throw new CustomException("Unable to parse response")
          }
          case 404 => None
          case unexpectedStatus => throw new CustomException(s"Unexpected response code '$unexpectedStatus'")
        }
    }

    "Return some data when getting a 200 back" in {
      stubFor(
        get(urlEqualTo("/bank-holidays.json"))
          .willReturn(aResponse().withStatus(200).withBodyFile("bankHolidays.json"))
      )

      val bankHolidays = httpClient.GET[Option[BankHolidays]](url"$wireMockUrl/bank-holidays.json").futureValue
      bankHolidays.get.events.head shouldBe BankHoliday("New Year's Day", LocalDate.of(2017, 1, 2))
    }

    "Fail when the response payload cannot be deserialised" in {
      stubFor(
        get(urlEqualTo("/bank-holidays.json"))
          .willReturn(aResponse().withStatus(200).withBody("Not JSON"))
      )

      httpClient.GET[Option[BankHolidays]](url"$wireMockUrl/bank-holidays.json").failed.futureValue shouldBe a [CustomException]
    }

    "Return None when getting a 404 back" in {
      stubFor(
        get(urlEqualTo("/bank-holidays.json"))
          .willReturn(aResponse().withStatus(404))
      )

      val bankHolidays = httpClient.GET[Option[BankHolidays]](url"$wireMockUrl/bank-holidays.json").futureValue
      bankHolidays shouldBe None
    }

    "Fail if we get back any other status code" in {
      stubFor(
        get(urlEqualTo("/bank-holidays.json"))
          .willReturn(aResponse().withStatus(418))
      )

      httpClient.GET[Option[BankHolidays]](url"$wireMockUrl/bank-holidays.json").failed.futureValue shouldBe a [CustomException]
    }
  }
}
