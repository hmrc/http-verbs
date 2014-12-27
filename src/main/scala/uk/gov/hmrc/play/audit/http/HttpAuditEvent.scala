package uk.gov.hmrc.play.audit.http

import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditProvider}
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.config.AppName

trait HttpAuditEvent extends AppName {

  import play.api.mvc.RequestHeader

  val auditConnector: AuditConnector = AuditConnector

  object auditDetailKeys {
    val Input = "input"
    val Method = "method"
    val UserAgentString = "userAgentString"
    val Referrer = "referrer"
  }

  object headers {
    val UserAgent = "User-Agent"
    val Referer = "Referer"
  }

  protected[http] def dataEvent(eventType: String, transactionName: String, request: RequestHeader)(implicit hc: AuditProvider = HeaderCarrier.fromHeaders(request.headers)) = {

    import auditDetailKeys._
    import headers._
    import uk.gov.hmrc.play.audit.http.HeaderFieldsExtractor._

    val requiredFields = hc.toAuditDetails(Input -> s"Request to ${request.path}",
     Method -> request.method.toUpperCase,
      UserAgentString -> request.headers.get(UserAgent).getOrElse("-"),
      Referrer -> request.headers.get(Referer).getOrElse("-"))

    val tags = hc.toAuditTags(transactionName, request.path)

    DataEvent(appName, eventType, detail = requiredFields ++ optionalAuditFieldsSeq(request.headers.toMap), tags = tags)
  }
}

object HeaderFieldsExtractor {
  private val SurrogateHeader = "Surrogate"

  def optionalAuditFields(headers : Map[String, String]) : Map[String, String] = {
    val map = headers map (t => t._1 -> Seq(t._2))
    optionalAuditFieldsSeq(map)
  }

  def optionalAuditFieldsSeq(headers : Map[String, Seq[String]]) : Map[String, String] = {
    headers.foldLeft(Map[String, String]()) { (existingList : Map[String, String], tup: (String, Seq[String])) =>
      tup match {
        case (SurrogateHeader, _) => existingList + ("surrogate" -> tup._2.mkString(","))
        // Add more optional here
        case _ => existingList
      }
    }
  }
}
