/*
 * Copyright 2015 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.play.http.reads

import play.twirl.api.Html
import uk.gov.hmrc.play.http.HttpResponse

class HtmlHttpReadsSpec extends HttpReadsSpec {
  "HtmlHttpReads.readToHtml" should {
    val reads = HtmlHttpReads.readToHtml

    "convert a successful response body to HTML" in forAll(responsesWith(`2xx`))(theBodyShouldBeConvertedToHtmlBy(reads))
    behave like theStandardErrorHandling(reads)
  }

  "HtmlHttpReads.bodyToHtml" should {
    val reads = HtmlHttpReads.bodyToHtml

    "convert a response body to HTML" in forAll(validResponses)(theBodyShouldBeConvertedToHtmlBy(reads))
  }

  def theBodyShouldBeConvertedToHtmlBy(reads: HttpReads[Html])(response: HttpResponse) {
    reads.read(exampleVerb, exampleUrl, response) should (
      be(an[Html]) and have('text(response.body))
    )
  }
}