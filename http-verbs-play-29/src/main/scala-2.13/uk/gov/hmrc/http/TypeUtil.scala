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

import scala.reflect.runtime.universe

object TypeUtil {
  def typeOf[A](implicit mf: Manifest[A]): String =
    mf.runtimeClass.getName

  // for erasure
  object IsMap {
    def unapply[B: universe.TypeTag](b: B): Option[Map[String, Seq[String]]] =
      universe.typeOf[B] match {
        case _ if universe.typeOf[B] =:= universe.typeOf[Map[String, String]]      => Some(b.asInstanceOf[Map[String, String]].map { case (k, v) => k -> Seq(v) })
        case _ if universe.typeOf[B] =:= universe.typeOf[Map[String, Seq[String]]] => Some(b.asInstanceOf[Map[String, Seq[String]]])
        case _                                                                     => None
      }
  }
}
