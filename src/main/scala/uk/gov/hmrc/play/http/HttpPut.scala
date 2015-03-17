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

package uk.gov.hmrc.play.http

import play.api.libs.json.{Json, Writes}
import play.api.http.HttpVerbs.{PUT => PUT_VERB}
import uk.gov.hmrc.play.audit.http.{HeaderCarrier, HttpAuditing}
import uk.gov.hmrc.play.http.logging.{MdcLoggingExecutionContext, ConnectionTracing}
import MdcLoggingExecutionContext._

import scala.concurrent.Future

trait HttpPut extends HttpVerb with ConnectionTracing with HttpAuditing {

  protected def doPut[A](url: String, body: A)(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse]

  private def defaultHandler(responseF: Future[HttpResponse], url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = responseF.map(handleResponse(PUT_VERB, url))

  def PUT[I, O](url: String, body: I)(implicit wts: Writes[I], rds: HttpReads[O], hc: HeaderCarrier): Future[O] = {
    withTracing(PUT_VERB, url) {
      val httpResponse = doPut(url, body)
      auditRequestWithResponseF(url, PUT_VERB, Option(Json.stringify(wts.writes(body))), httpResponse)
      mapErrors(PUT_VERB, url, httpResponse).map(response => rds.read(PUT_VERB, url, response))
    }
  }

  @deprecated("auditRequestBody/auditResponseBody are no longer supported, use PUT(url, body) and configuration instead")
  def PUT[A](url: String, body: A, auditRequestBody: Boolean, auditResponseBody: Boolean)(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = {
    PUT(url, body, defaultHandler, auditRequestBody, auditResponseBody)
  }

  @deprecated("auditRequestBody/auditResponseBody are no longer supported, use PUT(url, body) and configuration instead. ProcessingFunction is obselete, use the relevant HttpReads[A] instead")
  def PUT[A](url: String, body: A, responseHandler: ProcessingFunction,  auditRequestBody: Boolean = true, auditResponseBody: Boolean = true)(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = {
    withTracing(PUT_VERB, url) {
      val httpResponse = doPut(url, body)
      auditRequestWithResponseF(url, PUT_VERB, Option(Json.stringify(rds.writes(body))), httpResponse)
      responseHandler(mapErrors(PUT_VERB, url, httpResponse), url)
    }
  }
}
