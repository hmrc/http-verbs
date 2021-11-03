/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.http.play

import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.stream.scaladsl.{Flow, Sink}
import akka.stream.stage._
import akka.util.ByteString
import play.api.Logger

// based on play.filters.csrf.CSRFAction#BodyHandler

private class BodyCaptorFlow(
  loggingContext  : String,
  maxBodyLength   : Int,
  withCapturedBody: ByteString => Unit
) extends GraphStage[FlowShape[ByteString, ByteString]] {
  val in             = Inlet[ByteString]("BodyCaptorFlow.in")
  val out            = Outlet[ByteString]("BodyCaptorFlow.out")
  override val shape = FlowShape.of(in, out)

  private val logger = Logger(getClass)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {
      private var buffer     = ByteString.empty
      private var bodyLength = 0

      setHandlers(
        in,
        out,
        new InHandler with OutHandler {
          override def onPull(): Unit =
            pull(in)

          override def onPush(): Unit = {
            val chunk = grab(in)
            bodyLength += chunk.length
            if (buffer.size < maxBodyLength)
              buffer ++= chunk
            push(out, chunk)
          }

          override def onUpstreamFinish(): Unit = {
            // TODO how to opt out of auditing payloads?
            // currently we can only turn of auditing for url (auditDisabledForPattern) - not per method
            // and we can't turn off auditing of just the payload (and keep the fact the call has been made)
            // what about auditing request payloads, but not response payloads?
            if (bodyLength > maxBodyLength)
              logger.warn(
                s"txm play auditing: $loggingContext sanity check request body $bodyLength exceeds maxLength $maxBodyLength - do you need to be auditing this payload?"
              )
            withCapturedBody(buffer.take(maxBodyLength))
            if (isAvailable(out) && buffer == ByteString.empty)
              push(out, buffer)
            completeStage()
          }
        }
      )
    }
}

object BodyCaptor {
  def flow(
    loggingContext  : String,
    maxBodyLength   : Int,
    withCapturedBody: ByteString => Unit
  ): Flow[ByteString, ByteString, akka.NotUsed] =
    Flow.fromGraph(new BodyCaptorFlow(
      loggingContext   = loggingContext,
      maxBodyLength    = maxBodyLength,
      withCapturedBody = withCapturedBody
    ))

  def sink(
    loggingContext  : String,
    maxBodyLength   : Int,
    withCapturedBody: ByteString => Unit // TODO can we not just expose as the materialised value?
  ): Sink[ByteString, akka.NotUsed] =
    flow(loggingContext, maxBodyLength, withCapturedBody)
      .to(Sink.ignore)
}
