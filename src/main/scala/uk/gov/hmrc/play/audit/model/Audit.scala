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

package uk.gov.hmrc.play.audit.model

import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.Future
import scala.util.Try


sealed trait AuditAsMagnet[A] {

  import uk.gov.hmrc.play.audit.model.Audit.OutputTransformer

  val txName: String
  val inputs: Map[String, String]
  val outputTransformer: OutputTransformer[A]
  val eventTypes: (String, String)

  def apply(f: (String, Map[String, String], OutputTransformer[A], (String, String)) => A): A = f(txName, inputs, outputTransformer, eventTypes)

}

object AuditAsMagnet {

  import uk.gov.hmrc.play.audit.model.Audit.{EventTypeFlowDescriptions, OutputTransformer, defaultEventTypes}

  implicit def inputAsStringDefaultEventTypes[A](parms: (String, String, OutputTransformer[A])): AuditAsMagnet[A] = auditAsMagnet(parms._1, Map("" -> parms._2), defaultEventTypes, parms._3)

  implicit def inputAsString[A](parms: (String, String, EventTypeFlowDescriptions, OutputTransformer[A])): AuditAsMagnet[A] = auditAsMagnet(parms._1, Map("" -> parms._2), parms._3, parms._4)

  implicit def inputAsMapDefaultEventTypes[A](parms: (String, Map[String, String], OutputTransformer[A])): AuditAsMagnet[A] = auditAsMagnet(parms._1, parms._2, defaultEventTypes, parms._3)

  implicit def inputAsMap[A](parms: (String, Map[String, String], EventTypeFlowDescriptions, OutputTransformer[A])): AuditAsMagnet[A] = auditAsMagnet(parms._1, parms._2, parms._3, parms._4)

  private def auditAsMagnet[A](txN: String,
                               ins: Map[String, String],
                               et: EventTypeFlowDescriptions,
                               ot: OutputTransformer[A]): AuditAsMagnet[A] = new AuditAsMagnet[A] {
    val txName = txN
    val inputs = ins
    val outputTransformer = ot
    val eventTypes = et
  }

}

object EventTypes {
  val Succeeded = "TxSucceeded"
  val Failed = "TxFailed"
}

object Audit {
  import EventTypes._

  type OutputTransformer[A] = (A => TransactionResult)
  type Body[A] = () => A
  type AsyncBody[A] = () => Future[A]
  type EventTypeFlowDescriptions = (String, String)

  val defaultEventTypes: EventTypeFlowDescriptions = (Succeeded, Failed)

  def apply(applicationName: String, auditConnector: AuditConnector) = new Audit(applicationName, auditConnector)
}

trait AuditTags {
  val xRequestId = "X-Request-ID"
  val TransactionName = "transactionName"
}
class Audit(applicationName: String, auditConnector: AuditConnector) extends AuditTags {

  import uk.gov.hmrc.play.audit.http.HeaderCarrier
  import Audit._
  import scala.concurrent.ExecutionContext.Implicits.global

  def sendDataEvent: (DataEvent) => Unit = auditConnector.sendEvent(_)

  def sendLargeMergedDataEvent: (MergedDataEvent) => Unit = auditConnector.sendLargeMergedEvent(_)

  private def sendEvent[A](auditMagnet: AuditAsMagnet[A], eventType: String, outputs: Map[String, String])(implicit hc: HeaderCarrier): Unit = {
    val requestId = hc.requestId.map(_.value).getOrElse("")
    sendDataEvent(DataEvent(
      auditSource = applicationName,
      auditType = eventType,
      tags = Map(xRequestId -> requestId, TransactionName -> auditMagnet.txName),
      detail = auditMagnet.inputs.map(inputKeys) ++ outputs))
  }

  private def givenResultSendAuditEvent[A](auditMagnet: AuditAsMagnet[A])(implicit hc: HeaderCarrier): PartialFunction[TransactionResult, Unit] = {
    case TransactionSuccess(m) => sendEvent(auditMagnet, auditMagnet.eventTypes._1, m.map(outputKeys))
    case TransactionFailure(r, m) => sendEvent(auditMagnet, auditMagnet.eventTypes._2, r.map(reason => Map("transactionFailureReason" -> reason)).getOrElse(Map.empty) ++ m.map(outputKeys))
  }

  def asyncAs[A](auditMagnet: AuditAsMagnet[A])(body: AsyncBody[A])(implicit hc: HeaderCarrier): Future[A] = {

//    import MdcLoggingExecutionContext._

    val invokedBody: Future[A] =
      try { body() }
      catch { case e: Exception => Future.failed[A](e) }

    invokedBody
      .map ( auditMagnet.outputTransformer )
      .recover { case e: Exception => TransactionFailure(s"Exception Generated: ${e.getMessage}") }
      .map ( givenResultSendAuditEvent(auditMagnet) )

    invokedBody
  }

  def as[A](auditMagnet: AuditAsMagnet[A])(body: Body[A])(implicit hc: HeaderCarrier): A = {
    val result: Try[A] = Try(body())

    result
      .map ( auditMagnet.outputTransformer )
      .recover { case e: Exception => TransactionFailure(s"Exception Generated: ${ e.getMessage }") }
      .map ( givenResultSendAuditEvent(auditMagnet) )

    result.get
  }

  private val inputKeys = prependKeysWith("input") _
  private val outputKeys = prependKeysWith("output") _

  private def prependKeysWith(prefix: String)(entry: (String, String)) = entry match {
    case ("", value) => prefix -> value
    case (key, value) => s"$prefix-$key" -> value
  }
}

sealed trait TransactionResult {
  def outputs: Map[String, String]
}

case class TransactionFailure(reason: Option[String] = None, outputs: Map[String, String] = Map()) extends TransactionResult

object TransactionFailure {
  def apply(reason: String, outputs: (String, String)*): TransactionFailure = TransactionFailure(Some(reason), Map(outputs: _*))

  def apply(outputs: (String, String)*): TransactionFailure = TransactionFailure(outputs = Map(outputs: _*))
}

case class TransactionSuccess(outputs: Map[String, String] = Map()) extends TransactionResult

object TransactionSuccess {
  def apply(output: String): TransactionSuccess = TransactionSuccess(Map("" -> output))

  def apply(outputs: (String, String)*): TransactionSuccess = TransactionSuccess(Map(outputs: _*))
}
