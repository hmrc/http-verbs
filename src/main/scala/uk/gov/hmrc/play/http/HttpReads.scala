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
trait PartialHttpReads[O] {
  def read(method: String, url: String, response: HttpResponse): Option[O]

  def or[P >: O](rds: HttpReads[P]): HttpReads[P] = new HttpReads[P] {
    override def read(method: String, url: String, response: HttpResponse): P = {
      PartialHttpReads.this.read(method, url, response) getOrElse rds.read(method, url, response)
    }
  }

  def or[P >: O](rds: PartialHttpReads[P]): PartialHttpReads[P] = new PartialHttpReads[P] {
    override def read(method: String, url: String, response: HttpResponse) = {
      PartialHttpReads.this.read(method, url, response) orElse rds.read(method, url, response)
    }
  }
}

object HttpReads extends HtmlHttpReads with JsonHttpReads {
  // readRaw is brought in like this rather than in a trait as this gives it
  // compilation priority during implicit resolution. This means, unless
  // specified otherwise a verb call will return a plain HttpResponse
  implicit val readRaw: HttpReads[HttpResponse] = RawReads.readRaw
}

trait RawReads extends HttpErrorFunctions {
  implicit val readRaw: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
    def read(method: String, url: String, response: HttpResponse) = handleResponse(method, url)(response)
  }
}
object RawReads extends RawReads

trait OptionHttpReads extends HttpErrorFunctions {
  def noneOn(status: Int): PartialHttpReads[None.type] = new PartialHttpReads[None.type] {
    override def read(method: String, url: String, response: HttpResponse) =
      if (response.status == status) Some(None) else None
  }

  def alwaysSome[P](implicit rds: HttpReads[P]): HttpReads[Option[P]] = new HttpReads[Option[P]] {
    def read(method: String, url: String, response: HttpResponse) = Some(rds.read(method, url, response))
  }

  implicit def readOptionOf[P](implicit rds: HttpReads[P]): HttpReads[Option[P]] = 
    noneOn(status = 204) or noneOn(status = 404) or alwaysSome[P]
}
object OptionHttpReads extends OptionHttpReads

trait HtmlHttpReads extends HttpErrorFunctions {
  implicit val readToHtml: HttpReads[Html] = new HttpReads[Html] {
    def read(method: String, url: String, response: HttpResponse) = Html(handleResponse(method, url)(response).body)
  }
}
object HtmlHttpReads extends HtmlHttpReads

trait JsonHttpReads extends HttpErrorFunctions {
  implicit def readFromJson[O](implicit rds: json.Reads[O], mf: Manifest[O]): HttpReads[O] = new HttpReads[O] {
    def read(method: String, url: String, response: HttpResponse) = readJson(method, url, handleResponse(method, url)(response).json)
  }

  def atPath[O](path: String)(implicit rds: HttpReads[O]): HttpReads[O] = new HttpReads[O] {
    override def read(method: String, url: String, response: HttpResponse) = rds.read(method, url, new HttpResponse {
      override def allHeaders = response.allHeaders
      override def header(key: String) = response.header(key)
      override def status = response.status
      override def json = response.json \ path
      override def body = response.body
    })
  }

  def emptyOn(status: Int): PartialHttpReads[Seq[Nothing]] = new PartialHttpReads[Seq[Nothing]] {
    override def read(method: String, url: String, response: HttpResponse) =
      if (response.status == status) Some(Seq.empty) else None
  }

  def readSeqFromJsonProperty[O](name: String)(implicit rds: json.Reads[O], mf: Manifest[O]) =
    emptyOn(204) or emptyOn(404) or atPath(name)(readFromJson[Seq[O]])

  private def readJson[A](method: String, url: String, jsValue: JsValue)(implicit rds: json.Reads[A], mf: Manifest[A]) = {
    jsValue.validate[A].fold(
      errs => throw new JsValidationException(method, url, mf.runtimeClass, errs),
      valid => valid
    )
  }
}
