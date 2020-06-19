/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.examples.utils

import play.api.libs.json.{Json, Reads}
import java.time.LocalDate
import scala.io.Source

object XmlPayloads {
  val bankHolidays: String = Source.fromFile(getClass.getResource("/bankHolidays.xml").toURI, "UTF-8").getLines.mkString
}

object JsonPayloads {
  val bankHolidays: String = Source.fromFile(getClass.getResource("/bankHolidays.json").toURI, "UTF-8").getLines.mkString
  val userId: String = Source.fromFile(getClass.getResource("/userId.json").toURI, "UTF-8").getLines.mkString
}

case class BankHolidays(events: Seq[BankHoliday])
case class BankHoliday(title: String, date: LocalDate)

object BankHolidays {
  implicit val bhr: Reads[BankHoliday] = Json.reads[BankHoliday]
  val reads: Reads[BankHolidays] = Json.reads[BankHolidays]
}

case class User(email: String, fullName: String)

object User {
  val writes = Json.writes[User]
}

case class UserIdentifier(id: String)

object UserIdentifier {
  val reads = Json.reads[UserIdentifier]
}
