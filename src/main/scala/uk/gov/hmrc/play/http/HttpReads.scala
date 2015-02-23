package uk.gov.hmrc.play.http

import play.api.libs.json
import play.api.libs.json.{JsValue, Json}
import play.twirl.api.Html
import uk.gov.hmrc.play.audit.http.HeaderCarrier

trait HttpReads[O] {
  def read(method: String, url: String, response: HttpResponse): O
}

object HttpReads extends HttpErrorFunctions {
  implicit def readOptionOf[P](implicit rds: HttpReads[P]): HttpReads[Option[P]] = new HttpReads[Option[P]] {
    def read(method: String, url: String, response: HttpResponse) = response.status match {
      case 204 | 404 => None
      case _ => Some(rds.read(method, url, response))
    }
  }

  implicit val readToHtml: HttpReads[Html] = new HttpReads[Html] {
    def read(method: String, url: String, response: HttpResponse) = Html(handleResponse(method, url)(response).body)
  }

  implicit def readFromJson[O](implicit rds: json.Reads[O], mf: Manifest[O]): HttpReads[O] = new HttpReads[O] {
    def read(method: String, url: String, response: HttpResponse) = readJson(method, url, handleResponse(method, url)(response).json)
  }

  protected[http] def readJson[A](method: String, url: String, jsValue: JsValue)(implicit rds: json.Reads[A], mf: Manifest[A]) = {
    jsValue.validate[A].fold(
      errs => throw new JsValidationException(method, url, Json.stringify(jsValue), mf.runtimeClass, errs),
      valid => valid
    )
  }
}

protected[http] trait HttpErrorFunctions {
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

  protected[http] def handleResponse(httpMethod: String, url: String)(response: HttpResponse): HttpResponse =
    response.status match {
      case status if is2xx(status) => response
      case 400 => throw new BadRequestException(badRequestMessage(httpMethod, url, response.body))
      case 404 => throw new NotFoundException(notFoundMessage(httpMethod, url, response.body))
      case status if is4xx(status) => throw new Upstream4xxResponse(upstreamResponseMessage(httpMethod, url, status, response.body), status, 500, response.allHeaders)
      case status if is5xx(status) => throw new Upstream5xxResponse(upstreamResponseMessage(httpMethod, url, response.status, response.body), status, 502)
      case status => throw new Exception(s"$httpMethod to $url failed with status $status. Response body: '${response.body}'")
    }
}
