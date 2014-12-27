package uk.gov.hmrc.play.http

import play.api.libs.json.{JsValue, Json}

class DummyHttpResponse(override val body: String, override val status: Int, override val allHeaders: Map[String, Seq[String]] = Map.empty) extends HttpResponse {
  override def json: JsValue = Json.parse(body)
}
