package uk.gov.hmrc.play.controllers

import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json._
import play.api.libs.json.JsString
import org.joda.time.{LocalDate, DateTime, DateTimeZone, LocalDateTime}
import scala.util.Try

object RestFormats extends RestFormats

trait RestFormats {

  private val dateTimeFormat = ISODateTimeFormat.dateTime.withZoneUTC
  private val localDateRegex = """^(\d\d\d\d)-(\d\d)-(\d\d)$""".r

  implicit val localDateTimeRead: Reads[LocalDateTime] = new Reads[LocalDateTime] {
    override def reads(json: JsValue): JsResult[LocalDateTime] = {
      json match {
        case JsString(s) => Try {
          JsSuccess(new LocalDateTime(dateTimeFormat.parseDateTime(s), DateTimeZone.UTC))
        }.getOrElse {
          JsError(s"Could not parse $s as a DateTime with format ${dateTimeFormat.toString}")
        }
        case _ => JsError(s"Expected value to be a string, was actually $json")
      }
    }
  }

  implicit val localDateTimeWrite: Writes[LocalDateTime] = new Writes[LocalDateTime] {
    def writes(dateTime: LocalDateTime): JsValue = JsString(dateTimeFormat.print(dateTime.toDateTime(DateTimeZone.UTC)))
  }

  implicit val dateTimeRead: Reads[DateTime] = new Reads[DateTime] {
    override def reads(json: JsValue): JsResult[DateTime] = {
      json match {
        case JsString(s) => Try {
          JsSuccess(dateTimeFormat.parseDateTime(s))
        }.getOrElse {
          JsError(s"Could not parse $s as a DateTime with format ${dateTimeFormat.toString}")
        }
        case _ => JsError(s"Expected value to be a string, was actually $json")
      }
    }
  }

  implicit val dateTimeWrite: Writes[DateTime] = new Writes[DateTime] {
    def writes(dateTime: DateTime): JsValue = JsString(dateTimeFormat.print(dateTime))
  }

  implicit val localDateRead: Reads[LocalDate] = new Reads[LocalDate] {
    override def reads(json: JsValue): JsResult[LocalDate] = {
      json match {
        case JsString(s@localDateRegex(y, m, d)) => Try {
          JsSuccess(new LocalDate(y.toInt, m.toInt, d.toInt))
        }.getOrElse {
          JsError(s"$s is not a valid date")
        }
        case JsString(s) => JsError(s"Cannot parse $s as a LocalDate")
        case _ => JsError(s"Expected value to be a string, was actually $json")
      }
    }
  }

  implicit val localDateWrite: Writes[LocalDate] = new Writes[LocalDate] {
    def writes(date: LocalDate): JsValue = JsString("%04d-%02d-%02d".format(date.getYear, date.getMonthOfYear, date.getDayOfMonth))
  }

  implicit val dateTimeFormats = Format(dateTimeRead, dateTimeWrite)
  implicit val localDateTimeFormats = Format(localDateTimeRead, localDateTimeWrite)
  implicit val localDateFormats = Format(localDateRead, localDateWrite)

}
