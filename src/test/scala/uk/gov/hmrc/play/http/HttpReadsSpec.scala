package uk.gov.hmrc.play.http

import org.scalacheck.Gen
import org.scalatest.prop.{TableDrivenPropertyChecks, GeneratorDrivenPropertyChecks}
import org.scalatest.{TryValues, Matchers, WordSpec}

import scala.util.{Failure, Try}

class HttpReadsSpec extends WordSpec with Matchers with GeneratorDrivenPropertyChecks with TableDrivenPropertyChecks with TryValues {

  "HttpReads.readRaw" should {
    "return the response if the status code is between 200 and 299" in {
      forAll (Gen.choose(200, 299)) { statusCode: Int =>
        val expectedResponse = HttpResponse(statusCode)
        HttpReads.readRaw.read("m", "u", expectedResponse) should be (expectedResponse)
      }
    }
    "return the correct exception if the status code is non-200" in {
      Try(HttpReads.readRaw.read("m", "u", HttpResponse(400))).failure.exception should be (a [BadRequestException])
      Try(HttpReads.readRaw.read("m", "u", HttpResponse(404))).failure.exception should be (a [NotFoundException])

      forAll (Gen.choose(400, 599)) { statusCode =>
        whenever(statusCode != 400 && statusCode != 404) {
          val exception = Try(HttpReads.readRaw.read("m", "u", HttpResponse(statusCode))).failure.exception
          if (statusCode <= 499) exception should be (a[Upstream4xxResponse])
          else                   exception should be (a [Upstream5xxResponse])
        }
      }

      forAll (Gen.posNum[Int]) { statusCode =>
        whenever(statusCode < 200 || statusCode > 599) {
          Try(HttpReads.readRaw.read("m", "u", HttpResponse(statusCode))).failure.exception should be (a[Exception])
        }
      }
    }
  }
}
