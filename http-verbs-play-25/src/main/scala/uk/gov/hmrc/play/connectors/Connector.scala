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

package uk.gov.hmrc.play.connectors

import com.github.ghik.silencer.silent
import com.typesafe.config.ConfigFactory
import play.api.libs.ws.{WS, WSRequest}
import uk.gov.hmrc.http.HeaderCarrier

@deprecated("Use uk.gov.hmrc.play.http.ws.WSRequestBuilder", "13.0.0")
trait RequestBuilder {
  def buildRequest(url: String)(implicit hc: HeaderCarrier): WSRequest
}

@deprecated("Use uk.gov.hmrc.play.http.ws.WSRequest", "13.0.0")
trait PlayWSRequestBuilder extends RequestBuilder {

  @silent("deprecated")
  private lazy val app =
    play.api.Play.current

  private lazy val hcConfig =
    HeaderCarrier.Config.fromConfig(app.configuration.underlying)

  @silent("deprecated")
  def buildRequest(url: String)(implicit hc: HeaderCarrier): WSRequest =
    WS.url(url)(app)
      .withHeaders(hc.headersForUrl(hcConfig)(url): _*)
}

@deprecated("Use uk.gov.hmrc.play.http.ws.WSRequest", "13.0.0")
trait WSClientRequestBuilder extends RequestBuilder { this: WSClientProvider =>
  private val hcConfig =
    HeaderCarrier.Config.fromConfig(ConfigFactory.load())

  def buildRequest(url: String)(implicit hc: HeaderCarrier): WSRequest =
    client.url(url)
      .withHeaders(hc.headersForUrl(hcConfig)(url): _*)
}
