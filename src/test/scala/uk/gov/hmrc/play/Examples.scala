/*
 * Copyright 2015 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.play

import uk.gov.hmrc.play.audit.http.HeaderCarrier


object Examples {

  import uk.gov.hmrc.play.http._
  import ws._
  import audit.http.config._
  import audit.http.connector._

  trait ConnectorWithHttpValues {
    val http: HttpGet with HttpPost
  }
  object ConnectorWithHttpValues extends ConnectorWithHttpValues {
    val http = new WSGet with WSPost {
      val appName = "my-app-name"
      val auditConnector = AuditConnector(LoadAuditingConfig(key = "auditing"))
    }
  }

  trait ConnectorWithMixins extends HttpGet with HttpPost
  object ConnectorWithMixins extends ConnectorWithMixins with WSGet with WSPost {
    val appName = "my-app-name"
    val auditConnector = AuditConnector(LoadAuditingConfig(key = "auditing"))
  }

  trait VerbExamples {
    val http: HttpGet with HttpPost with HttpPut with HttpDelete

    implicit val hc = HeaderCarrier()

    http.GET("http://gov.uk/hmrc")
    http.DELETE("http://gov.uk/hmrc")
    http.POST("http://gov.uk/hmrc", body = "hi there")
    http.PUT("http://gov.uk/hmrc", body = "hi there")
  }
}
