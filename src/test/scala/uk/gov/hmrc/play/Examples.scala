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
