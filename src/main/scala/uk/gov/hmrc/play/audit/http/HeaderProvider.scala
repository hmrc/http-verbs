package uk.gov.hmrc.play.audit.http

trait HeaderProvider {
  def headers: Seq[(String, String)]
}
