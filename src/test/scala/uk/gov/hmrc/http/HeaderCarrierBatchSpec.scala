/*
 * Copyright 2020 HM Revenue & Customs
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

import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{JsString, Json}

class HeaderCarrierBatchSpec extends WordSpec with Matchers {
  "HeaderCarrierBatch" should {

    "give sessionIDs a batch- prefix" in {
      HeaderCarrierBatch().createFirstCallHeaderCarrier.sessionId.get.value should startWith("batch-")
    }

    "give item requestIDs a batch-item- prefix" in {
      HeaderCarrierBatch().createItemHeaderCarrier.requestId.get.value should startWith("batch-item-")
    }

    "give first call requestIDs a batch-start- prefix" in {
      HeaderCarrierBatch().createFirstCallHeaderCarrier.requestId.get.value should startWith("batch-start-")
    }

    "give single call requestIDs a batch-single- prefix" in {
      HeaderCarrierBatch().createSingleHeaderCarrier.requestId.get.value should startWith("batch-single-")
    }

    "create a fresh sessionID each time" in {
      HeaderCarrierBatch().createFirstCallHeaderCarrier.sessionId should not equal
        HeaderCarrierBatch().createFirstCallHeaderCarrier.sessionId
    }

    "create a fresh requestID for each item" in {
      val hcb = HeaderCarrierBatch()
      val requestIDs = (1 to 3).map { i => hcb.createItemHeaderCarrier }
      requestIDs.toSet.size shouldBe requestIDs.size
    }

  }
}