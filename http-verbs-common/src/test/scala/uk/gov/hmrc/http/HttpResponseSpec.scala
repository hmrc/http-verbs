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

package uk.gov.hmrc.http

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

/**
  * Created by jonathan on 25/07/16.
  */
class HttpResponseSpec extends AnyWordSpec with Matchers {
  "unapply" should {
    "return matching object in" in {
      HttpResponse(status = 1, body = "test body", headers = Map("a" -> List("1", "2", "3"))) match {
        case HttpResponse(status, body, headers) =>
          status  shouldBe 1
          body    shouldBe "test body"
          headers shouldBe Map("a" -> List("1", "2", "3"))
      }
    }
  }

  "header" should {
    "return the `headOption` value of the associated and case-insensitive header name" in {
      val headers =
        Map(
          "Test-Header-1" -> Vector("v1", "v2"),
        )

      val response =
        HttpResponse(
          status  = 200,
          body    = "",
          headers = headers
        )

      response.header("test-header-1") shouldBe Some("v1")
    }
  }
}
