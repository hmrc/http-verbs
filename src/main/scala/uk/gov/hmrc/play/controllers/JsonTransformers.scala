package uk.gov.hmrc.play.controllers

import play.api.libs.json._

object JsonTransformers {

  private type Transformation = PartialFunction[JsValue, JsValue]

  trait BaseTransformer {
    private val baseTransform: Transformation = {
      case JsObject(fields) => JsObject(fields.map(pair => (pair._1, apply()(pair._2))))
      case JsArray(values) => JsArray(values.map(apply()))
      case anything => anything
    }

    protected def transform: Transformation

    def apply() = transform orElse baseTransform
  }

  object TrimmerTransformer extends BaseTransformer {
    protected val transform: Transformation = {
      case JsString(value) => JsString(value.trim)
    }
  }

  object EmptyToNullTransformer extends BaseTransformer {
    protected val transform: Transformation = {
      case JsString(value) if value.trim.isEmpty => JsNull
    }
  }

}
