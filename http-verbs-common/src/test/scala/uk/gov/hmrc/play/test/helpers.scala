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

package uk.gov.hmrc.play.test

import play.api.libs.json.Writes
import uk.gov.hmrc.http._

import scala.concurrent.{ExecutionContext, Future}

trait TestHttpCore extends CorePost with CoreGet with CorePut with CorePatch with CoreDelete with Request {

  override def POST[I, O](
    url: String,
    body: I,
    headers: Seq[(String, String)])(
      implicit wts: Writes[I],
      rds: HttpReads[O],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[O] =
    ???

  override def POSTString[O](
    url: String,
    body: String,
    headers: Seq[(String, String)])(
      implicit rds: HttpReads[O],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[O] =
    ???

  override def POSTForm[O](
    url: String,
    body: Map[String, Seq[String]],
    headers: Seq[(String, String)])(
      implicit rds: HttpReads[O],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[O] =
    ???

  override def POSTEmpty[O](
    url: String,
    headers: Seq[(String, String)])(
      implicit rds: HttpReads[O],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[O] =
    ???

  override def GET[A](
    url: String,
    queryParams: Seq[(String, String)],
    headers: Seq[(String, String)])(
      implicit rds: HttpReads[A],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[A] =
    ???

  override def PUT[I, O](
    url: String,
    body: I,
    headers: Seq[(String, String)])(
      implicit wts: Writes[I],
      rds: HttpReads[O],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[O] =
    ???

  override def PUTString[O](
    url: String,
    body: String,
    headers: Seq[(String, String)])(
      implicit rds: HttpReads[O],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[O] =
    ???

  override def PATCH[I, O](
    url: String,
    body: I,
    headers: Seq[(String, String)])(
      implicit wts: Writes[I],
      rds: HttpReads[O],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[O] =
    ???

  override def DELETE[O](
    url: String,
    headers: Seq[(String, String)])(
      implicit rds: HttpReads[O],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[O] =
    ???
}
