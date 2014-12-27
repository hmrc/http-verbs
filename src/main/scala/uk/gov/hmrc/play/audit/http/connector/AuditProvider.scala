package uk.gov.hmrc.play.audit.http.connector

trait AuditProvider {
  def toAuditTags(transactionName: String, path: String): Map[String, String]

  def toAuditDetails(details: (String, String)*): Map[String, String]
}
