/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.play.controllers

import play.api.libs.json._


object JsPathEnrichment {

  implicit class RichJsPath(jsPath: JsPath) {

    // Same as JsPath.readNullable but does not fail if the path does not exist up to the read point,
    // which is how the now deprecated readOpt usefully behaves.
    def tolerantReadNullable[T](implicit r: Reads[T]): Reads[Option[T]] = tolerantReadNullable(jsPath)(r)

    private def tolerantReadNullable[A](path: JsPath)(implicit reads: Reads[A]) = Reads[Option[A]] { json =>
      path.applyTillLast(json).fold(
        jsError => JsSuccess(None),
        jsResult => jsResult.fold(
          _ => JsSuccess(None),
          a => a match {
            case JsNull => JsSuccess(None)
            case js => reads.reads(js).repath(path).map(Some(_))
          }
        )
      )
    }
  }
}
