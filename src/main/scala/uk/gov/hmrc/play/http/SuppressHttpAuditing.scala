package uk.gov.hmrc.play.http

import scala.concurrent.Future
import uk.gov.hmrc.play.audit.http.{HeaderCarrier, HttpAuditing}

trait SuppressHttpAuditing extends HttpAuditing {

  override protected def auditRequestWithResponseF(url: String, verb: String, body: Option[_], responseToAuditF: Future[HttpResponse])(implicit hc: HeaderCarrier) {}

}
