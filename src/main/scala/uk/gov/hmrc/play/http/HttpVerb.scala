package uk.gov.hmrc.play.http

import java.net.ConnectException
import java.util.concurrent.TimeoutException
import uk.gov.hmrc.play.audit.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait HttpVerb {
  protected def notFoundMessage(verbName: String, url: String, responseBody: String): String = {
    s"$verbName of '$url' returned 404 (Not Found). Response body: '$responseBody'"
  }

  protected def preconditionFailedMessage(verbName: String, url: String, responseBody: String): String = {
    s"$verbName of '$url' returned 412 (Precondition Failed). Response body: '$responseBody'"
  }

  protected def upstreamResponseMessage(verbName: String, url: String, status: Int, responseBody: String): String = {
    s"$verbName of '$url' returned $status. Response body: '$responseBody'"
  }

  def badRequestMessage(verbName: String, url: String, responseBody: String): String = {
    s"$verbName of '$url' returned 400 (Bad Request). Response body '$responseBody'"
  }

  protected def badGatewayMessage(verbName: String, url: String, status: Int, responseBody: String): String = {
    s"$verbName of '$url' returned status $status. Response body: '$responseBody'"
  }

  protected def badGatewayMessage(verbName: String, url: String, e: Exception): String = {
    s"$verbName of '$url' failed. Caused by: '${e.getMessage}'"
  }

  protected def gatewayTimeoutMessage(verbName: String, url: String, e: Exception): String = {
    s"$verbName of '$url' timed out with message '${e.getMessage}'"
  }

  protected def is2xx(status: Int) = status >= 200 && status < 300

  protected def is4xx(status: Int) = status >= 400 && status < 500

  protected def is5xx(status: Int) = status >= 500 && status < 600

  type ProcessingFunction = (Future[HttpResponse], String) => Future[HttpResponse]

  protected[http] def handleResponse(httpMethod: String, url: String)(response: HttpResponse)(implicit hc: HeaderCarrier): HttpResponse =
    response.status match {
    case status if is2xx(status) => response
    case 400 => throw new BadRequestException(badRequestMessage(httpMethod, url, response.body))
    case 404 => throw new NotFoundException(notFoundMessage(httpMethod, url, response.body))
    case status if is4xx(status) => throw new Upstream4xxResponse(upstreamResponseMessage(httpMethod, url, status, response.body), status, 500)
    case status if is5xx(status) => throw new Upstream5xxResponse(upstreamResponseMessage(httpMethod, url, response.status, response.body), status, 502)
    case status => throw new Exception(s"$httpMethod to $url failed with status $status. Response body: '${response.body}'")
  }

  protected[http] def mapErrors(httpMethod: String, url: String, f: Future[HttpResponse]): Future[HttpResponse] =
    f.recover {
      case e: TimeoutException => throw new GatewayTimeoutException(gatewayTimeoutMessage(httpMethod, url, e))
      case e: ConnectException => throw new BadGatewayException(badGatewayMessage(httpMethod, url, e))
  }
}
