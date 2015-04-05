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

import org.scalacheck.Gen
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.Json
import play.twirl.api.Html
import uk.gov.hmrc.play.http.{JsValidationException, HttpErrorFunctions, HttpResponse}

trait HttpReadsSpec extends WordSpec with GeneratorDrivenPropertyChecks with Matchers {
  val exampleVerb = "GET"
  val exampleUrl = "http://example.com/something"
  val exampleBody = "this is the string body"
  val exampleResponse = HttpResponse(
    responseStatus = 0,
    responseJson = Some(Json.parse("""{"test":1}""")),
    responseHeaders = Map("X-something" -> Seq("some value")),
    responseString = Some(exampleBody)
  )

  trait StubThatShouldNotBeCalled extends HttpErrorFunctions {
    override def handleResponse(httpMethod: String, url: String)(response: HttpResponse) = {
      fail("called handleResponse when not expected to")
    }
  }

  trait StubThatReturnsTheResponse extends HttpErrorFunctions {
    override def handleResponse(httpMethod: String, url: String)(response: HttpResponse) = response
  }

  trait StubThatThrowsAnException extends HttpErrorFunctions {
    override def handleResponse(httpMethod: String, url: String)(response: HttpResponse) = throw new Exception
  }
}
case class Example(v1: String, v2: Int)
