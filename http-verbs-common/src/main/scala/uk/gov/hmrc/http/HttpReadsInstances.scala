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
import play.api.libs.json.{JsError, JsResult, JsSuccess}

// Note, we're not doing the readRaw trick - i.e. we expect calls to GET to explicitly specify the type, not defaulting to HttpResponse
trait HttpReadsInstances extends HttpReadsHttpResponse with HttpReadsEither with HttpReadsOption with HttpReadsJson

object HttpReadsInstances extends HttpReadsInstances

import HttpReadsInstances._

trait HttpReadsHttpResponse {
  /** returns the HttpResponse as is - you will be responsible for checking any status codes. */
  implicit val readRaw: HttpReads[HttpResponse] =
    HttpReads.ask.map { case (_, _, response) => response }
}

trait HttpReadsEither {
  implicit val readEither: HttpReads[Either[UpstreamErrorResponse, HttpResponse]] =
    HttpReads.ask.map { case (method, url, response) =>
      HttpErrorFunctions.handleResponseEither(method, url)(response)
    }

  // Note - Either[UpstreamResponse, Option[A]]] will return Right(None) for 204, and Left(Upstream(404)) for 404
  // Option[Either[UpstreamResponse, A]] will return Right(None) for 204, 404, and
  implicit def readEitherOf[A : HttpReads]: HttpReads[Either[UpstreamErrorResponse, A]] =
    readEither.flatMap {
      case Left(err)       => HttpReads.pure(Left(err))
      case Right(response) => HttpReads[A].map(Right.apply)
    }

  def throwOnFailure[A](reads: HttpReads[Either[UpstreamErrorResponse, A]]): HttpReads[A] =
    reads
      .map {
        case Left(err)    => throw err
        case Right(value) => value
      }
}

trait HttpReadsOption {
  // TODO the Option is possibly ambiguous - e.g. would expect None for all failures? (since no other avenue for failure, depending on A..)
  // maybe it should just not be implicit (or add implicit to a scope to be explicitly imported)
  implicit def readOptionOf[A : HttpReads]: HttpReads[Option[A]] =
    HttpReads[HttpResponse]
      .flatMap(_.status match {
        case 204 | 404 => HttpReads.pure(None)
        case _         => HttpReads[A].map(Some.apply) // this delegates error handling to HttpReads[A]
      })
}

trait HttpReadsJson {
  /** Note to read json regardless of error response - can define your own:
    * {{{
    *   HttpReads[HttpResponse].map(_.json.validate[A])
    * }}}
    * or custom behaviour - define your own:
    * {{{
    * HttpReads[HttpResponse].map(response => response.status match {
    *   case 200   => Right(response.body.json.validate[A])
    *   case 400   => Right(response.body.json.validate[A])
    *   case other => Left(s"Invalid status code: \$other")
    * })
    * }}}
    */
  implicit def readFromJsonSafe[A : json.Reads]: HttpReads[Either[UpstreamErrorResponse, JsResult[A]]] =
    HttpReads[Either[UpstreamErrorResponse, HttpResponse]].map(_.right.map(_.json.validate[A]))

  /** variant of [[readFromJsonSafe]] which throws all failures as exceptions */
  implicit def readFromJson[A](implicit rds: json.Reads[A], mf: Manifest[A]): HttpReads[A] =
    throwOnJsonFailure(readFromJsonSafe)

  def throwOnJsonFailure[A](reads: HttpReads[Either[UpstreamErrorResponse, JsResult[A]]])(implicit mf: Manifest[A]): HttpReads[A] =
    reads
      .flatMap {
        case Left(err)                  => throw err
        case Right(JsError(errors))     => HttpReads.ask.map { case (method, url, response) =>
                                             throw new JsValidationException(method, url, mf.runtimeClass, errors.toString)
                                           }
        case Right(JsSuccess(value, _)) => HttpReads.pure(value)
      }
}
