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
import uk.gov.hmrc.play.http.{JsValidationException, HttpResponse}

trait JsonHttpReads {
  implicit def readFromJson[O](implicit rds: json.Reads[O], mf: Manifest[O]): HttpReads[O] =
    ErrorHttpReads.convertFailuresToExceptions or jsonBodyDeserialisedTo[O]

  def jsonBodyDeserialisedTo[O](implicit rds: json.Reads[O], mf: Manifest[O]) = HttpReads[O] { (method, url, response) =>
    response.json.validate[O].fold(
      errs => throw new JsValidationException(method, url, mf.runtimeClass, errs),
      valid => valid
    )
  }

  def atPath[O](path: String)(implicit rds: HttpReads[O]) = HttpReads[O] { (method, url, response) =>
    rds.read(method, url, HttpResponse(
      responseStatus = response.status,
      responseHeaders = response.allHeaders,
      responseJson = Some(response.json \ path)
    ))
  }

  def emptyOn(status: Int) = PartialHttpReads[Seq[Nothing]] { (method, url, response) =>
    if (response.status == status) Some(Seq.empty) else None
  }

  def readSeqFromJsonProperty[O](name: String)(implicit rds: json.Reads[O], mf: Manifest[O]): HttpReads[Seq[O]] = {
    import ErrorHttpReads._
    emptyOn(204) or
    emptyOn(404) or
    convert400ToBadRequest or
    convert4xxToUpstream4xxResponse or
    convert5xxToUpstream5xxResponse or
    convertLessThan200GreaterThan599ToException or
    atPath(name)(jsonBodyDeserialisedTo[Seq[O]])
  }
}
object JsonHttpReads extends JsonHttpReads
