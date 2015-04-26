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

class BackwardsCompatibleReadsRecipesSpec extends HttpReadsSpec {
  "BackwardsCompatibleReadsRecipes.readRaw" should {
    val reads = BackwardsCompatibleReadsRecipes.readRaw

    "return the bare response if returned" in
      forAll(responsesWith(`2xx`))(theBareResponseShouldBeReturnedBy(reads))

    behave like theStandardErrorHandling (reads)
  }
  "BackwardsCompatibleReadsRecipes.readFromJson" should {
    val reads = BackwardsCompatibleReadsRecipes.readFromJson[Example]

    "convert a successful response body to the given class" in
      forAll (responsesWith(`2xx`, bodyAsJson = exampleJsonObj))(aExampleClassShouldBeDeserialisedBy(reads))
    "convert a successful response body with json that doesn't validate into an exception" in
      forAll (responsesWith(`2xx`, bodyAsJson = brokenJsonObj))(expectAJsValidationExceptionFrom(reads))

    behave like theStandardErrorHandling (reads)
  }
  "BackwardsCompatibleReadsRecipes.readToHtml" should {
    val reads = BackwardsCompatibleReadsRecipes.readToHtml

    "convert a successful response body to HTML" in forAll(responsesWith(`2xx`))(theBodyShouldBeConvertedToHtmlBy(reads))
    behave like theStandardErrorHandling(reads)
  }
}
