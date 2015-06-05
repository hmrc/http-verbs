http-verbs
==========

[![Join the chat at https://gitter.im/hmrc/http-verbs](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/hmrc/http-verbs?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)  [![Build Status](https://travis-ci.org/hmrc/http-verbs.svg)](https://travis-ci.org/hmrc/http-verbs) [ ![Download](https://api.bintray.com/packages/hmrc/releases/http-verbs/images/download.svg) ](https://bintray.com/hmrc/releases/http-verbs/_latestVersion)

http-verbs is a Scala library providing an interface to make asynchronous HTTP calls.  The underlying implementation uses [Play WS](https://www.playframework.com/documentation/latest/ScalaWS).

It encapsulates some common concerns for calling other HTTP services on the HMRC Tax Platform, including:

* Auditing
* Logging
* Propagation of common headers
* Response handling, converting failure status codes into a consistent set of exceptions - allows failures to be automatically propagated to the caller
* Request & Response de-serialization

## Adding to your build

In your SBT build add:

```scala
resolvers += Resolver.bintrayRepo("hmrc", "releases")

libraryDependencies += "uk.gov.hmrc" %% "http-verbs" % "x.x.x"
```

## Usage

_All examples show below are available in [Examples.scala](src/test/scala/uk/gov/hmrc/play/Examples.scala)_

#### Adding to your app

Each verb is available as both an agnostic `Http___` trait and a play-specific `WS___` trait. They can be used as mixins:

```scala
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.ws._
import audit.http.config._
import audit.http.connector._

trait ConnectorWithMixins extends HttpGet with HttpPost {
  
}
object ConnectorWithMixins extends ConnectorWithMixins with WSGet with WSPost {
  val appName = "my-app-name"
  val auditConnector = AuditConnector(LoadAuditingConfig(key = "auditing"))
}
```

or as `val`s:

```scala
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.ws._
import audit.http.config._
import audit.http.connector._

trait ConnectorWithHttpValues {
  val http: HttpGet with HttpPost
}
object ConnectorWithHttpValues extends ConnectorWithHttpValues {
  val http = new WSGet with WSPost {
    val appName = "my-app-name"
    val auditConnector = AuditConnector(LoadAuditingConfig(key = "auditing"))
  }
}
```

In both cases, you'll need to supply an [auditing configuration](#configuration). 

#### Making HTTP Calls

Each verb supplies a single matching method, which takes a URL, and a request body if appropriate:

```scala
implicit val hc = HeaderCarrier()

http.GET("http://gov.uk/hmrc")
http.DELETE("http://gov.uk/hmrc")
http.POST("http://gov.uk/hmrc", body = "hi there")
http.PUT("http://gov.uk/hmrc", body = "hi there")
```

All calls require an implicit `HeaderCarrier`. 

Headers from the carrier are added to every request. Depending on how you scope your implicits, this could allow:

* Common headers to be configured for all requests
* Headers from an inbound call to be propagated to an outbound one (we do this by implicitly converting Play's `Request` into a `HeaderCarrier`)

## Response Handling

All verbs return `Future`s. By default, a `HttpResponse` is returned for successful responses, which gives access to the status code, raw body and headers:

```scala
val r1 = http.GET("http://gov.uk/hmrc") // Returns an HttpResponse
val r2 = http.GET[HttpResponse]("http://gov.uk/hmrc") // Can specify this explicitly
r1.map { r =>
  r.status
  r.body
  r.allHeaders
}
```

### Errors

To make microservice development simple, responses with non-200 status codes will cause a failed `Future` to be returned with a typed exception. This allows failures to be propagated back naturally. 

Status Code   | Exception
------------- | -------------
400           | `BadRequestException`
404           | `NotFoundException`
4xx           | `Upstream4xxResponse`
5xx           | `Upstream5xxResponse`

_For all possible exception types, see the [hmrc/http-exceptions](https://github.com/hmrc/http-exceptions) project_

If some failure status codes are expected in normal flow, `recover` can be used for simple cases: 

```scala
httpGet.GET("url") recover {
  case nf: NotFoundException => ...
  case se: Upstream5xxResponse => ...
}
```

or you can use an [`Option[...]` return type](#potentially-empty-responses).

### Response Types

http-verbs can automatically map responses into richer types.

##### JSON responses
If you have an implicit `play.api.libs.json.Reads[A]` for your type in scope, just specify that type and it will be automatically deserialised.

```scala
import play.api.libs.json._
case class MyCaseClass(a: String, b: Int)
implicit val f = Json.reads[MyCaseClass]
http.GET[MyCaseClass]("http://gov.uk/hmrc") // Returns an MyCaseClass de-serialised from JSON
```

##### HTML responses
If you wish to use HTML responses, Play's `Html` type can be used:

```scala                                      
import play.twirl.api.Html
http.GET[Html]("http://gov.uk/hmrc") // Returns a Play Html type
```

<!--- TODO: Talk about special methods POSTString, POSTForm etc. --->

## Extension & Customisation
Response handling is implemented via the `HttpReads[A]` typeclass, which is responsible for converting the raw response into either an exception or the specified type. Default implementations of `HttpReads[A]` have are provided in its companion object to cover common use cases. 

If you have more complex requirements, then you can create your own with a built-in DSL. To see all of the available methods in the DSL, see the types `uk.gov.hmrc.play.http.reads.__HttpReads`. Each can be used as either mixins or imports. 

#### Reading from an array at a named property

As an example, say we wanted to just deserialize an array of JSON objects from the `"items"` property. We also expected that we might get 204 or 404 responses, and wanted those to be converted into `Seq.empty`. We could write:

```scala
object ExampleOfSeqReads extends JsonHttpReads with ErrorHttpReads {
  implicit val readSeqMyCaseClass: HttpReads[Seq[MyCaseClass]] =
    emptyOn(204) or
    emptyOn(404) or
    convert400ToBadRequest or
    convert4xxToUpstream4xxResponse or
    convert5xxToUpstream5xxResponse or
    convertLessThan200GreaterThan599ToException or
    atPath("items")(jsonBodyDeserialisedTo[Seq[MyCaseClass]])

  http.GET[Seq[MyCaseClass]]("http://gov.uk/hmrc")
}
```

#### Potentially empty responses

If you expect to sometimes receive empty responses, then http-verbs supports an `Option[...]` on your return type. You'll need to when `None` should be returned.

For instance, if you wish to have `Option[Html]`, returning `None` on `204`, then you could write:

```scala
object ExampleWithOptionReads extends OptionHttpReads {
  implicit val myReads: HttpReads[Option[Html]] = noneOn(status = 204) or some[Html]
  http.GET[Option[Html]]("http://gov.uk/hmrc") // Returns a None, or a Play Html type
}
```

Or, let's say that you are deserializing a JSON response, and you also expect `404` codes, which should be converted to `None`, then try:

```scala
{
  import OptionHttpReads._
  import JsonHttpReads._

  implicit val readOptionalMyCaseClass: HttpReads[Option[MyCaseClass]] =
    noneOn(204) or noneOn(404) or some(jsonBodyDeserialisedTo[MyCaseClass])

  http.GET[Option[MyCaseClass]]("http://gov.uk/hmrc") // Returns None, or Some[MyCaseClass] de-serialised from JSON
}
```

## Configuration

Request auditing is provided for all HTTP requests that are made using this library. Each request/response pair results in an audit message being created and sent to an external auditing service for processing.  To configure this service, your Play configuration file needs to include:

```javascript
auditing {
  enabled = true
  traceRequests = true
  consumer {
    baseUri {
      host = ...
      port = ...
    }
  }
}
```

```HttpAuditing``` provides ```def auditDisabledForPattern = ("""http://.*\.service""").r``` which client applications may chose to override when mixing in ```HttpAuditing```.

_NOTE:_ This configuration used to be provided by reading Play configuration property ```<env>.http-client.audit.disabled-for``` which is now obsolete.

## License ##
 
This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").


