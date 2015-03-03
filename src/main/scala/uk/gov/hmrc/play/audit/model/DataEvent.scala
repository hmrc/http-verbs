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

import java.util.{TimeZone, UUID}

import org.apache.commons.lang3.time.FastDateFormat
import org.joda.time.DateTime
import play.api.libs.json._
import uk.gov.hmrc.time.DateTimeUtils

sealed trait AuditEvent {
  def auditSource: String
  def auditType: String
  def eventId: String
  def tags: Map[String, String]
  def generatedAt: DateTime

}

object DateWriter {
  implicit def dateTimeWrites = new Writes[DateTime] {
    private val dateFormat = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSZ", TimeZone.getTimeZone("UTC"))

    def writes(dt: DateTime): JsValue = JsString(dateFormat.format(dt.getMillis))
  }
}

object AuditEvent {

  implicit val dateWriter = DateWriter.dateTimeWrites
  val dataEventFormat = Json.format[DataEvent]
  val extendedDataEventFormat = Json.format[ExtendedDataEvent]
  implicit val auditEventFormat= new Writes[AuditEvent] {

    def writes(event: AuditEvent): JsValue = event match {
        case dataEvent: DataEvent => dataEventFormat.writes(dataEvent)
        case extendedDataEvent: ExtendedDataEvent => extendedDataEventFormat.writes(extendedDataEvent)
    }
  }
}

case class DataEvent(override val auditSource: String,
                     override val auditType: String,
                     override val eventId: String = UUID.randomUUID().toString,
                     override val tags: Map[String, String] = Map.empty,
                     detail: Map[String, String] = Map.empty,
                     override  val generatedAt: DateTime = DateTimeUtils.now) extends AuditEvent {

  /**
   * @return a copy of this DataEvent with the extra details added to the current details
   */
  def withDetail(moreDetail: (String, String)*) = copy(detail = detail ++ moreDetail)

  /**
   * @return a copy of this DataEvent with the extra tags added to the current tags
   */
  def withTags(moreTags: (String, String)*) = copy(tags = tags ++ moreTags)
}

object DataEvent {
  implicit val dateWriter = DateWriter.dateTimeWrites
  implicit val eventFormats = Json.format[DataEvent]
}


case class ExtendedDataEvent(override val auditSource: String,
                             override val auditType: String,
                             override val eventId: String = UUID.randomUUID().toString,
                             override val tags: Map[String, String] = Map.empty,
                             detail: JsValue = JsString(""),
                             override val generatedAt: DateTime = DateTimeUtils.now) extends AuditEvent


object ExtendedDataEvent {
  implicit val dateWriter = DateWriter.dateTimeWrites
  implicit val eventFormats = Json.format[ExtendedDataEvent]
}

case class DataCall(tags: Map[String, String],
                    detail: Map[String, String],
                    generatedAt: DateTime)

case class MergedDataEvent(auditSource: String,
                           auditType: String,
                           eventId: String = UUID.randomUUID().toString,
                           request: DataCall,
                           response: DataCall)

object MergedDataEvent {

  implicit val dateTimeWrites = DateWriter.dateTimeWrites
  implicit val callFormats = Json.format[DataCall]
  implicit val eventFormats = Json.format[MergedDataEvent]
}
