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