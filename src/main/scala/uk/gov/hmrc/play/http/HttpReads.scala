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

package uk.gov.hmrc.play.http

import play.api.libs.json
import play.api.libs.json.JsValue
import play.twirl.api.Html

trait HttpReads[O] {
  def read(method: String, url: String, response: HttpResponse): O
}
object HttpReads extends HtmlHttpReads with JsonHttpReads {
  // readRaw is brought in like this rather than in a trait as this gives it
  // compilation priority during implicit resolution. This means, unless
  // specified otherwise a verb call will return a plain HttpResponse
  implicit val readRaw: HttpReads[HttpResponse] = RawReads.readRaw

  def apply[O](readF: (String, String, HttpResponse) => O): HttpReads[O] = new HttpReads[O] {
    def read(method: String, url: String, response: HttpResponse) = readF(method, url, response)
  }
}

trait PartialHttpReads[O] {
  def read(method: String, url: String, response: HttpResponse): Option[O]

  def or[P >: O](rds: HttpReads[P]): HttpReads[P] = HttpReads[P] { (method, url, response) =>
    PartialHttpReads.this.read(method, url, response) getOrElse rds.read(method, url, response)
  }

  def or[P >: O](rds: PartialHttpReads[P]): PartialHttpReads[P] = new PartialHttpReads[P] {
    override def read(method: String, url: String, response: HttpResponse) = {
      PartialHttpReads.this.read(method, url, response) orElse rds.read(method, url, response)
    }
  }
}
object PartialHttpReads {
  def apply[O](readF: (String, String, HttpResponse) => Option[O]): PartialHttpReads[O] = new PartialHttpReads[O] {
    def read(method: String, url: String, response: HttpResponse) = readF(method, url, response)
  }
}

trait RawReads extends HttpErrorFunctions {
  implicit val readRaw = HttpReads[HttpResponse] { (method, url, response) => handleResponse(method, url)(response) }
}
object RawReads extends RawReads

trait OptionHttpReads extends HttpErrorFunctions {
  def noneOn(status: Int) = PartialHttpReads[None.type] { (method, url, response) =>
    if (response.status == status) Some(None) else None
  }

  def some[P](implicit rds: HttpReads[P]) = HttpReads[Option[P]] { (method, url, response) =>
    Some(rds.read(method, url, response))
  }

  implicit def readOptionOf[P](implicit rds: HttpReads[P]): HttpReads[Option[P]] = 
    noneOn(status = 204) or noneOn(status = 404) or some[P]
}
object OptionHttpReads extends OptionHttpReads

trait HtmlHttpReads extends HttpErrorFunctions {
  implicit val readToHtml = HttpReads[Html] { (method, url, response) => Html(handleResponse(method, url)(response).body) }
}
object HtmlHttpReads extends HtmlHttpReads

trait JsonHttpReads extends HttpErrorFunctions {
  implicit def readFromJson[O](implicit rds: json.Reads[O], mf: Manifest[O]): HttpReads[O] = HttpReads { (method, url, response) =>
    handleResponse(method, url)(response).json.validate[O].fold(
      errs => throw new JsValidationException(method, url, mf.runtimeClass, errs),
      valid => valid
    )
  }

  def atPath[O](path: String)(implicit rds: HttpReads[O]) = HttpReads[O] { (method, url, response) =>
    rds.read(method, url, new HttpResponse {  // TODO Move this to HttpResponse.copy
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
    emptyOn(204) or emptyOn(404) or atPath(name)(readFromJson[Seq[O]])
}
