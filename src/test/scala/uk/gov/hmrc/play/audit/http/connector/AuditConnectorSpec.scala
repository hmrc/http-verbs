/*
 * Copyright 2015 HM Revenue & Customs
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

package uk.gov.hmrc.play.audit.http.connector

import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, Tag, WordSpecLike}
import play.api.libs.json.{JsObject, JsValue, Json}
import uk.gov.hmrc.play.audit.EventTypes
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.config.{AuditingConfig, BaseUri}
import uk.gov.hmrc.play.audit.model.{DataCall, DataEvent, ExtendedDataEvent, MergedDataEvent}
import uk.gov.hmrc.play.http.HttpResponse
import uk.gov.hmrc.play.http.logging.LoggingDetails

import scala.concurrent.{ExecutionContext, Future}

class AuditConnectorSpec extends WordSpecLike with Matchers with ScalaFutures {

  import scala.concurrent.ExecutionContext.Implicits.global

  val eventTypes = new EventTypes {}

  val stubConnector = new StubAuditConnector {}

  "checkResponse" should {
    "return None for any response code less than 300" in {
      val body = Json.obj("key" -> "value")

      (0 to 299).foreach { code =>
        val response = new HttpResponse {
          override val status = code
        }
        stubConnector.checkResponse(body, response) shouldBe None
      }
    }

    "Return Some message for a response code of 300 or above" in {
      val body = Json.obj("key" -> "value")

      (300 to 599).foreach { code =>
        val response = new HttpResponse {
          override val status = code
        }
        val result = stubConnector.checkResponse(body, response)
        result shouldNot be(None)
        checkAuditFailureMessage(result.get, body, code)
      }
    }
  }

  "handleResult" should {
    case class Called(logError1: Option[String] = None, logError2: Option[(String, Throwable)] = None)

    class MockAuditConnector extends StubAuditConnector {
      var called = Called()

      override protected def logError(s: String, t: Throwable): Unit = called = called.copy(logError2 = Some((s, t)))

      override protected def logError(s: String): Unit = called = called.copy(logError1 = Some(s))
    }

    "not log any error or for a result status of 200" in {
      val mockConnector = new MockAuditConnector
      val body = Json.obj("key" -> "value")

      val response = new HttpResponse {
        override val status = 200
      }
      mockConnector.handleResult(Future.successful(response), body)(new HeaderCarrier)
      mockConnector.called shouldBe Called(None, None)
    }

    "log an error for a result status of 300" in {
      val mockConnector = new MockAuditConnector
      val body = Json.obj("key" -> "value")

      val code = 300
      val response = new HttpResponse {
        override val status = code
      }

      val f = Future.successful(response)
      mockConnector.handleResult(f, body)(new HeaderCarrier).failed.futureValue

      mockConnector.called.logError2 shouldBe None
      mockConnector.called.logError1 shouldNot be(None)

      checkAuditFailureMessage(mockConnector.called.logError1.get, body, code)
    }

    "log an error for a Future.failed" in {
      val mockConnector = new MockAuditConnector
      val body = Json.obj("key" -> "value")

      val f = Future.failed(new Exception("failed"))
      mockConnector.handleResult(f, body)(new HeaderCarrier).failed.futureValue

      mockConnector.called.logError1 shouldBe None
      mockConnector.called.logError2 shouldNot be(None)

      val (message, _) = mockConnector.called.logError2.get
      checkAuditRequestFailureMessage(message, body)
    }
  }

  "sendLargeMergedEvent" should {
    case class Called(callDatastream: Option[JsValue] = None, handleResult: Boolean = false)

    class MockAuditConnector(response: Future[HttpResponse]) extends StubAuditConnector {
      var called = new Called()

      override def auditingConfig: AuditingConfig = AuditingConfig(BaseUri("datastream-base-url", 8080))

      override protected[connector] def handleResult(resultF: Future[HttpResponse], body: JsValue)(implicit ld: LoggingDetails) = {
        called = called.copy(handleResult = true)
        resultF
      }

      override protected def callAuditConsumer(url: String, body: JsValue)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
        called = called.copy(callDatastream = Some(body))

        response
      }
    }

    "call datastream with large merged event" taggedAs Tag("txm80") in {
      val mockConnector = new MockAuditConnector(Future.failed(new Exception("failed")))
      val mergedEvent = MergedDataEvent("Test", "Test", "TestEventId",
                                        DataCall(Map.empty, Map.empty, DateTime.now(DateTimeZone.UTC)),
                                        DataCall(Map.empty, Map.empty, DateTime.now(DateTimeZone.UTC)))
      mockConnector.sendLargeMergedEvent(mergedEvent)
      mockConnector.called shouldBe Called(Some(Json.toJson(mergedEvent)), true)
    }
  }

  "sendEvent" should {
    case class Called(callDatastream: Option[JsValue] = None, handleResult: Boolean = false)

    class MockAuditConnector(response: Future[HttpResponse], enabled: Boolean = true) extends StubAuditConnector {
      var called = new Called()

      override def auditingConfig: AuditingConfig = AuditingConfig(BaseUri("datastream-base-url", 8080), enabled)

      override protected[connector] def handleResult(resultF: Future[HttpResponse], body: JsValue)(implicit ld: LoggingDetails) = {
        called = called.copy(handleResult = true)
        super.handleResult(resultF, body)
      }

      override protected def callAuditConsumer(url: String, body: JsValue)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
        called = called.copy(callDatastream = Some(body))

        response
      }
    }

    "call datastream with the event converted to json" in {
      val mockConnector = new MockAuditConnector(response = Future.successful(HttpResponse(200)))
      val event = DataEvent("source", "type")
      mockConnector.sendEvent(event).futureValue should be (AuditResult.Success)

      mockConnector.called shouldBe Called(Some(Json.toJson(event)), true)
    }

    "return a failed future if the HTTP response status is greater than 299" in {
      val mockConnector = new MockAuditConnector(response = Future.successful(HttpResponse(300)))
      val event = DataEvent("source", "type")

      val failureResponse = mockConnector.sendEvent(event).failed.futureValue
      failureResponse should have ('nested (None))
      checkAuditFailureMessage(failureResponse.getMessage, Json.toJson(event), 300)

      mockConnector.called shouldBe Called(Some(Json.toJson(event)), true)
    }

    "return a failed future if there is an exception in the HTTP connection" in {
      val exception = new Exception("failed")
      val mockConnector = new MockAuditConnector(response = Future.failed(exception))
      val event = DataEvent("source", "type")

      val failureResponse = mockConnector.sendEvent(event).failed.futureValue
      failureResponse should have ('nested (Some(exception)))
      checkAuditRequestFailureMessage(failureResponse.getMessage, Json.toJson(event))

      mockConnector.called shouldBe Called(Some(Json.toJson(event)), true)
    }

    "return disabled if auditing is not enabled" in {
      val mockConnector = new MockAuditConnector(response = Future.successful(HttpResponse(200)), enabled = false)
      val event = DataEvent("source", "type")
      mockConnector.sendEvent(event).futureValue should be (AuditResult.Disabled)

      mockConnector.called shouldBe Called()
    }

    "serialize the date correctly" in {
      val event: DataEvent = DataEvent("source", "type", generatedAt = new DateTime(0, DateTimeZone.UTC))
      val json: JsValue = Json.toJson(event)

      (json \ "generatedAt").as[String] shouldBe "1970-01-01T00:00:00.000+0000"
    }

    "call data stream with extended event data converted to json" in {
      val response = Future.successful(HttpResponse(200))

      val mockConnector = new MockAuditConnector(response)
      val detail = Json.parse( """{"some-event": "value", "some-other-event": "other-value"}""")
      val event: ExtendedDataEvent = ExtendedDataEvent(auditSource = "source", auditType = "type", detail = detail)

      mockConnector.sendEvent(event)

      mockConnector.called shouldBe Called(Some(Json.toJson(event)), true)
    }

    "calls handleResult" in {
      val f = Future.failed(new Exception("failed"))
      val mockConnector = new MockAuditConnector(f)
      val event: DataEvent = DataEvent("source", "type")
      mockConnector.sendEvent(event)
      val Called(_, handleResultCalled) = mockConnector.called
      handleResultCalled shouldBe true
    }

  }

  private def checkAuditRequestFailureMessage(message: String, body: JsValue) {
    message should startWith(AuditEventFailureKeys.LoggingAuditRequestFailureKey)
    message should include(body.toString)
  }

  private def checkAuditFailureMessage(message: String, body: JsValue, code: Int) {
    message should startWith(AuditEventFailureKeys.LoggingAuditFailureResponseKey)
    message should include(body.toString)
    message should include(code.toString)
  }


  "makeFailureMessage" should {
    "make a message containing the body and the right logging key" in {
      val body: JsObject = Json.obj("key" -> "value")
      val message: String = stubConnector.makeFailureMessage(body)
      message should startWith(AuditEventFailureKeys.LoggingAuditRequestFailureKey)
      message should include(body.toString)
    }
  }
}

trait StubAuditConnector extends AuditConnector {
  override def auditingConfig: AuditingConfig = ???

  override protected def callAuditConsumer(url: String, body: JsValue)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = ???

  override protected def logError(s: String, t: Throwable) {}

  override protected def logError(s: String) {}
}