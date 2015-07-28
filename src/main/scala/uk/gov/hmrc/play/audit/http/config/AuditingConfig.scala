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

package uk.gov.hmrc.play.audit.http.config

import play.api.Play

case class BaseUri(host: String, port: Int, protocol: String) {
  val uri = s"$protocol://$host:$port".stripSuffix("/") + "/"

  def addEndpoint(endpoint: String): String = s"$uri${endpoint.stripPrefix("/")}"
}

case class Consumer(baseUri: BaseUri,
                    singleEventUri: String = "write/audit",
                    mergedEventUri: String = "write/audit/merged",
                    largeMergedEventUri: String = "write/audit/merged/large") {

  val singleEventUrl = baseUri.addEndpoint(singleEventUri)
  val mergedEventUrl = baseUri.addEndpoint(mergedEventUri)
  val largeMergedEventUrl = baseUri.addEndpoint(largeMergedEventUri)

}

object Consumer {
  implicit def baseUriToConsumer(b: BaseUri): Consumer = Consumer(b)
}

case class AuditingConfig(consumer: Option[Consumer],
                          enabled: Boolean,
                          traceRequests: Boolean)

object LoadAuditingConfig {

  import play.api.Play.current

  def apply(key: String): AuditingConfig = {
    Play.configuration.getConfig(key).map { c =>

      val enabled = c.getBoolean("enabled").getOrElse(true)

      if(enabled) {
        AuditingConfig(
          enabled = enabled,
          traceRequests = c.getBoolean("traceRequests").getOrElse(true),
          consumer = Some(c.getConfig("consumer").map { con =>
            Consumer(
              baseUri = con.getConfig("baseUri").map { uri =>
                BaseUri(
                  host = uri.getString("host").getOrElse(throw new Exception("Missing consumer host for auditing")),
                  port = uri.getInt("port").getOrElse(throw new Exception("Missing consumer port for auditing")),
                  protocol = uri.getString("protocol").getOrElse("http")
                )
              }.getOrElse(throw new Exception("Missing consumer baseUri for auditing"))
            )
          }.getOrElse(throw new Exception("Missing consumer configuration for auditing")))
        )
      } else {
        AuditingConfig(consumer = None, enabled = false, traceRequests = false)
      }

    }
  }.getOrElse(throw new Exception("Missing auditing configuration"))
}
