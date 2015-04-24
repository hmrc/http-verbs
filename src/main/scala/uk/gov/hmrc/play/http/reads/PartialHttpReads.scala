package uk.gov.hmrc.play.http.reads

import uk.gov.hmrc.play.http.HttpResponse

object PartialHttpReads {
  def apply[O](readF: (String, String, HttpResponse) => Option[O]): PartialHttpReads[O] = new PartialHttpReads[O] {
    def read(method: String, url: String, response: HttpResponse) = readF(method, url, response)
  }

  def byStatus[O](statusF: PartialFunction[Int, O]) = PartialHttpReads[O] { (method, url, response) =>
    statusF.lift(response.status)
  }
  def onStatus[O](status: Int): ByStatusBuilder[O] = new ByStatusBuilder(statusMatcher = _ == status)
  def onStatus[O](statusRange: Range): ByStatusBuilder[O] = new ByStatusBuilder[O](statusRange contains _)
  def onStatus[O](statusMatcher: Int => Boolean): ByStatusBuilder[O] = new ByStatusBuilder[O](statusMatcher)

  class ByStatusBuilder[O](statusMatcher: Int => Boolean) {
    def apply(possibleRds: (String, String, HttpResponse) => Option[O]): PartialHttpReads[O] = apply(PartialHttpReads(possibleRds))

    def apply(possibleRds: PartialHttpReads[O]): PartialHttpReads[O] = PartialHttpReads[O] { (method, url, response) =>
      if (statusMatcher(response.status)) possibleRds.read(method, url, response) else None
    }

    def apply(possibleRds: HttpReads[O]): PartialHttpReads[O] = PartialHttpReads[O] { (method, url, response) =>
      if (statusMatcher(response.status)) Some(possibleRds.read(method, url, response)) else None
    }
  }
}

trait PartialHttpReads[+O] {
  def read(method: String, url: String, response: HttpResponse): Option[O]

  def or[P >: O](rds: HttpReads[P]) = HttpReads[P] { (method, url, response) =>
    PartialHttpReads.this.read(method, url, response) getOrElse rds.read(method, url, response)
  }

  def or[P >: O](rds: PartialHttpReads[P]) = PartialHttpReads[P] { (method, url, response) =>
    PartialHttpReads.this.read(method, url, response) orElse rds.read(method, url, response)
  }
}