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
import play.api.libs.json.{JsError, JsLookupResult, JsNull, JsResult, JsSuccess, JsValue}

object HttpReads extends EitherHttpReads with OptionHttpReads with JsonHttpReads {
  // readRaw is brought in like this rather than in a trait as this gives it
  // compilation priority during implicit resolution. This means, unless
  // specified otherwise a verb call will return a plain HttpResponse
  implicit val readRaw: HttpReads[HttpResponse] = RawReads.readRaw

  def apply[A : HttpReads] =
    implicitly[HttpReads[A]]

  def pure[A](a: A) =
    new HttpReads[A] {
      def read(method: String, url: String, response: HttpResponse): A =
        a
    }

  // i.e. HttpReads[A] = Reader[(Method, Url, HttpResponse), A]
  def ask: HttpReads[(String, String, HttpResponse)] =
    new HttpReads[(String, String, HttpResponse)] {
      def read(method: String, url: String, response: HttpResponse): (String, String, HttpResponse) =
        (method, url, response)
    }
}

trait HttpReads[A] {
  outer =>

  def read(method: String, url: String, response: HttpResponse): A

  def map[B](fn: A => B): HttpReads[B] =
    new HttpReads[B] {
      def read(method: String, url: String, response: HttpResponse): B =
        fn(outer.read(method, url, response))
    }

  def flatMap[B](fn: A => HttpReads[B]): HttpReads[B] =
    new HttpReads[B] {
      def read(method: String, url: String, response: HttpResponse): B =
        fn(outer.read(method, url, response)).read(method, url, response)
    }
}

trait RawReads {
  implicit val readRaw: HttpReads[HttpResponse] =
    HttpReads.ask.map { case (_, _, response) => response }
}

object RawReads extends RawReads

trait EitherHttpReads {
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

  def throwOnFailure[A](reads: HttpReads[Either[UpstreamErrorResponse, A]])(implicit mf: Manifest[A]): HttpReads[A] =
    reads
      .map {
        case Left(err)    => throw err
        case Right(value) => value
      }

  /** Provides backward compatible rawReads - you will have to add to implicit scope explicitly */
  val legacyRawReads: HttpReads[HttpResponse] =
    HttpReads.throwOnFailure(HttpReads.readEither)
}

trait OptionHttpReads {
  // the Option is possibly ambiguous - e.g. would expect None for all failures? (since no other avenue for failure, depending on A..)
  /*implicit def readOptionOf[A](implicit reads: HttpReads[A]): HttpReads[Option[A]] =
    new HttpReads[Option[A]] {
      def read(method: String, url: String, response: HttpResponse) =
        response.status match {
          case 204 | 404 => None
          case _         => Some(reads.read(method, url, response))
        }
    }
  */

  implicit def readOptionOf[A : HttpReads]: HttpReads[Option[A]] =
    HttpReads[HttpResponse]
      .flatMap(_.status match {
        case 204 | 404 => HttpReads.pure(None)
        case _         => HttpReads[A].map(Some.apply) // this delegates error handling to HttpReads[A]
      })
}

trait JsonHttpReads {
  import HttpErrorFunctions.handleResponse

  /** Note to read json regardless of error response - can define your own:
    * {{{
    *   HttpReads[HttpResponse].map(_.json.validate[A])
    * }}}
    * or custom behaviour - define your own:
    * {{{
    * HttpReads[HttpResponse].map(response => response.status match {
    *   case 200   => Right(response.body.json.validate[A])
    *   case 400   => Right(response.body.json.validate[A])
    *   case other => Left(s"Invalid status code: $other")
    * })
    * }}}
    */
  implicit def readFromJsonSafe[A : json.Reads]: HttpReads[Either[UpstreamErrorResponse, JsResult[A]]] =
    HttpReads.readEither.map(_.right.map(_.json.validate[A]))

  // useful? `readSeqFromJsonProperty` has this behaviour...
  def readFromJsonAt[A : json.Reads](selector: JsValue => JsLookupResult): HttpReads[Either[UpstreamErrorResponse, JsResult[A]]] =
    HttpReads.readEither.map(_.right.map(response => selector(response.json).validate[A]))

  /** backward compatible variant which throws all failures as exceptions */
  implicit def readFromJson[A](implicit rds: json.Reads[A], mf: Manifest[A]): HttpReads[A] =
    /* new HttpReads[O] {
       def read(method: String, url: String, response: HttpResponse) =
         readJson(method, url, handleResponse(method, url)(response).json)
     }
    */
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



  // TODO Can we drop this? It conflates Seq.empty in json (already handled by json.Reads)
  // with Seq.empty for 204/404 - already handled by Option above - client can choose if they want to conflate...
  // also can we just use a JsLookup to traverse into the Json structure?
  // only 2 uses of it across hmrc...
  def readSeqFromJsonProperty[A](name: String)(implicit reads: json.Reads[A], mf: Manifest[A]): HttpReads[Seq[A]] =
   /*new HttpReads[Seq[O]] {
    def read(method: String, url: String, response: HttpResponse) = response.status match {
      case 204 | 404 => Seq.empty
      case _ =>
        readJson[Seq[O]](method, url, (handleResponse(method, url)(response).json \ name).getOrElse(JsNull)) //Added JsNull here to force validate to fail - replicates existing behaviour
    }
  }*/
    HttpReads.readOptionOf[HttpResponse]
      .flatMap {
        case None => HttpReads.pure(Seq.empty)
        case _    => throwOnJsonFailure(HttpReads.readFromJsonAt[Seq[A]](_ \ name))
      }


  /*
  private def readJson[A](method: String, url: String, jsValue: JsValue)(implicit rds: json.Reads[A], mf: Manifest[A]) =
    jsValue
      .validate[A]
      .fold(
        errs => throw new JsValidationException(method, url, mf.runtimeClass, errs.toString()),
        valid => valid
      )*/
}
