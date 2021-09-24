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

package uk.gov.hmrc.http

import java.net.URL

import _root_.play.api.libs.json.Writes

import scala.concurrent.{ExecutionContext, Future}

trait GetHttpTransport {
  def doGet(
    url: String,
    headers: Seq[(String, String)] = Seq.empty)(
      implicit ec: ExecutionContext): Future[HttpResponse]
}

trait DeleteHttpTransport {
  def doDelete(
    url: String,
    headers: Seq[(String, String)] = Seq.empty)(
      implicit ec: ExecutionContext): Future[HttpResponse]
}

trait PatchHttpTransport {
  def doPatch[A](
    url: String,
    body: A,
    headers: Seq[(String, String)] = Seq.empty)(
      implicit rds: Writes[A],
      ec: ExecutionContext): Future[HttpResponse]
}

trait PutHttpTransport {
  def doPut[A](
    url: String,
    body: A,
    headers: Seq[(String, String)] = Seq.empty)(
      implicit rds: Writes[A],
      ec: ExecutionContext): Future[HttpResponse]

  def doPutString(
    url: String,
    body: String,
    headers: Seq[(String, String)] = Seq.empty)(
      implicit ec: ExecutionContext): Future[HttpResponse]
}

trait PostHttpTransport {
  def doPost[A](
    url: String,
    body: A,
    headers: Seq[(String, String)] = Seq.empty)(
      implicit wts: Writes[A],
      ec: ExecutionContext): Future[HttpResponse]

  def doPostString(
    url: String,
    body: String,
    headers: Seq[(String, String)] = Seq.empty)(
      implicit ec: ExecutionContext): Future[HttpResponse]

  def doEmptyPost[A](
    url: String,
    headers: Seq[(String, String)] = Seq.empty)(
    implicit ec: ExecutionContext): Future[HttpResponse]

  def doFormPost(
    url: String,
    body: Map[String, Seq[String]],
    headers: Seq[(String, String)] = Seq.empty)(
    implicit ec: ExecutionContext): Future[HttpResponse]
}

trait HttpTransport
    extends GetHttpTransport
    with DeleteHttpTransport
    with PatchHttpTransport
    with PutHttpTransport
    with PostHttpTransport {}

trait CoreGet {

  final def GET[A](
    url: URL)(
      implicit rds: HttpReads[A],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[A] =
    GET(url.toString, Seq.empty, Seq.empty)

  def GET[A](
    url: URL,
    headers: Seq[(String, String)])(
      implicit rds: HttpReads[A],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[A] =
    GET(url.toString, Seq.empty, headers)

  def GET[A](
    url: String,
    queryParams: Seq[(String, String)] = Seq.empty,
    headers: Seq[(String, String)] = Seq.empty)(
      implicit rds: HttpReads[A],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[A]

}

trait CoreDelete {

  final def DELETE[O](
    url: URL)(
      implicit rds: HttpReads[O],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[O] =
    DELETE(url.toString, Seq.empty)

  def DELETE[O](
    url: URL,
    headers: Seq[(String, String)])(
      implicit rds: HttpReads[O],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[O] =
    DELETE(url.toString, headers)

  def DELETE[O](
    url: String,
    headers: Seq[(String, String)] = Seq.empty)(
      implicit rds: HttpReads[O],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[O]
}

trait CorePatch {

  final def PATCH[I, O](
    url: URL,
    body: I)(
      implicit wts: Writes[I],
      rds: HttpReads[O],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[O] =
    PATCH(url.toString, body, Seq.empty)

  def PATCH[I, O](
    url: URL,
    body: I,
    headers: Seq[(String, String)])(
      implicit wts: Writes[I],
      rds: HttpReads[O],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[O] =
    PATCH(url.toString, body, headers)

  def PATCH[I, O](
    url: String,
    body: I,
    headers: Seq[(String, String)] = Seq.empty)(
      implicit wts: Writes[I],
      rds: HttpReads[O],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[O]
}

trait CorePut {

  final def PUT[I, O](
    url: URL,
    body: I)(
      implicit wts: Writes[I],
      rds: HttpReads[O],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[O] =
    PUT(url.toString, body, Seq.empty)

  def PUT[I, O](
    url: URL,
    body: I,
    headers: Seq[(String, String)])(
      implicit wts: Writes[I],
      rds: HttpReads[O],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[O] =
    PUT(url.toString, body, headers)

  def PUT[I, O](
    url: String,
    body: I,
    headers: Seq[(String, String)] = Seq.empty)(
      implicit wts: Writes[I],
      rds: HttpReads[O],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[O]

  final def PUTString[O](
    url: URL,
    body: String)(
      implicit rds: HttpReads[O],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[O] =
    PUTString(url.toString, body, Seq.empty)

  def PUTString[O](
    url: URL,
    body: String,
    headers: Seq[(String, String)])(
      implicit rds: HttpReads[O],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[O] =
    PUTString(url.toString, body, headers)

  def PUTString[O](
    url: String,
    body: String,
    headers: Seq[(String, String)] = Seq.empty)(
      implicit rds: HttpReads[O],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[O]
}

trait CorePost {

  final def POST[I, O](
    url: URL,
    body: I)(
      implicit wts: Writes[I],
      rds: HttpReads[O],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[O] =
    POST(url.toString, body, Seq.empty)

  def POST[I, O](
    url: URL,
    body: I,
    headers: Seq[(String, String)])(
      implicit wts: Writes[I],
      rds: HttpReads[O],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[O] =
    POST(url.toString, body, headers)

  def POST[I, O](
    url: String,
    body: I,
    headers: Seq[(String, String)] = Seq.empty)(
      implicit wts: Writes[I],
      rds: HttpReads[O],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[O]

  final def POSTString[O](
    url: URL,
    body: String)(
      implicit rds: HttpReads[O],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[O] =
    POSTString(url.toString, body, Seq.empty)

  def POSTString[O](
    url: URL,
    body: String,
    headers: Seq[(String, String)])(
      implicit rds: HttpReads[O],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[O] =
    POSTString(url.toString, body, headers)

  def POSTString[O](
    url: String,
    body: String,
    headers: Seq[(String, String)] = Seq.empty)(
      implicit rds: HttpReads[O],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[O]

  final def POSTForm[O](
    url: URL,
    body: Map[String, Seq[String]])(
      implicit rds: HttpReads[O],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[O] =
    POSTForm(url.toString, body, Seq.empty)

  def POSTForm[O](
    url: URL,
    body: Map[String, Seq[String]],
    headers: Seq[(String, String)])(
      implicit rds: HttpReads[O],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[O] =
    POSTForm(url.toString, body, headers)

  def POSTForm[O](
    url: String,
    body: Map[String, Seq[String]],
    headers: Seq[(String, String)] = Seq.empty)(
      implicit rds: HttpReads[O],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[O]

  final def POSTEmpty[O](
    url: URL)(
      implicit rds: HttpReads[O],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[O] =
    POSTEmpty(url.toString, Seq.empty)

  def POSTEmpty[O](
    url: URL,
    headers: Seq[(String, String)])(
      implicit rds: HttpReads[O],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[O] =
    POSTEmpty(url.toString, headers)

  def POSTEmpty[O](
    url: String,
    headers: Seq[(String, String)] = Seq.empty)(
      implicit rds: HttpReads[O],
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[O]
}
