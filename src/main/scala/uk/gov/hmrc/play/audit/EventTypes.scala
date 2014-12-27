package uk.gov.hmrc.play.audit

trait EventTypes {

  type EventType = String

  val ServiceReceivedRequest: EventType = "ServiceReceivedRequest"
  val ServiceSentResponse: EventType = "ServiceSentResponse"

  val OutboundCall:EventType = "OutboundCall"

  val TransactionFailureReason: EventType = "transactionFailureReason"
  val ServerInternalError: EventType = "ServerInternalError"
  val ResourceNotFound: EventType = "ResourceNotFound"
  val ServerValidationError: EventType = "ServerValidationError"
}

object EventTypes extends EventTypes

