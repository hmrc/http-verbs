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

package uk.gov.hmrc.http.controllers

import play.api.libs.json._

@deprecated("Use JsPath.readNullable", "15.0.0")
object JsPathEnrichment {

  implicit class RichJsPath(jsPath: JsPath) {

    // Existed since (the deprecated) JsPath.readOpt would fail if the path did notexist up to the read point.
    def tolerantReadNullable[T](implicit r: Reads[T]): Reads[Option[T]] =
      JsPath.readNullable
  }
}
