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

import play.api.libs.json
import play.api.libs.json.{JsNull, JsValue}

object HttpReads extends OptionHttpReads with EitherHttpReads with JsonHttpReads {
  // readRaw is brought in like this rather than in a trait as this gives it
  // compilation priority during implicit resolution. This means, unless
  // specified otherwise a verb call will return a plain HttpResponse
  implicit val readRaw: HttpReads[HttpResponse] = RawReads.readRaw
}

trait HttpReads[O] {
  def read(method: String, url: String, response: HttpResponse): O
}

trait RawReads {
  implicit val readRaw: HttpReads[HttpResponse] =
    new HttpReads[HttpResponse] {
      def read(method: String, url: String, response: HttpResponse) =
        response
    }
}

object RawReads extends RawReads

trait OptionHttpReads {
  implicit def readOptionOf[P](implicit rds: HttpReads[P]): HttpReads[Option[P]] = new HttpReads[Option[P]] {
    def read(method: String, url: String, response: HttpResponse) = response.status match {
      case 204 | 404 => None
      case _         => Some(rds.read(method, url, response))
    }
  }
}

trait EitherHttpReads {
   // Note - Either[UpstreamResponse, Option[A]]] will return Right(None) for 204, and Left(Upstream(404)) for 404
   // the type is ambiguous (2 ways to represent 404) - to return Right(None) for 404, we'd have to delegate to rds, to see
   // if what errors can be handled first, then recover?
   // Or just provide an instance of `Either[UpstreamResponse, Option[A]]]`
  implicit def readEitherOf[P](implicit rds: HttpReads[P]): HttpReads[Either[UpstreamErrorResponse, P]] =
    new HttpReads[Either[UpstreamErrorResponse, P]] {
      def read(method: String, url: String, response: HttpResponse) =
        HttpErrorFunctions.handleResponseEither(method, url)(response)
          .right.map(rds.read(method, url, _))
    }
}

trait JsonHttpReads {
  import HttpErrorFunctions.handleResponse

  implicit def readFromJson[O](implicit rds: json.Reads[O], mf: Manifest[O]): HttpReads[O] = new HttpReads[O] {
    def read(method: String, url: String, response: HttpResponse) =
      readJson(method, url, handleResponse(method, url)(response).json)
  }

  def readSeqFromJsonProperty[O](name: String)(implicit rds: json.Reads[O], mf: Manifest[O]) = new HttpReads[Seq[O]] {
    def read(method: String, url: String, response: HttpResponse) = response.status match {
      case 204 | 404 => Seq.empty
      case _ =>
        readJson[Seq[O]](method, url, (handleResponse(method, url)(response).json \ name).getOrElse(JsNull)) //Added JsNull here to force validate to fail - replicates existing behaviour
    }
  }

  private def readJson[A](method: String, url: String, jsValue: JsValue)(implicit rds: json.Reads[A], mf: Manifest[A]) =
    jsValue
      .validate[A]
      .fold(
        errs => throw new JsValidationException(method, url, mf.runtimeClass, errs.toString()),
        valid => valid
      )
}
