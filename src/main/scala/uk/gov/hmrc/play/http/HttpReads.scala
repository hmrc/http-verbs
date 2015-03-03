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

package uk.gov.hmrc.play.http

import play.api.libs.json
import play.api.libs.json.JsValue
import play.twirl.api.Html

trait HttpReads[O] {
  def read(method: String, url: String, response: HttpResponse): O
}

object HttpReads extends HttpErrorFunctions {
  implicit def readOptionOf[P](implicit rds: HttpReads[P]): HttpReads[Option[P]] = new HttpReads[Option[P]] {
    def read(method: String, url: String, response: HttpResponse) = response.status match {
      case 204 | 404 => None
      case _ => Some(rds.read(method, url, response))
    }
  }

  implicit val readToHtml: HttpReads[Html] = new HttpReads[Html] {
    def read(method: String, url: String, response: HttpResponse) = Html(handleResponse(method, url)(response).body)
  }

  implicit def readFromJson[O](implicit rds: json.Reads[O], mf: Manifest[O]): HttpReads[O] = new HttpReads[O] {
    def read(method: String, url: String, response: HttpResponse) = readJson(method, url, handleResponse(method, url)(response).json)
  }

  implicit val readRaw: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
    def read(method: String, url: String, response: HttpResponse) = handleResponse(method, url)(response)
  }

  def readSeqFromJsonProperty[O](name: String)(implicit rds: json.Reads[O], mf: Manifest[O]) = new HttpReads[Seq[O]] {
    def read(method: String, url: String, response: HttpResponse) = response.status match {
      case 204 | 404 => Seq.empty
      case _ => readJson[Seq[O]](method, url, handleResponse(method, url)(response).json \ name)
    }
  }

  protected[http] def readJson[A](method: String, url: String, jsValue: JsValue)(implicit rds: json.Reads[A], mf: Manifest[A]) = {
    jsValue.validate[A].fold(
      errs => throw new JsValidationException(method, url, mf.runtimeClass, errs),
      valid => valid
    )
  }
}
