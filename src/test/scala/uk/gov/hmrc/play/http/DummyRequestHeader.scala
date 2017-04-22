/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.play.http

import java.security.cert.X509Certificate

import play.api.mvc.{Headers, RequestHeader}
import play.api.test.FakeHeaders


class DummyRequestHeader extends RequestHeader {

  override def remoteAddress: String = ???

  override def headers: Headers = FakeHeaders(Seq.empty)

  override def queryString: Map[String, Seq[String]] = ???

  override def version: String = ???

  override def method: String = "GET"

  override def path: String = "/"

  override def uri: String = "/"

  override def tags: Map[String, String] = ???

  override def id: Long = ???

  override def secure: Boolean = false

  override def clientCertificateChain: Option[Seq[X509Certificate]] = ???
}
