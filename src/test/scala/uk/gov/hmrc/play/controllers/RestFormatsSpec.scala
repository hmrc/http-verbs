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

import org.joda.time.{DateTime, DateTimeZone, LocalDate, LocalDateTime}
import org.scalatest.{Matchers, WordSpecLike}
import play.api.libs.json.{JsSuccess, _}

class RestFormatsSpec extends WordSpecLike with Matchers {
  "localDateTimeRead" should {
    "return a LocalDateTime for correctly formatted JsString" in {
      val testDate = new LocalDateTime(0)
      val jsValue = RestFormats.localDateTimeWrite.writes(testDate)

      val JsSuccess(result, _) = RestFormats.localDateTimeRead.reads(jsValue)
      result shouldBe testDate
    }

    "return a JsError for a json value that is not a JsString" in {
      RestFormats.localDateTimeRead.reads(Json.obj()) shouldBe a[JsError]
    }

    "return a JsError for a JsString that is not a well-formatted date" in {
      RestFormats.localDateTimeRead.reads(JsString("not a valid date")) shouldBe a[JsError]
    }
  }

  "dateTimeRead" should {
    "return a DateTime in zone UTC for correctly formatted JsString" in {
      val testDate = new DateTime(0)
      val jsValue = RestFormats.dateTimeWrite.writes(testDate)

      val JsSuccess(result, _) = RestFormats.dateTimeRead.reads(jsValue)
      result shouldBe testDate.withZone(DateTimeZone.UTC)
    }

    "return a JsError for a json value that is not a JsString" in {
      RestFormats.dateTimeRead.reads(Json.obj()) shouldBe a[JsError]
    }

    "return a JsError for a JsString that is not a well-formatted date" in {
      RestFormats.dateTimeRead.reads(JsString("not a valid date")) shouldBe a[JsError]
    }
  }

  "localDateRead" should {
    "return a LocalDate in zone UTC for correctly formatted JsString" in {
      val json = JsString("1994-05-01")
      val expectedDate = new LocalDate(1994, 5, 1)

      val JsSuccess(result, _) =RestFormats.localDateRead.reads(json)
      result shouldBe expectedDate
    }

    "return a JsError for a json value that is not a JsString" in {
      RestFormats.localDateRead.reads(Json.obj()) shouldBe a[JsError]
    }

    "return a JsError for a JsString that is not a well-formatted date" in {
      RestFormats.localDateRead.reads(JsString("not a valid date")) shouldBe a[JsError]
    }

    "return a JsError for a JsString that is well formatted but has bad values" in {
      RestFormats.localDateRead.reads(JsString("1994-13-32")) shouldBe a[JsError]
    }
  }
}
