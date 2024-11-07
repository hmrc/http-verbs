/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.http.logging

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

class UrlSanitiserSpec
  extends AnyWordSpec
     with Matchers {

  "UrlSanitiser.sanitiseForLogging" should {
    "sanitise url" in {
      UrlSanitiser.sanitiseForLogging("https://user:password@host:1234/p1/p2?q1=asd&q2=dsa") shouldBe "https://<<UserInfo>>@host:1234/p1/p2"
    }

    "include query params if requested" in {
      UrlSanitiser.sanitiseForLogging("https://user:password@host:1234/p1/p2?q1=asd&q2=dsa", includeQueryParams = true) shouldBe "https://<<UserInfo>>@host:1234/p1/p2?q1=asd&q2=dsa"
    }

    "handle missing elements" in {
      UrlSanitiser.sanitiseForLogging("https://host/p1/p2", includeQueryParams = true) shouldBe "https://host/p1/p2"
    }
  }
}
