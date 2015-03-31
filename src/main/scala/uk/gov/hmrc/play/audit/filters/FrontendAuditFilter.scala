/*
 * Copyright 2015 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.play.audit.filters

import play.api.http.HeaderNames
import play.api.mvc.{RequestHeader, ResponseHeader}
import uk.gov.hmrc.play.audit.EventTypes
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.audit.model.{DataEvent, DeviceFingerprint}

trait FrontendAuditFilter extends AuditFilter {

  private val textHtml = ".*(text/html).*".r

  def maskedFormFields: Seq[String]

  def applicationPort: Option[Int]

  override def buildAuditedHeaders(request: RequestHeader) = HeaderCarrier.fromSessionAndHeaders(request.session, request.headers)

  override def buildAuditRequestEvent(eventType: EventTypes.EventType, request: RequestHeader, requestBody: String)(implicit hc: HeaderCarrier): DataEvent = {

    super.buildAuditRequestEvent(eventType, request, stripPasswords(request.contentType, requestBody, maskedFormFields))
      .withDetail(buildRequestDetails(request).toSeq: _*)
  }

  override def buildAuditResponseEvent(eventType: EventTypes.EventType, request: RequestHeader, response: ResponseHeader, responseBody: String)(implicit hc: HeaderCarrier): DataEvent = {
    super.buildAuditResponseEvent(eventType, request, response, filterResponseBody(response, responseBody))
      .withDetail(buildResponseDetails(response).toSeq: _*)
  }

  private def filterResponseBody(response: ResponseHeader, responseBody: String) = {
    response.headers.get("Content-Type")
      .collect { case textHtml(a) => "<HTML>...</HTML>"}
      .getOrElse(responseBody)
  }

  private def buildRequestDetails(request: RequestHeader)(implicit hc: HeaderCarrier): Map[String, String] = {

    val details = new collection.mutable.HashMap[String, String]
    details.put("deviceFingerprint", DeviceFingerprint.deviceFingerprintFrom(request))

    details.put("host", getHost(request))
    details.put("port", getPort)
    details.put("queryString", getQueryString(request.queryString))

    details.toMap
  }

  private def buildResponseDetails(response: ResponseHeader)(implicit hc: HeaderCarrier): Map[String, String] = {

    val details = new collection.mutable.HashMap[String, String]

    response.headers.get(HeaderNames.LOCATION).map { location =>
      details.put(HeaderNames.LOCATION, location)
    }

    details.toMap
  }

  private[filters] def getQueryString(queryString: Map[String, Seq[String]]): String = {
    cleanQueryStringForDatastream(queryString.foldLeft[String]("") {
      (stringRepresentation, mapOfArgs) =>
        val spacer = stringRepresentation match {
          case "" => "";
          case _ => "&"
        }

        stringRepresentation + spacer + mapOfArgs._1 + ":" + getQueryStringValue(mapOfArgs._2)
    })
  }

  private[filters] def getHost(request: RequestHeader) = {
    request.headers.get("Host").map(_.takeWhile(_ != ':')).getOrElse("-")
  }

  private[filters] def getPort = applicationPort.map(_.toString).getOrElse("-")


  private[filters] def stripPasswords(contentType: Option[String], requestBody: String, maskedFormFields: Seq[String]): String = {
    contentType match {
      case Some("application/x-www-form-urlencoded") => maskedFormFields.foldLeft(requestBody)((maskedBody, field) =>
        maskedBody.replaceAll(field + """=.*?(?=&|$|\s)""", field + "=#########"))
      case _ => requestBody
    }
  }

  private def getQueryStringValue(seqOfArgs: Seq[String]): String = {
    seqOfArgs.foldLeft("")(
      (queryStringArrayConcat, queryStringArrayItem) => {
        val queryStringArrayPrepend = queryStringArrayConcat match {
          case "" => ""
          case _ => ","
        }

        queryStringArrayConcat + queryStringArrayPrepend + queryStringArrayItem
      }
    )
  }

  private def cleanQueryStringForDatastream(queryString: String): String = {
    queryString.trim match {
      case "" => "-"
      case _ => queryString.trim
    }
  }

}
