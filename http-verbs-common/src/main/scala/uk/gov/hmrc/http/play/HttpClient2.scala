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

// TODO putting this in this package means that all clients which do
// `import uk.gov.hmrc.http._` will then have to make play imports with _root_ `import _root_.play...`
package uk.gov.hmrc.http.play

import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.libs.ws.{BodyWritable, WSRequest}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads}

import java.net.URL
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.runtime.universe.TypeTag

trait HttpClient2 {
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

  def replaceHeader(header: (String, String)): RequestBuilder

  def addHeaders(headers: (String, String)*): RequestBuilder

  def withProxy: RequestBuilder

  /** `withBody` should be called rather than `transform(_.withBody)`.
    * Failure to do so will lead to a runtime exception
    */
  def withBody[B : BodyWritable : TypeTag](body: B): RequestBuilder
}
