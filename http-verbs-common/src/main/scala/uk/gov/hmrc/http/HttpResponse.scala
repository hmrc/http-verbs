/*
 * Copyright 2022 HM Revenue & Customs
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

import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.github.ghik.silencer.silent
import play.api.libs.json.{JsValue, Json}

/**
  * The ws.Response class is very hard to dummy up as it wraps a concrete instance of
  * the ning http Response. This trait exposes just the bits of the response that we
  * need in methods that we are passing the response to for processing, making it
  * much easier to provide dummy data in our specs.
  */
// This trait will be replaced with a case class (Which will remove coupling to specific types, enable `.copy` etc (useful for testing))
// To not break clients, we will discourage use of extending HttpResponse, rather use the apply functions. We will be able to introduce
// the case class afterwards.
trait HttpResponse {
  def status: Int

  def body: String

  @deprecated("For reading use headers instead. If setting, use HttpResponse.apply instead. You should not extend HttpResponse, but create instances with HttpResponse.apply", "11.0.0")
  def allHeaders: Map[String, Seq[String]]

  // final to help migrate away from allHeaders (i.e. read only - set via HttpResponse.apply)
  @silent("deprecated")
  final def headers: Map[String, Seq[String]]
    = allHeaders

  def json: JsValue =
    Json.parse(body)

  def bodyAsSource: Source[ByteString, _] =
    Source.single(ByteString(body))

  def header(key: String): Option[String] =
    headers.collectFirst { case (k, v :: _) if k.equalsIgnoreCase(key) => v }

  override def toString: String =
    s"HttpResponse status=$status"
}

object HttpResponse {
  @deprecated("Use alternative HttpResponse.apply functions instead", "11.0.0")
  def apply(
    responseStatus : Int,
    responseJson   : Option[JsValue]          = None,
    responseHeaders: Map[String, Seq[String]] = Map.empty,
    responseString : Option[String]           = None
  ) = new HttpResponse {
    override def status    : Int                      = responseStatus
    override def body      : String                   = responseString.orElse(responseJson.map(Json.prettyPrint)).orNull
    override def allHeaders: Map[String, Seq[String]] = responseHeaders
    override def json      : JsValue                  = responseJson.orNull
  }

  def apply(
    status : Int,
    body   : String
  ): HttpResponse =
    apply(
      status  = status,
      body    = body,
      headers = Map.empty
    )

  def apply(
    status : Int,
    body   : String,
    headers: Map[String, Seq[String]]
  ): HttpResponse = {
    val pStatus  = status
    val pBody    = body
    val pHeaders = headers
    new HttpResponse {
      override def status    : Int                      = pStatus
      override def body      : String                   = pBody
      override def allHeaders: Map[String, Seq[String]] = pHeaders
    }
  }

  def apply(
    status : Int,
    json   : JsValue,
    headers: Map[String, Seq[String]]
  ): HttpResponse = {
    val pStatus  = status
    val pJson    = json
    val pHeaders = headers
    new HttpResponse {
      override def status    : Int                      = pStatus
      override def body      : String                   = Json.prettyPrint(pJson)
      override def allHeaders: Map[String, Seq[String]] = pHeaders
      override def json      : JsValue                  = pJson
    }
  }

  def apply(
    status      : Int,
    bodyAsSource: Source[ByteString, _],
    headers     : Map[String, Seq[String]]
  ): HttpResponse = {
    val pStatus       = status
    val pBodyAsSource = bodyAsSource
    val pHeaders      = headers
    new HttpResponse {
      override def status      : Int                      = pStatus
      override def body        : String                   = sys.error(s"This is a streamed response, please use `bodyAsSource`")
      override def bodyAsSource: Source[ByteString, _]    = pBodyAsSource
      override def allHeaders  : Map[String, Seq[String]] = pHeaders
    }
  }

  def unapply(that: HttpResponse): Option[(Int, String, Map[String, Seq[String]])] =
    Some((that.status, that.body, that.headers))
}
