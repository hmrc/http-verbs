/*
 * Copyright 2021 HM Revenue & Customs
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
import _root_.play.api.libs.json.{JsValue, Writes}
import _root_.play.api.libs.ws.{WSRequest, WSResponse}
import scala.concurrent.Future


package object play {
  // These will still need explicitly importing
  // should they be moved to `import httpClient2._`? which means implementations can then depend on
  // httpClient values (e.g. configuration) or does this make mocking/providing alternative implementations harder?
  // Alternatively, could HttpClient2 trait just be replaced by HttpClient2Impl - and forget about alternative implementations
  // (solves the final builder problem)
  def fromStream(request: WSRequest)(response: WSResponse): Future[Source[ByteString, _]] =
    Future.successful(response.bodyAsSource)

  def fromJson[A](request: WSRequest)(response: WSResponse)(implicit r: HttpReads[A]): Future[A] =
    // reusing existing HttpReads - currently requires HttpResponse
    Future.successful {
      val httpResponse = HttpResponse(
        status  = response.status,
        body    = response.body,
        headers = response.headers
      )
      r.read(request.method, request.url, httpResponse)
    }

  def toJson[A](model: A)(implicit w: Writes[A]): JsValue =
    w.writes(model)
}
