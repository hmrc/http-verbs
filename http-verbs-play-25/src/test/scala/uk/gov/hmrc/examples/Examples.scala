/*
 * Copyright 2020 HM Revenue & Customs
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

/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.examples

import akka.actor.ActorSystem
import com.github.tomakehurst.wiremock.client.WireMock._
import com.typesafe.config.Config
import org.apache.commons.codec.binary.Base64
import org.joda.time.LocalDate
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{Reads, Writes}
import play.api.libs.ws.WSClient
import uk.gov.hmrc.examples.utils._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.hooks.HttpHook
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import scala.xml.XML

class Examples extends AnyWordSpecLike
  with ScalaFutures
  with IntegrationPatience
  with WiremockTestServer
  with Matchers {

  class CustomException(message: String) extends Exception(message) {}

  private lazy val app: Application = new GuiceApplicationBuilder().build()

  private lazy val client = new HttpGet with HttpPost with HttpDelete with HttpPatch with HttpPut with WSHttp {
    override def wsClient: WSClient                      = app.injector.instanceOf[WSClient]
    override protected def configuration: Option[Config] = None
    override val hooks: Seq[HttpHook]                    = Seq.empty
    override protected def actorSystem: ActorSystem      = ActorSystem("test-actor-system")

    // The default implementation doesn't allow setting headers on calls to hosts external to mdtp.
    override def applicableHeaders(url: String)(implicit hc: HeaderCarrier): Seq[(String, String)] = hc.headers
  }

  private implicit val userWrites: Writes[User] = User.writes
  private implicit val userIdentifierWrites: Reads[UserIdentifier] = UserIdentifier.reads
  private implicit val bankHolidaysReads: Reads[BankHolidays] = BankHolidays.reads

  "A verb" should {

    "allow the user to set additional headers using the header carrier" in {

      implicit val hc = HeaderCarrier(otherHeaders = Seq("some-header" -> "header value"))

      stubFor(get(urlEqualTo("/bank-holidays.json"))
        .willReturn(aResponse().withStatus(200).withBody(JsonPayloads.bankHolidays)))

      client.GET[BankHolidays]("http://localhost:20001/bank-holidays.json").futureValue

      verify(getRequestedFor(urlEqualTo("/bank-holidays.json"))
        .withHeader("some-header", equalTo("header value")))

    }

    "allow the use to set an authorization header using the header carrier" in {

      val username = "user"
      val password = "123"
      val encodedAuthHeader = Base64.encodeBase64String(s"$username:$password".getBytes("UTF-8"))
      implicit val hc = HeaderCarrier(authorization = Some(Authorization(s"Basic $encodedAuthHeader")))

      stubFor(get(urlEqualTo("/bank-holidays.json"))
        .willReturn(aResponse().withStatus(200).withBody(JsonPayloads.bankHolidays)))

      client.GET[BankHolidays]("http://localhost:20001/bank-holidays.json").futureValue

      verify(getRequestedFor(urlEqualTo("/bank-holidays.json"))
        .withHeader("Authorization", equalTo("Basic dXNlcjoxMjM=")))

    }

    "allow the user to set additional headers as part of the POST" in {

      implicit val hc = HeaderCarrier()

      stubFor(post(urlEqualTo("/create-user")).willReturn(aResponse().withStatus(200)))

      val user = User("me@mail.com", "John Smith")

      client.POST[User, HttpResponse]("http://localhost:20001/create-user", user,
        headers = Seq("some-header" -> "header value")).futureValue

      verify(postRequestedFor(urlEqualTo("/create-user"))
        .withHeader("some-header", equalTo("header value")))

    }

    "allow the use to set an authorization header as part of a header in the POST and override the Authorization header in the headerCarrier" in {

      implicit val hc = HeaderCarrier(authorization = None)

      stubFor(post(urlEqualTo("/create-user")).willReturn(aResponse().withStatus(200)))

      val user = User("me@mail.com", "John Smith")

      client.POST[User, HttpResponse]("http://localhost:20001/create-user", user,
        headers = Seq("Authorization" -> "Basic dXNlcjoxMjM=")).futureValue

      verify(postRequestedFor(urlEqualTo("/create-user"))
        .withHeader("Authorization", equalTo("Basic dXNlcjoxMjM=")))

    }
  }

  "A GET" should {

    implicit val hc = HeaderCarrier()

    "read some json and return a case class" in {

      stubFor(get(urlEqualTo("/bank-holidays.json"))
        .willReturn(aResponse().withStatus(200).withBody(JsonPayloads.bankHolidays)
        ))

      val bankHolidays: BankHolidays = client.GET[BankHolidays]("http://localhost:20001/bank-holidays.json").futureValue
      bankHolidays.events.head shouldBe BankHoliday("New Year’s Day", new LocalDate(2017, 1, 2))
    }

    "read some json and return a raw http response" in {

      stubFor(get(urlEqualTo("/bank-holidays.json"))
        .willReturn(aResponse().withStatus(200).withBody(JsonPayloads.bankHolidays)))

      val response: HttpResponse = client.GET("http://localhost:20001/bank-holidays.json").futureValue
      response.status shouldBe 200
      response.body shouldBe JsonPayloads.bankHolidays
    }

    "be able to handle a 404 without throwing an exception" in {

      stubFor(get(urlEqualTo("/404.json")).willReturn(aResponse().withStatus(404)       ))

      // By adding an Option to your case class, the 404 is translated into None
      val bankHolidays: Option[BankHolidays] = client.GET[Option[BankHolidays]]("http://localhost:20001/404.json").futureValue
      bankHolidays shouldBe None
    }

    "throw an BadRequestException for 400 errors" in {

      stubFor(get(urlEqualTo("/400.json")).willReturn(aResponse().withStatus(400)))

      client.GET[Option[BankHolidays]]("http://localhost:20001/400.json").recover {
        case e: BadRequestException => // handle here a bad request
      }.futureValue
    }

    "throw an Upstream4xxResponse for 4xx errors and Upstream5xxResponse for 5xx errors" in {
      implicit val hc = HeaderCarrier()

      stubFor(get(urlEqualTo("/401.json")).willReturn(aResponse().withStatus(401)))
      stubFor(get(urlEqualTo("/500.json")).willReturn(aResponse().withStatus(500)))

      client.GET[Option[BankHolidays]]("http://localhost:20001/401.json").recover {
        case e: UpstreamErrorResponse => e match {
          case Upstream4xxResponse(message, upstreamResponseCode, reportAs, headers) => // handle here 4xx errors
          case Upstream5xxResponse(message, upstreamResponseCode, reportAs, headers) => // handle here 5xx errors
        }
      }.futureValue
    }
  }

  "A POST" should {

    implicit val hc = HeaderCarrier()

    "write a case class to json body and return a response" in {

      stubFor(post(urlEqualTo("/create-user"))
        .willReturn(aResponse().withStatus(204)))

      val user = User("me@mail.com", "John Smith")

      // Use HttpResponse when the API always returns an empty body
      val response: HttpResponse = client.POST[User, HttpResponse]("http://localhost:20001/create-user", user).futureValue
      response.status shouldBe 204
    }

    "read the response body of the POST into a case class" in {

      stubFor(post(urlEqualTo("/create-user"))
        .willReturn(aResponse().withStatus(200).withBody(JsonPayloads.userId)))

      val user = User("me@mail.com", "John Smith")

      // Use a case class when the API returns a json body
      val userId: UserIdentifier = client.POST[User, UserIdentifier]("http://localhost:20001/create-user", user).futureValue
      userId.id shouldBe "123"
    }

    "be able to handle both 204 and 200 in the same configuration" in {

      stubFor(post(urlEqualTo("/create-user"))
        .willReturn(aResponse().withStatus(204)))
      val user = User("me@mail.com", "John Smith")

      // Use Option[T], where T is your case class, if the API might return both 200 and 204
      val userId: Option[UserIdentifier] = client.POST[User, Option[UserIdentifier]]("http://localhost:20001/create-user", user).futureValue
      userId shouldBe None
    }
  }

  "HttpResponse" should {

    implicit val hc = HeaderCarrier()

    def fromXml(xml: String): BankHolidays =
      BankHolidays((XML.loadString(xml) \ "event") map { event => {
        BankHoliday((event \ "title").text, LocalDate.parse((event \ "date").text)) }})

    def responseHandler(response: HttpResponse) : Option[BankHolidays] = {
      response.status match {
        case 200 => Try(fromXml(response.body)) match {
          case Success(data) => Some(data)
          case Failure(e) =>
            throw new CustomException("Unable to parse response")
        }
      }
    }

    "Return some data when getting a 200 back" in {
      stubFor(get(urlEqualTo("/bank-holidays.xml"))
        .willReturn(aResponse().withStatus(200).withBody(XmlPayloads.bankHolidays)))

      val bankHolidays = client.GET[HttpResponse]("http://localhost:20001/bank-holidays.xml")
        .map(responseHandler).futureValue

      bankHolidays.get.events.head shouldBe BankHoliday("New Year’s Day", new LocalDate(2017, 1, 2))
    }

    "Fail when the response payload cannot be deserialised" in {
      stubFor(get(urlEqualTo("/bank-holidays.xml"))
        .willReturn(aResponse().withStatus(200).withBody("Not XML")))

      a[CustomException] shouldBe thrownBy {
        Await.result(client.GET[HttpResponse]("http://localhost:20001/bank-holidays.xml")
          .map(responseHandler), Duration(2, SECONDS))
      }
    }
  }

  "HttpReads" should {

    implicit val hc = HeaderCarrier()

    implicit val responseHandler = new HttpReads[Option[BankHolidays]] {
      override def read(method: String, url: String, response: HttpResponse): Option[BankHolidays] = {
        response.status match {
          case 200 => Try(response.json.as[BankHolidays]) match {
            case Success(data) => Some(data)
            case Failure(e) => throw new CustomException("Unable to parse response")
          }
          case 404 => None
          case unexpectedStatus => throw new CustomException(s"Unexpected response code '$unexpectedStatus'")
        }
      }
    }

    "Return some data when getting a 200 back" in {
      stubFor(get(urlEqualTo("/bank-holidays.json"))
        .willReturn(aResponse().withStatus(200).withBody(JsonPayloads.bankHolidays)
        ))

      val bankHolidays = client.GET[Option[BankHolidays]]("http://localhost:20001/bank-holidays.json").futureValue
      bankHolidays.get.events.head shouldBe BankHoliday("New Year’s Day", new LocalDate(2017, 1, 2))
    }

    "Fail when the response payload cannot be deserialised" in {
      stubFor(get(urlEqualTo("/bank-holidays.json"))
        .willReturn(aResponse().withStatus(200).withBody("Not JSON")
        ))

      a[CustomException] shouldBe thrownBy {
        Await.result(
          client.GET[Option[BankHolidays]]("http://localhost:20001/bank-holidays.json"),
          Duration(2, SECONDS))
      }
    }

    "Return None when getting a 404 back" in {
      stubFor(get(urlEqualTo("/bank-holidays.json"))
        .willReturn(aResponse().withStatus(404)
        ))

      val bankHolidays = client.GET[Option[BankHolidays]]("http://localhost:20001/bank-holidays.json").futureValue
      bankHolidays shouldBe None
    }

    "Fail if we get back any other status code" in {
      stubFor(get(urlEqualTo("/bank-holidays.json"))
        .willReturn(aResponse().withStatus(418)
        ))

      a[CustomException] shouldBe thrownBy {
        Await.result(client.GET[Option[BankHolidays]]("http://localhost:20001/bank-holidays.json"), Duration(2, SECONDS))
      }
    }
  }
}
