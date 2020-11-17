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

import java.net.{URI, URL, URLEncoder}
import java.nio.charset.StandardCharsets.UTF_8

import play.api.Logger
import play.api.libs.json.Writes
import play.utils.UriEncoding

import scala.concurrent.{ExecutionContext, Future}

trait GetHttpTransport {
  def doGet(
    url: String,
    headers: Seq[(String, String)] = Seq.empty)(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext): Future[HttpResponse]
}

trait DeleteHttpTransport {
  def doDelete(
    url: String,
    headers: Seq[(String, String)] = Seq.empty)(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext): Future[HttpResponse]
}

trait PatchHttpTransport {
  def doPatch[A](
    url: String,
    body: A,
    headers: Seq[(String, String)] = Seq.empty)(
      implicit rds: Writes[A],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[HttpResponse]
}

trait PutHttpTransport {
  def doPut[A](
    url: String,
    body: A,
    headers: Seq[(String, String)] = Seq.empty)(
      implicit rds: Writes[A],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[HttpResponse]

  def doPutString(
    url: String,
    body: String,
    headers: Seq[(String, String)] = Seq.empty)(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext): Future[HttpResponse]
}

trait PostHttpTransport {
  def doPost[A](
    url: String,
    body: A,
    headers: Seq[(String, String)] = Seq.empty)(
      implicit wts: Writes[A],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[HttpResponse]

  def doPostString(
    url: String,
    body: String,
    headers: Seq[(String, String)] = Seq.empty)(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext): Future[HttpResponse]

  def doEmptyPost[A](
    url: String,
    headers: Seq[(String, String)] = Seq.empty)(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext): Future[HttpResponse]

  def doFormPost(
    url: String,
    body: Map[String, Seq[String]],
    headers: Seq[(String, String)] = Seq.empty)(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext): Future[HttpResponse]
}

trait HttpTransport
    extends GetHttpTransport
    with DeleteHttpTransport
    with PatchHttpTransport
    with PutHttpTransport
    with PostHttpTransport {}

class UrlBuilder private (baseUrl: String, queryParams: Seq[(String, String)], fragment: Option[String]) {

  if(baseUrl.contains("?")){
    Logger(getClass).warn("Passing params in baseUrl is deprecated now. Please use builder methods on UrlBuilder to add query parameters")
  }

  if(baseUrl.contains("#")){
    Logger(getClass).warn("Passing fragments in baseUrl is deprecated now. Please use builder methods on UrlBuilder to add fragment")
  }

  def addQueryParams(queryParameters: Seq[(String, String)]): UrlBuilder =
    new UrlBuilder(baseUrl, queryParams = queryParams ++ queryParameters, fragment)
  def addQueryParam(queryParam: (String, String)): UrlBuilder          = addQueryParams(Seq(queryParam))
  def withFragment(fragment: String): UrlBuilder                       = new UrlBuilder(baseUrl, queryParams, fragment = Some(fragment))
  def addPath(path: String): UrlBuilder = {
    if(path.isEmpty) this else
    new UrlBuilder(s"$baseUrl/${encodeUri(path)}", queryParams, fragment)
  }

  private val uri = new URI(baseUrl)

  def toUrl: URL = {

    val allQueryParams = Option(uri.getQuery).getOrElse("&").split("&").map { param =>
      val keyValue = param.split("=", 2)
      keyValue(0) -> keyValue(1)
    }.toSeq ++ queryParams

    val encodeQueryParams = allQueryParams
      .map {
        case (k, v) => s"${encodeQuery(k)}=${encodeQuery(v)}"
      }

    val queryParamPrefix = if(encodeQueryParams.nonEmpty) "?" else ""

    val encodeQueryParamString = encodeQueryParams.mkString(queryParamPrefix, "&", "")

    val encodedFragment = fragment.orElse(Option(uri.getFragment)).map(encodeUri).getOrElse("")

    val path = baseUrl.takeWhile(_ != '?')

    new URL(s"$path$encodeQueryParamString$encodedFragment")
  }

  private def encodeUri(input: String) =  UriEncoding.encodePathSegment(input, UTF_8)
  private def encodeQuery(input: String) =  URLEncoder.encode(input, UTF_8.toString)
}

object UrlBuilder {
  def apply(baseUrl: String) = new UrlBuilder(baseUrl, Seq.empty, None)
}

trait CoreGet {
  def GET[A](
    urlBuilder: UrlBuilder,
    headers: Seq[(String, String)])(implicit rds: HttpReads[A], hc: HeaderCarrier, ec: ExecutionContext): Future[A]

  def GET[A](url: String, queryParams: Seq[(String, String)], headers: Seq[(String, String)])(
    implicit rds: HttpReads[A],
    hc: HeaderCarrier,
    ec: ExecutionContext): Future[A] = GET(UrlBuilder(url).addQueryParams(queryParams), headers)

  def GET[A](url: String)(implicit rds: HttpReads[A], hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
    GET(UrlBuilder(url), Seq.empty)

  def GET[A](url: String, queryParams: Seq[(String, String)])(
    implicit rds: HttpReads[A],
    hc: HeaderCarrier,
    ec: ExecutionContext): Future[A] =
    GET(UrlBuilder(url).addQueryParams(queryParams), Seq.empty)
}

trait CoreDelete {
  def DELETE[O](
    url: String,
    headers: Seq[(String, String)] = Seq.empty)(
      implicit rds: HttpReads[O],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[O]
}

trait CorePatch {
  def PATCH[I, O](
    url: String,
    body: I,
    headers: Seq[(String, String)] = Seq.empty)(
      implicit wts: Writes[I],
      rds: HttpReads[O],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[O]
}

trait CorePut {
  def PUT[I, O](
    url: String,
    body: I,
    headers: Seq[(String, String)] = Seq.empty)(
      implicit wts: Writes[I],
      rds: HttpReads[O],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[O]

  def PUTString[O](
    url: String,
    body: String,
    headers: Seq[(String, String)] = Seq.empty)(
      implicit rds: HttpReads[O],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[O]
}

trait CorePost {
  def POST[I, O](
    url: String,
    body: I,
    headers: Seq[(String, String)] = Seq.empty)(
      implicit wts: Writes[I],
      rds: HttpReads[O],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[O]

  def POSTString[O](
    url: String,
    body: String,
    headers: Seq[(String, String)] = Seq.empty)(
      implicit rds: HttpReads[O],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[O]

  def POSTForm[O](
    url: String,
    body: Map[String, Seq[String]],
    headers: Seq[(String, String)] = Seq.empty)(
      implicit rds: HttpReads[O],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[O]

  def POSTEmpty[O](
    url: String,
    headers: Seq[(String, String)] = Seq.empty)(
      implicit rds: HttpReads[O],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[O]
}
