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

package uk.gov.hmrc.http.controllers

import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers
import play.api.libs.json._

class JsPathEnrichmentSpec extends AnyWordSpecLike with Matchers {

    import JsPathEnrichment.RichJsPath

    implicit val reads: Reads[Option[BigDecimal]] = (JsPath \ "rti" \ "balance").tolerantReadNullable[BigDecimal]

    val pathDoesNotExistJson =
      Json.parse(
        """{
          "nonRti": {
            "paidToDate": 200.25
          }
        }"""
      )

    val pathExistsAndValueMissingJson =
      Json.parse(
        """{
          "rti": {
            "notTheBalance": 123.45
          }
        }"""
      )

    val pathAndValueExistsJson =
      Json.parse(
        """{
          "rti": {
            "balance": 899.80
          }
        }"""
      )

  "Parsing json when the path does not exist prior to the structure being parsed" should {
    "result in None without failure when early sections of path are not present" in {
      pathDoesNotExistJson.validate[Option[BigDecimal]] shouldBe JsSuccess(None)
    }

    "result in None without failure when the patch exists but the value does not" in {
      pathExistsAndValueMissingJson.validate[Option[BigDecimal]] shouldBe JsSuccess(None)
    }

    "result in value when path exists" in {
      pathAndValueExistsJson.validate[Option[BigDecimal]] shouldBe JsSuccess(Some(BigDecimal("899.80")), __ \ "rti" \ "balance")
    }
  }
}
