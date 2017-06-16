/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.play.http.filter

import org.slf4j.{Marker, Logger}

trait FakeLogger extends Logger{
  override def warn(s: String): Unit = ???

  override def warn(s: String, o: scala.Any): Unit = ???

  override def warn(s: String, objects: AnyRef*): Unit = ???

  override def warn(s: String, o: scala.Any, o1: scala.Any): Unit = ???

  override def warn(s: String, throwable: Throwable): Unit = ???

  override def warn(marker: Marker, s: String): Unit = ???

  override def warn(marker: Marker, s: String, o: scala.Any): Unit = ???

  override def warn(marker: Marker, s: String, o: scala.Any, o1: scala.Any): Unit = ???

  override def warn(marker: Marker, s: String, objects: AnyRef*): Unit = ???

  override def warn(marker: Marker, s: String, throwable: Throwable): Unit = ???

  override def isErrorEnabled: Boolean = ???

  override def isErrorEnabled(marker: Marker): Boolean = ???

  override def getName: String = ???

  override def isInfoEnabled: Boolean = ???

  override def isInfoEnabled(marker: Marker): Boolean = ???

  override def isDebugEnabled: Boolean = ???

  override def isDebugEnabled(marker: Marker): Boolean = ???

  override def isTraceEnabled: Boolean = ???

  override def isTraceEnabled(marker: Marker): Boolean = ???

  override def error(s: String): Unit = ???

  override def error(s: String, o: scala.Any): Unit = ???

  override def error(s: String, o: scala.Any, o1: scala.Any): Unit = ???

  override def error(s: String, objects: AnyRef*): Unit = ???

  override def error(s: String, throwable: Throwable): Unit = ???

  override def error(marker: Marker, s: String): Unit = ???

  override def error(marker: Marker, s: String, o: scala.Any): Unit = ???

  override def error(marker: Marker, s: String, o: scala.Any, o1: scala.Any): Unit = ???

  override def error(marker: Marker, s: String, objects: AnyRef*): Unit = ???

  override def error(marker: Marker, s: String, throwable: Throwable): Unit = ???

  override def debug(s: String): Unit = ???

  override def debug(s: String, o: scala.Any): Unit = ???

  override def debug(s: String, o: scala.Any, o1: scala.Any): Unit = ???

  override def debug(s: String, objects: AnyRef*): Unit = ???

  override def debug(s: String, throwable: Throwable): Unit = ???

  override def debug(marker: Marker, s: String): Unit = ???

  override def debug(marker: Marker, s: String, o: scala.Any): Unit = ???

  override def debug(marker: Marker, s: String, o: scala.Any, o1: scala.Any): Unit = ???

  override def debug(marker: Marker, s: String, objects: AnyRef*): Unit = ???

  override def debug(marker: Marker, s: String, throwable: Throwable): Unit = ???

  override def isWarnEnabled: Boolean = ???

  override def isWarnEnabled(marker: Marker): Boolean = ???

  override def trace(s: String): Unit = ???

  override def trace(s: String, o: scala.Any): Unit = ???

  override def trace(s: String, o: scala.Any, o1: scala.Any): Unit = ???

  override def trace(s: String, objects: AnyRef*): Unit = ???

  override def trace(s: String, throwable: Throwable): Unit = ???

  override def trace(marker: Marker, s: String): Unit = ???

  override def trace(marker: Marker, s: String, o: scala.Any): Unit = ???

  override def trace(marker: Marker, s: String, o: scala.Any, o1: scala.Any): Unit = ???

  override def trace(marker: Marker, s: String, objects: AnyRef*): Unit = ???

  override def trace(marker: Marker, s: String, throwable: Throwable): Unit = ???

  override def info(s: String): Unit = ???

  override def info(s: String, o: scala.Any): Unit = ???

  override def info(s: String, o: scala.Any, o1: scala.Any): Unit = ???

  override def info(s: String, objects: AnyRef*): Unit = ???

  override def info(s: String, throwable: Throwable): Unit = ???

  override def info(marker: Marker, s: String): Unit = ???

  override def info(marker: Marker, s: String, o: scala.Any): Unit = ???

  override def info(marker: Marker, s: String, o: scala.Any, o1: scala.Any): Unit = ???

  override def info(marker: Marker, s: String, objects: AnyRef*): Unit = ???

  override def info(marker: Marker, s: String, throwable: Throwable): Unit = ???
}
