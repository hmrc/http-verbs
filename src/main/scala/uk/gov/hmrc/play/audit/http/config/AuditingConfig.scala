package uk.gov.hmrc.play.audit.http.config

case class BaseUri(host : String, port : Int, protocol : String = "http") {
  val uri = s"$protocol://$host:$port".stripSuffix("/") + "/"

  def addEndpoint(endpoint : String) : String = s"$uri${endpoint.stripPrefix("/")}"
}

case class Consumer(baseUri : BaseUri,
                    singleEventUri : String = "write/audit",
                    mergedEventUri : String = "write/audit/merged",
                    largeMergedEventUri: String = "write/audit/merged/large") {

  val singleEventUrl = baseUri.addEndpoint(singleEventUri)
  val mergedEventUrl = baseUri.addEndpoint(mergedEventUri)
  val largeMergedEventUrl = baseUri.addEndpoint(largeMergedEventUri)

}

object Consumer {
  implicit def baseUriToConsumer(b : BaseUri) : Consumer = Consumer(b)
}

case class AuditingConfig(consumer : Consumer,
                          enabled : Boolean = true,
                          traceRequests : Boolean = true)