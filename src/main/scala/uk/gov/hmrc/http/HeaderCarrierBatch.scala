package uk.gov.hmrc.http

import java.util.UUID

import uk.gov.hmrc.http.logging.{RequestId, SessionId}

/**
  * Creates a HeaderCarrier for scheduled actions so that any audits made
  * as part of the batch process can be tied together
  *
  * SessionID is used to represent the whole batch.
  * If the batch loops through items each item should get a new HeaderCarrier so that the requestID changes
  */
object HeaderCarrierBatch {
  def apply() = new HeaderCarrierBatch(s"batch-${UUID.randomUUID()}")
}
class HeaderCarrierBatch(sessionID:String) {

  /*
    Creates a header carrier for use in a batch process that does not process a list of items
   */
  def createSingleHeaderCarrier = create("batch-single")

  /*
    Creates a header carrier for the first call in a batch process that fetches a list of items to process
   */
  def createFirstCallHeaderCarrier = create("batch-start")

  /*
    Creates a header carrier for use in the processing of a single item in a batch
   */
  def createItemHeaderCarrier = create("batch-item")

  private def create(prefix:String) = {
    HeaderCarrier(
      sessionId = Some(SessionId(sessionID)),
      requestId = Some(RequestId(s"$prefix-${UUID.randomUUID()}"))
    )
  }

}
