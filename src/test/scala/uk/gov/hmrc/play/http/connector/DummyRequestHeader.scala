package uk.gov.hmrc.play.http.connector

import play.api.mvc.{Headers, RequestHeader}
import play.api.test.FakeHeaders


class DummyRequestHeader extends RequestHeader {

  override def remoteAddress: String = ???

  override def headers: Headers = FakeHeaders(Seq.empty)

  override def queryString: Map[String, Seq[String]] = ???

  override def version: String = ???

  override def method: String = "GET"

  override def path: String = "/"

  override def uri: String = "/"

  override def tags: Map[String, String] = ???

  override def id: Long = ???

  override def secure: Boolean = false
}
