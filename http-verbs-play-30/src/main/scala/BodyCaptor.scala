/*
 * Copyright 2023 HM Revenue & Customs
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

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.{Attributes, FlowShape, Inlet, Outlet}
import org.apache.pekko.stream.scaladsl.{Flow, Sink}
import org.apache.pekko.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import org.apache.pekko.util.ByteString
import uk.gov.hmrc.http.hooks.Data

// based on play.filters.csrf.CSRFAction#BodyHandler

private class BodyCaptorFlow(
  maxBodyLength   : Int,
  withCapturedBody: Data[ByteString] => Unit
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
            withCapturedBody(BodyCaptor.bodyUpto(buffer, maxBodyLength))
            completeStage()
          }
        }
      )
    }
}

object BodyCaptor {
  def flow(
    maxBodyLength   : Int,
    withCapturedBody: Data[ByteString] => Unit // provide a callback since a Materialized value would be not be available until the flow has been run
  ): Flow[ByteString, ByteString, NotUsed] =
    Flow.fromGraph(new BodyCaptorFlow(
      maxBodyLength    = maxBodyLength,
      withCapturedBody = withCapturedBody
    ))

  def sink(
    maxBodyLength   : Int,
    withCapturedBody: Data[ByteString] => Unit
  ): Sink[ByteString, NotUsed] =
    flow(maxBodyLength, withCapturedBody)
      .to(Sink.ignore)

  def bodyUpto(body: ByteString, maxBodyLength: Int): Data[ByteString] =
    if (body.length > maxBodyLength)
      Data.truncated(body.take(maxBodyLength))
    else
      Data.pure(body)
}
