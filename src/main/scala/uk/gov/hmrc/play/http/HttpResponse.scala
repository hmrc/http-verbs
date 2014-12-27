package uk.gov.hmrc.play.http

import play.api.libs.json.{Json, JsValue}
import play.api.mvc.Headers

/**
 * The ws.Response class is very hard to dummy up as it wraps a concrete instance of
 * the ning http Response. This trait exposes just the bits of the response that we
 * need in methods that we are passing the response to for processing, making it
 * much easier to provide dummy data in our specs.
 */
trait HttpResponse {
  def allHeaders: Map[String, Seq[String]] = ???

  def header(key: String) : Option[String] = allHeaders.get(key).flatMap { list => list.headOption }

  def status: Int = ???

  def json: JsValue = ???

  def body: String = ???
}

object HttpResponse {

  def apply(responseStatus: Int, responseJson: Option[JsValue] = None, responseHeaders: Map[String, Seq[String]] = Map.empty) = {
    new HttpResponse {
      override def allHeaders: Map[String, Seq[String]] = responseHeaders

      override def body: String = if(responseJson.isDefined) Json.prettyPrint(responseJson.get) else null

      override def json: JsValue = responseJson.getOrElse(null)

      override def status: Int = responseStatus
    }
  }

}