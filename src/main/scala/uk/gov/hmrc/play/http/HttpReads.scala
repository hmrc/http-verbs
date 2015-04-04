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

trait PartialHttpReads[+O] {
  def read(method: String, url: String, response: HttpResponse): Option[O]

  def or[P >: O](rds: HttpReads[P]): HttpReads[P] = HttpReads[P] { (method, url, response) =>
    PartialHttpReads.this.read(method, url, response) getOrElse rds.read(method, url, response)
  }

  def or[P >: O](rds: PartialHttpReads[P]): PartialHttpReads[P] = PartialHttpReads[P] { (method, url, response) =>
    PartialHttpReads.this.read(method, url, response) orElse rds.read(method, url, response)
  }
}
object PartialHttpReads {
  def apply[O](readF: (String, String, HttpResponse) => Option[O]): PartialHttpReads[O] = new PartialHttpReads[O] {
    def read(method: String, url: String, response: HttpResponse) = readF(method, url, response)
  }
  def byStatus[O](statusF: PartialFunction[Int, O]): PartialHttpReads[O] = new PartialHttpReads[O] {
    def read(method: String, url: String, response: HttpResponse) = statusF.lift(response.status)
  }
}

trait RawReads extends HttpErrorFunctions {
  implicit val readRaw = HttpReads[HttpResponse] { (method, url, response) => handleResponse(method, url)(response) }
}
object RawReads extends RawReads

trait ErrorReads extends HttpErrorFunctions {
  def convertFailuresToExceptions: PartialHttpReads[Nothing] =
    convert400ToBadRequest or
    convert404ToNotFound or
    convert4xxToUpstream4xxResponse or
    convert5xxToUpstream5xxResponse or
    convertLessThan200GreaterThan599ToException

  // TODO not sure that this is a good thing. should be generalised out or more strictly typed
  private def convert(statusMatches: Int => Boolean)(f: (String, String, HttpResponse) => Exception): PartialHttpReads[Nothing] = PartialHttpReads { (method, url, response) =>
    if (statusMatches(response.status)) throw f(method, url, response) else None
  }
  def convert400ToBadRequest = convert(_ == 400) { (m, u, r) => new BadRequestException(s"$m of '$u' returned 400 (Bad Request). Response body '${r.body}'") }
  def convert404ToNotFound = convert(_ == 404) { (m, u, r) => new NotFoundException(s"$m of '$u' returned 404 (Not Found). Response body: '${r.body}'") }
  def convert4xxToUpstream4xxResponse = convert(400 to 499 contains _) { (m, u, r) =>
    new Upstream4xxResponse(s"$m of '$u' returned ${r.status}. Response body: '${r.body}'", r.status, 500, r.allHeaders)
  }
  def convert5xxToUpstream5xxResponse = convert(500 to 599 contains _) { (m, u, r) =>
    new Upstream5xxResponse(s"$m of '$u' returned ${r.status}. Response body: '${r.body}'", r.status, 502)
  }
  def convertLessThan200GreaterThan599ToException = convert(status => status < 200 || status >= 600) { (m, u, r) =>
    new Exception(s"$m to $u failed with status ${r.status}. Response body: '${r.body}'")
  }
}
object ErrorReads extends ErrorReads

trait OptionHttpReads extends HttpErrorFunctions {
  def noneOn(status: Int) = PartialHttpReads[None.type] { (method, url, response) =>
    if (response.status == status) Some(None) else None
  }

  def some[P](implicit rds: HttpReads[P]) = HttpReads[Option[P]] { (method, url, response) =>
    Some(rds.read(method, url, response))
  }

  implicit def readOptionOf[P](implicit rds: HttpReads[P]): HttpReads[Option[P]] =
    PartialHttpReads.byStatus { case 204 | 404 => None } or some[P]
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
    PartialHttpReads.byStatus { case 204 | 404 => Seq.empty } or atPath(name)(readFromJson[Seq[O]])
}
