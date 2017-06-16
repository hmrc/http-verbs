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

package uk.gov.hmrc.play.http

import play.api.libs.json.JsPath
import play.api.data.validation.ValidationError

class JsValidationException(val method: String,
                            val url: String,
                            val readingAs: Class[_],
                            val errors: Seq[(JsPath, Seq[ValidationError])]) extends Exception {
  override def getMessage: String = {
    s"$method of '$url' returned invalid json. Attempting to convert to ${readingAs.getName} gave errors: $errors"
  }
}

class UrlValidationException(val url: String, val context: String, val message: String) extends Exception {
  override def getMessage: String = {
    s"'$url' is invalid for $context. $message"
  }
}
