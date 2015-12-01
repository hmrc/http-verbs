http-verbs
==========

[![Join the chat at https://gitter.im/hmrc/http-verbs](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/hmrc/http-verbs?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)  [![Build Status](https://travis-ci.org/hmrc/http-verbs.svg)](https://travis-ci.org/hmrc/http-verbs) [ ![Download](https://api.bintray.com/packages/hmrc/releases/http-verbs/images/download.svg) ](https://bintray.com/hmrc/releases/http-verbs/_latestVersion)

http-verbs is a Scala library providing an interface to make asynchronous HTTP calls.  The underlying implementation uses [Play WS](https://www.playframework.com/documentation/latest/ScalaWS).

It encapsulates some common concerns for calling other HTTP services on the HMRC Tax Platform, including:

* ~~Auditing~~
* Logging
* Propagation of common headers
* Response handling, converting failure status codes into a consistent set of exceptions - allows failures to be automatically propagated to the caller
* Request & Response de-serialization

**Auditing is no longer part of http-verbs, please see docs for [play-auditing](http://github.com/hmrc/play-auditing) for further info.**

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

trait ConnectorWithMixins extends HttpGet with HttpPost {
  
}

object ConnectorWithMixins extends ConnectorWithMixins with WSGet with WSPost {
  val appName = "my-app-name"
}
```

or as `val`s:

```scala
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.ws._

trait ConnectorWithHttpValues {
  val http: HttpGet with HttpPost
}

object ConnectorWithHttpValues extends ConnectorWithHttpValues {
  val http = new WSGet with WSPost {
    val appName = "my-app-name"
  }
}
```

#### Making HTTP Requests

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

#### Conditional HTTP Requests

Each verb permits an optional Precondition parameter, for which either the ifMatch or ifNoneMatch field should be
populated to make a conditional request.

* Use ifMatch preconditions typically when you need to PUT, POST or DELETE some data, but only to succeed when 
  no conflict is detected.
* Use ifNoneMatch to GET or HEAD some resource that will not need to return any content when nothing has changed
  since an earlier request.

You will have received the 'etag' values from earlier requests via the ETag header. They should be returned verbatim
(note that they are normally quoted: see [Precondition header fields](https://tools.ietf.org/html/rfc7232#section-3)).

```scala
implicit val hc = HeaderCarrier()

http.GET("http://gov.uk/hmrc", Precondition(ifNoneMatch = Seq(etag))
http.POST("http://gov.uk/hmrc", body = "hi there", Precondition(ifMatch = Seq(etag1, etag2))
```

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

For conditional requests and anything that may return 204, make sure the type is an Option:

```scala
val r3 = http.GET[Option[FooValue]]("http://gov.uk/hmrc", Precondition(ifMatch = Seq(etag1, etag2))
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

If some failure status codes are expected in normal flow, `recover` can be used: 

```scala
httpGet.GET("url") recover {
  case nf: NotFoundException => ...
  case se: Upstream5xxResponse => ...
}
```

or if you expect responses which indicate no content, you can use an [`Option[...]` return type](#potentially-empty-responses).

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

#### Potentially empty responses
If you expect to receive a `204` or `404` response in some circumstances, or you make a conditional GET request, then 
you can add `Option[...]` to your return type:

```scala
http.GET[Option[MyCaseClass]]("http://gov.uk/hmrc") // Returns None, or Some[MyCaseClass] de-serialised from JSON
http.GET[Option[Html]]("http://gov.uk/hmrc") // Returns a None, or a Play Html type
```

<!--- TODO: How to influence which implicit is used - mixin vs import vs directly by type --->

<!--- TODO: Talk about special methods POSTString, POSTForm etc. --->

## Extension & Customisation
Response handling is implemented via the `HttpReads[A]` typeclass, which is responsible for converting the raw response into either an exception or the specified type. Default implementations of `HttpReads[A]` have been provided in its companion object to cover common use cases, but clients may provide their own implementations if required. 

#### Hooks

You can now set up http-verbs to run a series of callbacks when the future representing an http request completes - these are known as hooks. Hooks are used by [play-auditing](http://github.com/hmrc/play-auditing) in order to wire up implicit auditing.

A hook looks like this:

```scala
object MyHook extends HttpHook {
  override def apply(url: String, verb: String, body: Option[_], responseF: Future[HttpResponse])(implicit hc: HeaderCarrier): Unit = {
    responseF.map {
      response => // do something on success
    }.recover {
      case e: Throwable => // do something on exception
    }
  }
}
```

And is registered with http-verbs like this:

```scala
object WSHttp extends WSGet with WSPut with WSPost with WSDelete with WSPatch {
  override val hooks = Seq(MyHook, AnotherHook)
  ...
  ...
}
```

## License ##
 
This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
