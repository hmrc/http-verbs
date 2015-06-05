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

package uk.gov.hmrc.play.http.reads

import org.scalacheck.Gen

class ErrorHttpReadsSpec extends HttpReadsSpec {
  "ErrorReads.convertFailuresToExceptions" should {
    val reads = ErrorHttpReads.convertFailuresToExceptions
    behave like theStandardErrorHandling (reads or failTheTest)
    behave like aPassthroughForSuccessCodes (reads)
  }
  "ErrorReads.convert400ToBadRequest" should {
    val reads = ErrorHttpReads.convert400ToBadRequest

    behave like theStandardErrorHandlingFor400 (reads or failTheTest)
    "pass through all other responses" in
      forAll(validResponses.suchThat(_.status != 400))(theResponseShouldBePassedThroughBy(reads))
  }
  "ErrorReads.convert404ToNotFound" should {
    val reads = ErrorHttpReads.convert404ToNotFound

    behave like theStandardErrorHandlingFor404 (reads or failTheTest)
    "pass through all other responses" in
      forAll(validResponses.suchThat(_.status != 404))(theResponseShouldBePassedThroughBy(reads))
  }
  "ErrorReads.convert4xxToUpstream4xxResponse" should {
    val reads = ErrorHttpReads.convert4xxToUpstream4xxResponse

    behave like theStandardErrorHandlingFor4xxCodes (reads or failTheTest)
    "pass through all other responses" in
      forAll(responsesWith(statusCodes = Gen.oneOf(invalidCodes, `1xx`, `2xx`, `5xx`)))(theResponseShouldBePassedThroughBy(reads))
  }
  "ErrorReads.convert5xxToUpstream5xxResponse" should {
    val reads = ErrorHttpReads.convert5xxToUpstream5xxResponse

    behave like theStandardErrorHandlingFor5xxCodes (reads or failTheTest)
    "pass through all other responses" in
      forAll(responsesWith(statusCodes = Gen.oneOf(invalidCodes, `1xx`, `2xx`, `4xx`)))(theResponseShouldBePassedThroughBy(reads))
  }
  "ErrorReads.convertLessThan200GreaterThan599ToException" should {
    val reads = ErrorHttpReads.convertLessThan200GreaterThan599ToException

    behave like theStandardErrorHandlingFor1xxCodes (reads or failTheTest)
    behave like theStandardErrorHandlingForInvalidCodes (reads or failTheTest)
    "pass through all other responses" in
      forAll(responsesWith(statusCodes = Gen.oneOf(`2xx`, `4xx`, `5xx`)))(theResponseShouldBePassedThroughBy(reads))
  }
}
