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

package uk.gov.hmrc.play.http.ws

import com.github.ghik.silencer.silent
import play.api.libs.json.JsValue
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.http.HttpResponse

@deprecated("Use WsHttpResponse.apply and HttpResponse instead", "11.0.0")
class WSHttpResponse(wsResponse: WSResponse) extends HttpResponse {

  @silent("deprecated") // allHeaders is required for Play 2.5
  override def allHeaders: Map[String, Seq[String]] = wsResponse.allHeaders

  override def status: Int = wsResponse.status

  override def json: JsValue = wsResponse.json

  override def body: String = wsResponse.body
}

// Note, replacing `new WSHttpResponse` with `WSHttpResponse.apply` in library means that methods are `HttpResponse`
// instead of `WSHttpResponse` and will break clients who rely on this
object WSHttpResponse {
  @silent("deprecated") // allHeaders is required for Play 2.5
  def apply(wsResponse: WSResponse): HttpResponse =
    HttpResponse(
      status  = wsResponse.status,
      // Note json is defined in HttpResponse in the same way as wsResponse.json (in latest version...)
      // (it was defined as Json.parse(response.bodyAsBytes.toArray) between 1.0.1 and 2.1.0, but instead of solving en encoding issue, it introduced inconsisten parsing: https://github.com/playframework/play-ws/commits/master/play-ws-standalone-json/src/main/scala/play/api/libs/ws/JsonBodyReadables.scala)
      // 2.7.4 -> 2.0.8
      // 2.6.25 -> 1.1.14
      body    = wsResponse.body,
      headers = wsResponse.allHeaders
    )
}

trait WSHttp extends WSGet with WSPut with WSPost with WSDelete with WSPatch
