/*
 * Copyright 2020 HM Revenue & Customs
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

import play.api.libs.json.{JsValue, Json}

/**
  * The ws.Response class is very hard to dummy up as it wraps a concrete instance of
  * the ning http Response. This trait exposes just the bits of the response that we
  * need in methods that we are passing the response to for processing, making it
  * much easier to provide dummy data in our specs.
  */
// TODO Convert to a case-class, allowing to use `copy(...)` in tests
// ensuring field names in apply/unapply match
// removes "???"
// the only classes which extends this DummyHttpResponse and WSHttpResponse can be provided by constructors (e.g. `httpResponsefromWsResponse`)
trait HttpResponse {
  def allHeaders: Map[String, Seq[String]] = ???

  def header(key: String): Option[String] =
    allHeaders.get(key).flatMap(_.headOption)

  def status: Int = ???

  def json: JsValue = ???

  def body: String = ???

  override def toString: String =
    s"HttpResponse status=$status"
}

object HttpResponse {
  def apply(
    responseStatus: Int,
    responseJson: Option[JsValue]             = None,
    responseHeaders: Map[String, Seq[String]] = Map.empty,
    responseString: Option[String]            = None
  ) = new HttpResponse {
    override def allHeaders: Map[String, Seq[String]] = responseHeaders
    override def body: String                         = responseString.orElse(responseJson.map(Json.prettyPrint)).orNull
    override def json: JsValue                        = responseJson.orNull
    override def status: Int                          = responseStatus
  }

  def unapply(that: HttpResponse) = Some((that.status, that.json, that.allHeaders, that.body))
}
