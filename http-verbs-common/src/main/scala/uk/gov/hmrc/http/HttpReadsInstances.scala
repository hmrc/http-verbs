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

// Note, we're not doing the readRaw trick - we expect calls to GET to explicitly specify the type, not defaulting to HttpResponse
trait HttpReadsInstances extends HttpReadsHttpResponse with HttpReadsEither with HttpReadsOption with HttpReadsJson

object HttpReadsInstances extends HttpReadsInstances

import HttpReadsInstances._

trait HttpReadsHttpResponse {
  /** returns the HttpResponse as is - you will be responsible for checking any status codes. */
  implicit val readRaw: HttpReads[HttpResponse] =
    HttpReads.ask.map { case (_, _, response) => response }
}

trait HttpReadsEither {
  implicit def readEitherOf[A : HttpReads]: HttpReads[Either[UpstreamErrorResponse, A]] =
    HttpReads.ask.flatMap { case (method, url, response) =>
      HttpErrorFunctions.handleResponseEither(method, url)(response) match {
        case Left(err)       => HttpReads.pure(Left(err))
        case Right(response) => HttpReads[A].map(Right.apply)
      }
    }

  def throwOnFailure[A](reads: HttpReads[Either[UpstreamErrorResponse, A]]): HttpReads[A] =
    reads
      .map {
        case Left(err)    => throw err
        case Right(value) => value
      }
}

trait HttpReadsOption {
  /** An opinionated HttpReads which returns None for 404/204.
    * If you need a None for any UpstreamErrorResponse, consider using `HttpReads[Either[UpstreamErrorResponse, A]].map(_.toOption)`
    */
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
  // is this awkward? Should we have a union of UpstreamErrorResponse and JsValidationException
  // e.g. HttpReads[Either[UpstreamErrorResponse or JsValidationException, A]]
  // HttpReads[Either[Throwable, A]] too broad?
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
