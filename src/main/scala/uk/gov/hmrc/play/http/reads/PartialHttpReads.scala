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

import uk.gov.hmrc.play.http.HttpResponse

object PartialHttpReads {
  def apply[O](readF: (String, String, HttpResponse) => Option[O]): PartialHttpReads[O] = new PartialHttpReads[O] {
    def read(method: String, url: String, response: HttpResponse) = readF(method, url, response)
  }

  def byStatus[O](statusF: PartialFunction[Int, O]) = PartialHttpReads[O] { (method, url, response) =>
    statusF.lift(response.status)
  }
  def onStatus[O](status: Int): ByStatusBuilder[O] = new ByStatusBuilder(statusMatcher = _ == status)
  def onStatus[O](statusRange: Range): ByStatusBuilder[O] = new ByStatusBuilder[O](statusRange contains _)
  def onStatus[O](statusMatcher: Int => Boolean): ByStatusBuilder[O] = new ByStatusBuilder[O](statusMatcher)

  class ByStatusBuilder[O](statusMatcher: Int => Boolean) {
    def apply(possibleRds: (String, String, HttpResponse) => Option[O]): PartialHttpReads[O] = apply(PartialHttpReads(possibleRds))

    def apply(possibleRds: PartialHttpReads[O]): PartialHttpReads[O] = PartialHttpReads[O] { (method, url, response) =>
      if (statusMatcher(response.status)) possibleRds.read(method, url, response) else None
    }

    def apply(possibleRds: HttpReads[O]): PartialHttpReads[O] = PartialHttpReads[O] { (method, url, response) =>
      if (statusMatcher(response.status)) Some(possibleRds.read(method, url, response)) else None
    }
  }
}

trait PartialHttpReads[+O] {
  def read(method: String, url: String, response: HttpResponse): Option[O]

  def or[P >: O](rds: HttpReads[P]) = HttpReads[P] { (method, url, response) =>
    PartialHttpReads.this.read(method, url, response) getOrElse rds.read(method, url, response)
  }

  def or[P >: O](rds: PartialHttpReads[P]) = PartialHttpReads[P] { (method, url, response) =>
    PartialHttpReads.this.read(method, url, response) orElse rds.read(method, url, response)
  }
}
