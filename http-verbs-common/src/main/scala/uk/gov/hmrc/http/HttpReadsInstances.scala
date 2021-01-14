/*
 * Copyright 2021 HM Revenue & Customs
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
import scala.util.Try

trait HttpReadsInstances
  extends HttpReadsHttpResponse
     with HttpReadsEither
     with HttpReadsTry
     with HttpReadsOption
     with HttpReadsJson
     with LowPriorityHttpReadsJson

object HttpReadsInstances extends HttpReadsInstances

import HttpReadsInstances._

trait HttpReadsHttpResponse {
  /** returns the HttpResponse as is - you will be responsible for checking any status codes. */
  implicit val readRaw: HttpReads[HttpResponse] =
    HttpReads.ask.map { case (_, _, response) => response }

  /** Ignores the response and returns Unit - useful for handling 204 etc.
    * It can be combined with error handling types (e.g. `Either[UpstreamErrorResponse, Unit]`)
    */
  implicit val readUnit: HttpReads[Unit] =
    HttpReads.pure(())
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

trait HttpReadsTry {
  implicit def readTryOf[A : HttpReads]: HttpReads[Try[A]] =
    new HttpReads[Try[A]] {
      def read(method: String, url: String, response: HttpResponse): Try[A] =
        Try(HttpReads[A].read(method, url, response))
    }
}

trait HttpReadsOption {
  /** An opinionated HttpReads which returns None for 404.
    * This does not have any special treatment for 204, as did the previous version.
    * If you need a None for any UpstreamErrorResponse, consider using:
    * {{{
    *  HttpReads[Either[UpstreamErrorResponse, A]].map(_.toOption)
    * }}}
    */
  implicit def readOptionOfNotFound[A : HttpReads]: HttpReads[Option[A]] =
    HttpReads[HttpResponse]
      .flatMap(_.status match {
        case 404 => HttpReads.pure(None)
        case _   => HttpReads[A].map(Some.apply) // this delegates error handling to HttpReads[A]
      })
}

trait HttpReadsJson {
  implicit val readJsValue: HttpReads[JsValue] =
    HttpReads[HttpResponse]
      .map(_.json)

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
  implicit def readJsResult[A : JsonReads]: HttpReads[JsResult[A]] =
    HttpReads[JsValue]
      .map(_.validate[A])
}

trait LowPriorityHttpReadsJson {

  /** This is probably the typical instance to use, since all http calls occur within `Future`, allowing recovery.
    */
  @throws(classOf[UpstreamErrorResponse])
  @throws(classOf[JsValidationException])
  implicit def readFromJson[A](implicit rds: JsonReads[A], mf: Manifest[A]): HttpReads[A] =
    HttpReads[Either[UpstreamErrorResponse, JsResult[A]]]
      .flatMap {
        case Left(err)                  => throw err
        case Right(JsError(errors))     => HttpReads.ask.map { case (method, url, response) =>
                                             throw new JsValidationException(method, url, mf.runtimeClass, errors.toString)
                                           }
        case Right(JsSuccess(value, _)) => HttpReads.pure(value)
      }
}
