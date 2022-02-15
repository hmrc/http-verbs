/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.play.http

import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.stream.scaladsl.{Flow, Sink}
import akka.stream.stage._
import akka.util.ByteString
import org.slf4j.LoggerFactory

// based on play.filters.csrf.CSRFAction#BodyHandler

private class BodyCaptorFlow(
  loggingContext  : String,
  maxBodyLength   : Int,
  withCapturedBody: ByteString => Unit
) extends GraphStage[FlowShape[ByteString, ByteString]] {
  val in             = Inlet[ByteString]("BodyCaptorFlow.in")
  val out            = Outlet[ByteString]("BodyCaptorFlow.out")
  override val shape = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {
      private var buffer = ByteString.empty

      setHandlers(
        in,
        out,
        new InHandler with OutHandler {
          override def onPull(): Unit =
            pull(in)

          override def onPush(): Unit = {
            val chunk = grab(in)
            if (buffer.size < maxBodyLength)
              buffer ++= chunk
            push(out, chunk)
          }

          override def onUpstreamFinish(): Unit = {
            withCapturedBody(BodyCaptor.bodyUpto(buffer, maxBodyLength, loggingContext, isStream = true))
            if (isAvailable(out) && buffer == ByteString.empty)
              push(out, buffer)
            completeStage()
          }
        }
      )
    }
}

object BodyCaptor {
  private val logger = LoggerFactory.getLogger(getClass)

  def flow(
    loggingContext  : String,
    maxBodyLength   : Int,
    withCapturedBody: ByteString => Unit // provide a callback since a Materialized value would be not be available until the flow has been run
  ): Flow[ByteString, ByteString, akka.NotUsed] =
    Flow.fromGraph(new BodyCaptorFlow(
      loggingContext   = loggingContext,
      maxBodyLength    = maxBodyLength,
      withCapturedBody = withCapturedBody
    ))

  def sink(
    loggingContext  : String,
    maxBodyLength   : Int,
    withCapturedBody: ByteString => Unit
  ): Sink[ByteString, akka.NotUsed] =
    flow(loggingContext, maxBodyLength, withCapturedBody)
      .to(Sink.ignore)

  def bodyUpto(body: String, maxBodyLength: Int, loggingContext: String, isStream: Boolean): String =
    if (body.length > maxBodyLength) {
      logger.warn(
        s"$loggingContext ${if (isStream) "streamed body" else "body " + body.length} exceeds maxLength $maxBodyLength - truncating"
      )
      body.take(maxBodyLength)
    } else
      body

  def bodyUpto(body: ByteString, maxBodyLength: Int, loggingContext: String, isStream: Boolean): ByteString =
    if (body.length > maxBodyLength) {
      logger.warn(
        s"$loggingContext ${if (isStream) "streamed body" else "body " + body.length} exceeds maxLength $maxBodyLength - truncating"
      )
      body.take(maxBodyLength)
    } else
      body
}
