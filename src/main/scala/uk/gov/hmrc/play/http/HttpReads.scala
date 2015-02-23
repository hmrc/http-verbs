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

  implicit val readRaw: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
    def read(method: String, url: String, response: HttpResponse) = handleResponse(method, url)(response)
  }

  def readJsonFromProperty[O](name: String)(implicit rds: json.Reads[O], mf: Manifest[O]) = new HttpReads[Seq[O]] {
    def read(method: String, url: String, response: HttpResponse) = response.status match {
      case 204 | 404 => Seq.empty
      case _ => readJson[Seq[O]](method, url, handleResponse(method, url)(response).json \ name)
    }
  }

  protected[http] def readJson[A](method: String, url: String, jsValue: JsValue)(implicit rds: json.Reads[A], mf: Manifest[A]) = {
    jsValue.validate[A].fold(
      errs => throw new JsValidationException(method, url, Json.stringify(jsValue), mf.runtimeClass, errs),
      valid => valid
    )
  }
}
