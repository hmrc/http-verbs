/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.play.http

import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{JsString, Json}

/**
  * Created by jonathan on 25/07/16.
  */
class HttpResponseSpec extends WordSpec with Matchers {
  "unapply" should {
    "return matching object in" in {
      HttpResponse(1, Some(JsString("test json")), Map("a" -> List("1", "2", "3")), Some("test body")) match {
        case HttpResponse(status, json, headers, body) => {
          status shouldBe 1
          json shouldBe JsString("test json")
          headers shouldBe Map("a" -> List("1", "2", "3"))
          body shouldBe "test body"
        }
      }
    }

    "return matching object when json is null" in {
      HttpResponse(1, None, Map("a" -> List("1", "2", "3")), Some("test body")) match {
        case HttpResponse(status, json, headers, body) => {
          status shouldBe 1
          json shouldBe null
          headers shouldBe Map("a" -> List("1", "2", "3"))
          body shouldBe "test body"
        }
      }
    }

    "return matching object with response json as response body when response string is None" in {
      HttpResponse(1, Some(JsString("test json")), Map("a" -> List("1", "2", "3")), None) match {
        case HttpResponse(status, json, headers, body) => {
          status shouldBe 1
          json shouldBe JsString("test json")
          headers shouldBe Map("a" -> List("1", "2", "3"))
          body shouldBe Json.prettyPrint(JsString("test json"))
        }
      }
    }

    "return matching object with response json and response body are null" in {
      HttpResponse(1, None, Map("a" -> List("1", "2", "3")), None) match {
        case HttpResponse(status, json, headers, body) => {
          status shouldBe 1
          json shouldBe null
          headers shouldBe Map("a" -> List("1", "2", "3"))
          body shouldBe null
        }
      }
    }
  }
}
