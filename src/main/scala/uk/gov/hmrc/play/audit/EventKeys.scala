package uk.gov.hmrc.play.audit

/**
 * Created by duncancrawford on 27/12/14.
 */
object EventKeys {
  val StatusCode = "statusCode"

  val FailedRequestMessage = "failedRequestReason"
  val ResponseMessage = "responseMessage"
  val Path = "path"
  val Method = "method"
  val RequestBody = "requestBody"
  val ExternalApplicationName = "externalApplicationName"
  val TransactionName = "transactionName"
}
