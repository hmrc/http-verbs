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

import play.api.libs.json
import uk.gov.hmrc.play.http.{HttpErrorFunctions, JsValidationException, HttpResponse}

trait JsonHttpReads extends HttpErrorFunctions {
  implicit def readFromJson[O](implicit rds: json.Reads[O], mf: Manifest[O]): HttpReads[O] = HttpReads { (method, url, response) =>
    handleResponse(method, url)(response).json.validate[O].fold(
      errs => throw new JsValidationException(method, url, mf.runtimeClass, errs),
      valid => valid
    )
  }

  def atPath[O](path: String)(implicit rds: HttpReads[O]) = HttpReads[O] { (method, url, response) =>
    rds.read(method, url, new HttpResponse {
      // TODO Move this to HttpResponse.copy
      override def allHeaders = response.allHeaders
      override def header(key: String) = response.header(key)
      override def status = response.status
      override def json = response.json \ path
      override def body = response.body
    })
  }

  def emptyOn(status: Int) = PartialHttpReads[Seq[Nothing]] { (method, url, response) =>
    if (response.status == status) Some(Seq.empty) else None
  }

  def readSeqFromJsonProperty[O](name: String)(implicit rds: json.Reads[O], mf: Manifest[O]) =
    PartialHttpReads.byStatus { case 204 | 404 => Seq.empty } or atPath(name)(readFromJson[Seq[O]])
}
object JsonHttpReads extends JsonHttpReads
