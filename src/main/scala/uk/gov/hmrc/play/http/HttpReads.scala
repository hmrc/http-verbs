package uk.gov.hmrc.play.http

import play.api.libs.json
import play.api.libs.json.Json
import play.twirl.api.Html

trait HttpReads[O] {
  def read(method: String, url: String, response: HttpResponse): O
}

object HttpReads {
  implicit val readToHtml: HttpReads[Html] = new HttpReads[Html] {
    def read(method: String, url: String, response: HttpResponse) = Html(response.body)
  }

  implicit def readFromJson[O](implicit rds: json.Reads[O], mf: Manifest[O]): HttpReads[O] =
    new HttpReads[O] {
      def read(method: String, url: String, response: HttpResponse) =
        response.json.validate[O].fold(
          errs => throw new JsValidationException(method, url, Json.stringify(response.json), mf.runtimeClass, errs),
          valid => valid
        )
    }
}
