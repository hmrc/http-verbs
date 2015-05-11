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
import scala.concurrent.ExecutionContext.Implicits.global

object Examples {

  import uk.gov.hmrc.play.http._
  import uk.gov.hmrc.play.http.ws._
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

  trait ConnectorWithMixins extends HttpGet with HttpPost {

  }
  object ConnectorWithMixins extends ConnectorWithMixins with WSGet with WSPost {
    val appName = "my-app-name"
    val auditConnector = AuditConnector(LoadAuditingConfig(key = "auditing"))
  }

  trait VerbExamples {
    val http: HttpGet with HttpPost with HttpPut with HttpDelete with HttpPatch

    implicit val hc = HeaderCarrier()

    http.GET("http://gov.uk/hmrc")
    http.DELETE("http://gov.uk/hmrc")
    http.POST("http://gov.uk/hmrc", body = "hi there")
    http.PUT("http://gov.uk/hmrc", body = "hi there")
    http.PATCH("http://gov.uk/hmrc", body = "hi there")

    val r1 = http.GET("http://gov.uk/hmrc") // Returns an HttpResponse
    val r2 = http.GET[HttpResponse]("http://gov.uk/hmrc") // Can specify this explicitly
    r1.map { r =>
      r.status
      r.body
      r.allHeaders
    }

    import play.api.libs.json._
    case class MyCaseClass(a: String, b: Int)
    implicit val f = Json.reads[MyCaseClass]
    http.GET[MyCaseClass]("http://gov.uk/hmrc") // Returns an MyCaseClass de-serialised from JSON

    import play.twirl.api.Html
    http.GET[Html]("http://gov.uk/hmrc") // Returns a Play Html type

    http.GET[Option[MyCaseClass]]("http://gov.uk/hmrc") // Returns None, or Some[MyCaseClass] de-serialised from JSON
    http.GET[Option[Html]]("http://gov.uk/hmrc") // Returns a None, or a Play Html type
  }
}
