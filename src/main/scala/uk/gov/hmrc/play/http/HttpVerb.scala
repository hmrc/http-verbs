package uk.gov.hmrc.play.http

import java.net.ConnectException
import java.util.concurrent.TimeoutException
import uk.gov.hmrc.play.audit.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait HttpVerb extends HttpErrorFunctions {

  type ProcessingFunction = (Future[HttpResponse], String) => Future[HttpResponse]

  protected[http] def mapErrors(httpMethod: String, url: String, f: Future[HttpResponse]): Future[HttpResponse] =
    f.recover {
      case e: TimeoutException => throw new GatewayTimeoutException(gatewayTimeoutMessage(httpMethod, url, e))
      case e: ConnectException => throw new BadGatewayException(badGatewayMessage(httpMethod, url, e))
  }
}
