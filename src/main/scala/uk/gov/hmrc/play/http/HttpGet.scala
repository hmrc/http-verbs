package uk.gov.hmrc.play.http

import uk.gov.hmrc.play.audit.http.{HeaderCarrier, HttpAuditing}
import uk.gov.hmrc.play.http.logging.{MdcLoggingExecutionContext, ConnectionTracing}

import scala.concurrent.Future
import play.api.libs.json
import play.api.libs.json.{Json, JsValue}
import MdcLoggingExecutionContext._
import play.api.http.HttpVerbs.{GET => GET_VERB}

trait HttpGet extends HttpVerb with ConnectionTracing with HttpAuditing {
  protected def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse]

  def GET_RawResponse(url:String, fn: HttpResponse => HttpResponse = identity, auditResponseBody: Boolean = true)(implicit hc: HeaderCarrier): Future[HttpResponse] = withTracing(GET_VERB, url) {
    val httpResponse = doGet(url)
    auditRequestWithResponseF(url, GET_VERB, None, httpResponse)
    mapErrors(GET_VERB, url, httpResponse).map(fn)
  }

  def GET[A](url: String)(implicit rds: HttpReads[A], hc: HeaderCarrier): Future[A] =
    GET_RawResponse(url).map(response => rds.read(GET_VERB, url, response))

  @deprecated("GET[Option[A]] and GET_Collection have been added for common use cases, and GET_RawResponse gives you access to the unprocessed HttpResponse", "10/10/14")
  def GET[A](url: String, responseHandler: ProcessingFunction)(implicit rds: json.Reads[A], mf: Manifest[A], hc: HeaderCarrier): Future[HttpResponse] =
    responseHandler(GET_RawResponse(url), url)

  /**
   * The method wraps the response in Option.
   * For HttpResponse with status 404 or 202, instead of throwing an Exception, None will be returned.
   */
  @deprecated("use GET[Option[A]] instead", "23/2/2015")
  def GET_Optional[A](url: String)(implicit rds: json.Reads[A], mfst: Manifest[A], hc: HeaderCarrier): Future[Option[A]] =
    GET[Option[A]](url)

  /**
   * The method extracts the requested collection (array) from JSON response.
   * arrayFieldName indicates the name of the array field available in the JSON response.
   * For HttpResponse with status 404 or 202, instead of throwing an Exception, empty Seq is returned.
   */
  def GET_Collection[A](url: String, arrayFieldName: String)(implicit rds: json.Reads[A], mfst: Manifest[A], hc: HeaderCarrier) : Future[Seq[A]] =
    GETm[A, Seq](url, Seq.empty, response => readJson[Seq[A]](url, response.json \ arrayFieldName))

  protected[http] def GETm[A, M[_]](url: String, empty: => M[A], extractor: HttpResponse => M[A])(implicit hc: HeaderCarrier) : Future[M[A]] =
    GET_RawResponse(url).map { response =>
      response.status match {
        case 204 | 404 => empty
        case _ =>
          extractor(handleResponse(GET_VERB, url)(response))
      }
  }

  @deprecated("moved to HttpReads", "23/2/2015")
  def readJson[A](url: String, jsValue: JsValue)(implicit rds: json.Reads[A], mf: Manifest[A], hc: HeaderCarrier) =
    HttpReads.readJson(GET_VERB, url, jsValue)
}