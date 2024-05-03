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

import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import play.api.libs.json.{JsValue, Json}

/**
  * The ws.Response class is very hard to dummy up as it wraps a concrete instance of
  * the ning http Response. This trait exposes just the bits of the response that we
  * need in methods that we are passing the response to for processing, making it
  * much easier to provide dummy data in our specs.
  */
trait HttpResponse {
  def status: Int

  def body: String

  def bodyAsSource: Source[ByteString, _] =
    Source.single(ByteString(body))

  def json: JsValue =
    Json.parse(body)

  def headers: Map[String, Seq[String]]

  def header(key: String): Option[String] =
    headers
      .collectFirst { case (k, values) if k.equalsIgnoreCase(key) => values }
      .flatMap(_.headOption)

  override def toString: String =
    s"HttpResponse status=$status"
}

object HttpResponse {
  def apply(
    status : Int,
    body   : String                   = "",
    headers: Map[String, Seq[String]] = Map.empty
  ): HttpResponse = {
    val pStatus  = status
    val pBody    = body
    val pHeaders = headers
    new HttpResponse {
      override def status  = pStatus
      override def body    = pBody
      override def headers = pHeaders
    }
  }


  def apply(
    status : Int,
    json   : JsValue,
    headers: Map[String, Seq[String]]
  ): HttpResponse =
    apply(
      status  = status,
      body    = Json.prettyPrint(json),
      headers = headers
    )

  def apply(
    status      : Int,
    bodyAsSource: Source[ByteString, _],
    headers     : Map[String, Seq[String]]
  ): HttpResponse = {
    val pStatus       = status
    val pBodyAsSource = bodyAsSource
    val pHeaders      = headers
    new HttpResponse {
      override def status       = pStatus
      override def bodyAsSource = pBodyAsSource
      override def headers      = pHeaders
      override def body         = sys.error(s"This is a streamed response, please use `bodyAsSource`")
    }
  }

  def unapply(that: HttpResponse): Option[(Int, String, Map[String, Seq[String]])] =
    Some((that.status, that.body, that.headers))
}
