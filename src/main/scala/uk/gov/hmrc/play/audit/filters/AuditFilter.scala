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

package uk.gov.hmrc.play.audit.filters

import play.api.mvc.{Result, _}
import uk.gov.hmrc.play.audit.EventKeys._
import uk.gov.hmrc.play.audit.EventTypes
import uk.gov.hmrc.play.audit.EventTypes._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.{HeaderCarrier, HttpAuditEvent}
import uk.gov.hmrc.play.audit.model.DataEvent

trait AuditFilter extends EssentialFilter with HttpAuditEvent {

  def auditConnector: AuditConnector

  def controllerNeedsAuditing(controllerName: String): Boolean

  import play.api.Routes
  import play.api.libs.iteratee._
  import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

  def apply(nextAction: EssentialAction): EssentialAction = new EssentialAction {
    def apply(request: RequestHeader): Iteratee[Array[Byte], Result] = {
      audit(request, nextAction)
    }
  }

  def buildAuditedHeaders(request: RequestHeader) = HeaderCarrier.fromHeaders(request.headers)

  def audit(request: RequestHeader, nextAction: EssentialAction) = {
    implicit val hc = buildAuditedHeaders(request)
    if (needsAuditing(request)) withAuditedResponse(withAuditedRequest(nextAction, request), request)
    else nextAction(request)
  }

  def needsAuditing(request: RequestHeader): Boolean = {
    (for {
      controllerName <- request.tags.get(Routes.ROUTE_CONTROLLER)
    } yield controllerNeedsAuditing(controllerName)).getOrElse(true)
  }

  def withAuditedRequest(nextAction: EssentialAction, request: RequestHeader)(implicit hc: HeaderCarrier): Iteratee[Array[Byte], Result] = {
    readRequestBody(nextAction, request) {
      body => {
        val event = buildAuditRequestEvent(EventTypes.ServiceReceivedRequest, request, new String(body))
        auditConnector.sendEvent(event)
      }
    }
  }

  def withAuditedResponse(iteratee: Iteratee[Array[Byte], Result], request: RequestHeader)(implicit hc: HeaderCarrier): Iteratee[Array[Byte], Result] = {
    iteratee.map {
      result =>
        var collectedBody = new Array[Byte](0)
        val mappedBody = result.body.map {
          i =>
            collectedBody = collectedBody ++ i
            i
        }.onDoneEnumerating {

          val event = buildAuditResponseEvent(EventTypes.ServiceSentResponse, request, result.header, new String(collectedBody))
          auditConnector.sendEvent(event)
        }
        result.copy(body = mappedBody)
    }
  }

  def buildAuditRequestEvent(eventType: EventType, request: RequestHeader, requestBody: String)(implicit hc: HeaderCarrier): DataEvent = {
    dataEvent(eventType, request.uri, request).withDetail(RequestBody -> requestBody)
  }

  def buildAuditResponseEvent(eventType: EventType, request: RequestHeader, response: ResponseHeader, responseBody: String)(implicit hc: HeaderCarrier): DataEvent = {
    dataEvent(eventType, request.uri, request).withDetail(ResponseMessage -> responseBody, StatusCode -> response.status.toString)
  }

  def readRequestBody(nextA: EssentialAction, request: RequestHeader)(bodyHandler: (Array[Byte]) => Unit): Iteratee[Array[Byte], Result] = {

    def step(body: Array[Byte], nextI: Iteratee[Array[Byte], Result])(input: Input[Array[Byte]]): Iteratee[Array[Byte], Result] = {
      input match {

        case Input.EOF => {
          bodyHandler(body)
          Iteratee.flatten(nextI.feed(Input.EOF))
        }

        case Input.Empty => Cont[Array[Byte], Result](step(body, nextI))

        case Input.El(e) => {
          val curBody = Array.concat(body, e)
          Cont[Array[Byte], Result](step(curBody, Iteratee.flatten(nextI.feed(Input.El(e)))))
        }
      }
    }

    val nextIteratee: Iteratee[Array[Byte], Result] = nextA(request)

    Cont[Array[Byte], Result](i => step(Array(), nextIteratee)(i))
  }
}
