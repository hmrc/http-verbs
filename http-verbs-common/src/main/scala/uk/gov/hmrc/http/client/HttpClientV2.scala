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

package uk.gov.hmrc.http.client

import play.api.libs.ws.{BodyWritable, WSRequest}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads}

import java.net.URL
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.runtime.universe.TypeTag

/** This client centralises the execution of the request to ensure that the common concerns (e.g. auditing, logging,
  * retries) occur, but makes building the request more flexible (by exposing play-ws).
  * It also supports streaming.
  */
trait HttpClientV2 {
  protected def mkRequestBuilder(url: URL, method: String)(implicit hc: HeaderCarrier): RequestBuilder

  def get(url: URL)(implicit hc: HeaderCarrier): RequestBuilder =
    mkRequestBuilder(url, "GET")

  def post(url: URL)(implicit hc: HeaderCarrier): RequestBuilder =
    mkRequestBuilder(url, "POST")

  def put(url: URL)(implicit hc: HeaderCarrier): RequestBuilder =
    mkRequestBuilder(url, "PUT")

  def delete(url: URL)(implicit hc: HeaderCarrier): RequestBuilder =
    mkRequestBuilder(url, "DELETE")

  def patch(url: URL)(implicit hc: HeaderCarrier): RequestBuilder =
    mkRequestBuilder(url, "PATCH")

  def head(url: URL)(implicit hc: HeaderCarrier): RequestBuilder =
    mkRequestBuilder(url, "HEAD")

  def options(url: URL)(implicit hc: HeaderCarrier): RequestBuilder =
    mkRequestBuilder(url, "OPTIONS")
}

trait RequestBuilder {
  def transform(transform: WSRequest => WSRequest): RequestBuilder

  def execute[A: HttpReads](implicit ec: ExecutionContext): Future[A]

  def stream[A: StreamHttpReads](implicit ec: ExecutionContext): Future[A]

  // support functions

  /** Adds the header. If the header has already been defined (e.g. from HeaderCarrier), it will be replaced. */
  def setHeader(header: (String, String)): RequestBuilder

  @deprecated("Use setHeader", "14.5.0")
  def replaceHeader(header: (String, String)): RequestBuilder

  @deprecated("Use setHeader to add or replace, or use transform(_.addHttpHeaders) to append header values to existing", "14.5.0")
  def addHeaders(headers: (String, String)*): RequestBuilder

  def withProxy: RequestBuilder

  /** `withBody` should be called rather than `transform(_.withBody)`.
    * Failure to do so will lead to a runtime exception
    */
  def withBody[B : BodyWritable : TypeTag](body: B): RequestBuilder
}
