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

// https://dotty.epfl.ch/docs/reference/metaprogramming/macros.html
// https://docs.scala-lang.org/scala3/reference/other-new-features/type-test.html#
object TypeUtil {
  type TypeTag[A] = A

  import scala.reflect.TypeTest

  object IsMap {
    def unapply[T](
      t: T
    )(using
      tt1: TypeTest[T, Map[String, Seq[String]]],
      tt2: TypeTest[T, Map[String, String]]
    ): Option[Map[String, Seq[String]]] =
      t match {
        case tt1(x) => Some(x)
        case tt2(x) => Some(x.map { case (k, v) => k -> Seq(v) })
        case _      => None
      }
  }
}
