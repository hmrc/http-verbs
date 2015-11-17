/*
 * Copyright 2015 HM Revenue & Customs
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

import play.api.libs.ws.{WSClient, DefaultWSClientConfig}
import play.api.libs.ws.ning.NingAsyncHttpClientConfigBuilder

trait WSClientProvider {
  implicit val client: WSClient
}

trait DefaultWSClientProvider extends WSClientProvider {
  val clientConfig = new DefaultWSClientConfig()
  val secureDefaults:com.ning.http.client.AsyncHttpClientConfig = new NingAsyncHttpClientConfigBuilder(clientConfig).build()
  val builder = new com.ning.http.client.AsyncHttpClientConfig.Builder(secureDefaults)
  builder.setCompressionEnabled(true)
  val secureDefaultsWithSpecificOptions:com.ning.http.client.AsyncHttpClientConfig = builder.build()

  implicit val client = new play.api.libs.ws.ning.NingWSClient(secureDefaultsWithSpecificOptions)
}
