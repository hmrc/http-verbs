/*
 * Copyright 2022 HM Revenue & Customs
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

object HttpReads extends HttpReadsLegacyInstances {
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


  // readRaw is brought in like this rather than in a trait as this gives it
  // compilation priority during implicit resolution. This means, unless
  // specified otherwise a verb call will return a plain HttpResponse
  @deprecated("Use uk.gov.hmrc.http.HttpReads.Implicits instead. See README for differences.", "11.0.0")
  implicit val readRaw: HttpReads[HttpResponse] = HttpReadsLegacyRawReads.readRaw

  object Implicits extends HttpReadsInstances
}

trait HttpReads[A] {
  outer =>

  def read(method: String, url: String, response: HttpResponse): A

  def map[B](fn: A => B): HttpReads[B] =
    flatMap(a => HttpReads.pure(fn(a)))

  def flatMap[B](fn: A => HttpReads[B]): HttpReads[B] =
    new HttpReads[B] {
      def read(method: String, url: String, response: HttpResponse): B =
        fn(outer.read(method, url, response)).read(method, url, response)
    }
}
