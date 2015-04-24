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

trait HttpReads[O] {
  def read(method: String, url: String, response: HttpResponse): O
}
object HttpReads extends HtmlHttpReads with JsonHttpReads {
  // readRaw is brought in like this rather than in a trait as this gives it
  // compilation priority during implicit resolution. This means, unless
  // specified otherwise a verb call will return a plain HttpResponse
  implicit val readRaw: HttpReads[HttpResponse] = RawHttpReads.readRaw

  def apply[O](readF: (String, String, HttpResponse) => O): HttpReads[O] = new HttpReads[O] {
    def read(method: String, url: String, response: HttpResponse) = readF(method, url, response)
  }
  def always[O](const: O): HttpReads[O] = HttpReads((_,_,_) => const)
}