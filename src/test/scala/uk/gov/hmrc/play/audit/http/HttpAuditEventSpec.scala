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

package uk.gov.hmrc.play.audit.http

import org.scalatest.{LoneElement, Matchers, WordSpecLike}
import play.api.test.{FakeRequest, WithApplication}

class HttpAuditEventSpec extends WordSpecLike with Matchers with LoneElement {

  "The optional audit fields code" should {

    "Return the correct size map when fed with a given amount of items" in {
      val optionalFields = HeaderFieldsExtractor.optionalAuditFields(Map("Foo" -> "Bar", "Ehh" -> "Meh", "Surrogate" -> "Cool"))
      optionalFields.loneElement shouldBe ("surrogate" -> "Cool")
    }

    "Return the correct size map when fed with two identicle items" in {
      val optionalFields = HeaderFieldsExtractor.optionalAuditFields(Map("Foo" -> "Bar", "Ehh" -> "Meh", "Surrogate" -> "Cool", "Surrogate" -> "Cool"))
      optionalFields.loneElement shouldBe ("surrogate" -> "Cool")
    }


    "Return the correct size map when fed with seq values" in {
      val optionalFields = HeaderFieldsExtractor.optionalAuditFieldsSeq(Map("Foo" -> Seq("Bar"), "Ehh" -> Seq("Meh"), "Surrogate" -> Seq("Cool"), "Surrogate" -> Seq("Cool", "funk", "grr")))
      optionalFields.loneElement shouldBe ("surrogate" -> "Cool,funk,grr")
    }

    "Return the correct size map when fed with no items" in {
      val optionalFields = HeaderFieldsExtractor.optionalAuditFields(Map("Foo" -> "Bar", "Ehh" -> "Meh"))
      optionalFields shouldBe empty
    }

  }

  "The code to generate an audit event" should {

    object HttpAuditEventForTest extends HttpAuditEvent {
      override def appName: String = "my-test-app"
    }

    "create a valid audit event with optional headers" in new WithApplication {
      val r = FakeRequest().withHeaders(("Foo" -> "Bar"), ("Ehh" -> "Meh"), ("Surrogate" -> "Cool"), ("Surrogate" -> "Cool"))
      val event = HttpAuditEventForTest.dataEvent("foo", "bar", r)
      event.detail.get("surrogate") shouldBe Some("Cool")
    }
    "create a valid audit event with no optional headers" in new WithApplication {
      val r = FakeRequest().withHeaders(("Foo" -> "Bar"), ("Ehh" -> "Meh"))
      val event = HttpAuditEventForTest.dataEvent("foo", "bar", r)
      event.detail.get("surrogate") shouldBe None
    }

//    "Include the authorisation, token and ip address in the audit messages" in new WithApplication {
//        val request = FakeRequest("GET", "/foo")
//
//        implicit val hcWithoutSessionData = new HeaderCarrier()
//
//        val event: DataEvent = AuditFilter.buildAuditRequestEvent(EventTypes.ServiceSentResponse, request, "")
//        event.detail should contain ("Authorization" -> "-")
//        event.detail should contain ("token" -> "-")
//        event.detail.keySet should contain ("ipAddress")
//      }

  }
}
