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

import play.api.libs.json.{JsValue, JsError, JsResult, JsSuccess, Reads => JsonReads}
import scala.util.{Failure, Success, Try}

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
    * If you need a None for any UpstreamErrorResponse, consider using:
    * {{{
    *  HttpReads[Either[UpstreamErrorResponse, A]].map(_.toOption)
    * }}}
    */
  implicit def readOptionOf[A : HttpReads]: HttpReads[Option[A]] =
    HttpReads[HttpResponse]
      .flatMap(_.status match {
        case 204 | 404 => HttpReads.pure(None)
        case _         => HttpReads[A].map(Some.apply) // this delegates error handling to HttpReads[A]
      })
}

trait HttpReadsJson {
  implicit val readJson: HttpReads[Either[UpstreamErrorResponse, JsValue]] =
    HttpReads[Either[UpstreamErrorResponse, HttpResponse]]
      .map(_.right.map(_.json))

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
  implicit def readJsonWithValidate[A : JsonReads]: HttpReads[Either[UpstreamErrorResponse, JsResult[A]]] =
    HttpReads[Either[UpstreamErrorResponse, JsValue]]
      .map(_.right.map(_.validate[A]))

  implicit def readFromJsonAsTry[A](implicit rds: JsonReads[A], mf: Manifest[A]): HttpReads[Try[A]] =
    HttpReads[Either[UpstreamErrorResponse, JsResult[A]]]
      .flatMap {
        case Left(err)                  => HttpReads.pure(err).map(Failure.apply)
        case Right(JsError(errors))     => HttpReads.ask.map { case (method, url, response) =>
                                             Failure(new JsValidationException(method, url, mf.runtimeClass, errors.toString))
                                           }
        case Right(JsSuccess(value, _)) => HttpReads.pure(value).map(Success.apply)
      }

  /** Variant of [[readFromJsonAsTry]] which throws all failures as exceptions.
    * This is probably the typical instance to use, since all http calls occur within `Future`, allowing recovery.
    */
  implicit def readFromJson[A](implicit rds: JsonReads[A], mf: Manifest[A]): HttpReads[A] =
    readFromJsonAsTry.map(_.get)
}
