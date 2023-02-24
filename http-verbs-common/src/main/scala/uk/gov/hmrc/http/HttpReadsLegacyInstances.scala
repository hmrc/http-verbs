/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.libs.json.{JsNull, JsValue, Reads}


trait HttpReadsLegacyInstances extends HttpReadsLegacyOption with HttpReadsLegacyJson

trait HttpReadsLegacyRawReads extends HttpErrorFunctions {
  @deprecated("Use uk.gov.hmrc.http.HttpReads.Implicits instead. See README for differences.", "11.0.0")
  implicit val readRaw: HttpReads[HttpResponse] =
    (method: String, url: String, response: HttpResponse) =>
      handleResponse(method, url)(response)
}

object HttpReadsLegacyRawReads extends HttpReadsLegacyRawReads

trait HttpReadsLegacyOption extends HttpErrorFunctions {
  @deprecated("Use uk.gov.hmrc.http.HttpReads.Implicits instead. See README for differences.", "11.0.0")
  implicit def readOptionOf[P](implicit rds: HttpReads[P]): HttpReads[Option[P]] =
    (method: String, url: String, response: HttpResponse) =>
      response.status match {
        case 204 | 404 => None
        case _         => Some(rds.read(method, url, response))
      }
}

trait HttpReadsLegacyJson extends HttpErrorFunctions {
  @deprecated("Use uk.gov.hmrc.http.HttpReads.Implicits instead. See README for differences.", "11.0.0")
  implicit def readFromJson[O](implicit rds: Reads[O], mf: Manifest[O]): HttpReads[O] =
    (method: String, url: String, response: HttpResponse) =>
      readJson(method, url, handleResponse(method, url)(response).json)

  @deprecated("Use uk.gov.hmrc.http.HttpReads.Implicits instead. See README for differences.", "11.0.0")
  def readSeqFromJsonProperty[O](name: String)(implicit rds: Reads[O], mf: Manifest[O]): HttpReads[Seq[O]] =
    (method: String, url: String, response: HttpResponse) =>
      response.status match {
        case 204 | 404 => Seq.empty
        case _ =>
          readJson[Seq[O]](method, url, (handleResponse(method, url)(response).json \ name).getOrElse(JsNull)) //Added JsNull here to force validate to fail - replicates existing behaviour
      }

  private def readJson[A](method: String, url: String, jsValue: JsValue)(implicit rds: Reads[A], mf: Manifest[A]): A =
    jsValue
      .validate[A]
      .fold(
        errs => throw new JsValidationException(method, url, mf.runtimeClass, errs.toString()),
        valid => valid
      )
}
