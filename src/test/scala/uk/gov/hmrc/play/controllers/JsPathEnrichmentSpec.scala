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

package uk.gov.hmrc.play.controllers

import org.scalatest.{Matchers, WordSpecLike}
import play.api.libs.json._

class JsPathEnrichmentSpec extends WordSpecLike with Matchers {

  "Parsing json when the path does not exist prior to the structure being parsed" should {

    "result in None without failure when early sections of path are not present" in new Setup {
      pathDoesNotExistJson.validate[Option[BigDecimal]] shouldBe JsSuccess(None)
    }

    "result in None without failure when the patch exists but the value does not" in new Setup {
      pathDoesNotExistJson.validate[Option[BigDecimal]] shouldBe JsSuccess(None)
    }

    "result in value when path exists" in new Setup {
      pathAndValueExistsJson.validate[Option[BigDecimal]] match {
        case s: JsSuccess[Option[BigDecimal]] => s.value shouldBe Some(BigDecimal("899.80"))
        case e: JsError => fail(s"Should have parsed bigDecimal, failed with $e")
      }
    }
  }

  class Setup {

    import JsPathEnrichment.RichJsPath

    implicit val reads: Reads[Option[BigDecimal]] = (JsPath \ "rti" \ "balance").tolerantReadNullable[BigDecimal]

    val pathDoesNotExistJson = Json.parse(
      """
        |{
        |	"nonRti": {
        |		"paidToDate": 200.25
        |	}
        |}
      """.stripMargin)

    val pathExistsAndValueMissingJson = Json.parse(
      """
        |{
        |	"rti": {
        |		"notTheBalance": 123.45
        |	}
        |}
      """.stripMargin)

    val pathAndValueExistsJson = Json.parse(
      """
        |{
        |	"rti": {
        |		"balance": 899.80
        |	}
        |}
      """.stripMargin)
  }
}
