http-verbs
==========

[![Build Status](https://travis-ci.org/hmrc/http-verbs.svg)](https://travis-ci.org/hmrc/http-verbs) [ ![Download](https://api.bintray.com/packages/hmrc/releases/http-verbs/images/download.svg) ](https://bintray.com/hmrc/releases/http-verbs/_latestVersion)

http-verbs is a Scala library providing an interface to make asynchronous HTTP calls.

It encapsulates some common concerns for calling other HTTP services on the HMRC Tax Platform, including:

* Logging
* Header Carrier
* Http Transport
* Core Http function interfaces
* executing hooks
* mapping errors
* Auditing
* Logging
* Propagation of common headers
* Response handling, converting failure status codes into a consistent set of exceptions - allows failures to be * automatically propagated to the caller
* Request & Response de-serializations

## Migration

### Version 12.0.0

#### HeaderCarrier
The fields `token` and `userId` are no longer available in the session and so have been removed from HeaderCarrier

### Version 11.0.0

#### HttpReads

The default implicits for `HttpReads` have been deprecated. There are new implicits, which need to be pulled in explicitly with
```scala
import uk.gov.hmrc.http.HttpReads.Implicits._
```
The behaviour of the predefined implicits are not quite the same as the deprecated ones, and you are encouraged to define your own HttpReads if none are apropriate. The differences are:
* You will have to explicitly state the type of the response - it will not resolve to `HttpResponse` if none is specified. (i.e. `GET(url)` will now be `GET[HttpResponse](url)`). It is deemed better to be explicit since the type will dictate how errors are handled.
* The default `HttpRead[HttpResponse]` will no longer throw an exception if there is a non-2xx status code. Since the HttpResponse already encodes errors, it expects you will handle this yourself. To get the behaviour similar to previous (see Exceptions for differences), use:
```scala
implicit val legacyRawReads = HttpReads.throwOnFailure(HttpReads.readEither)
```
* `HttpReads[Option[A]]` only returns None for 404, and will try to parse other responses. Previously, 204 was also treated as None, consider representing this with Unit instead.
* The `HttpReads[A]` where `A` is defined by a `play.api.libs.json.Reads[A]` works in the same way as before, i.e. throws exceptions for non-2xx response codes (`UpstreamErrorResponse`), and json parsing errors (`JsValidationException`). Since the http-verbs API operates within `Future`, this is probably the simplest response type, since Future offers recovery, and if not handled, will propagate to the caller. However the HttpReads can be combined with other HttpReads to return the errors in different ways. E.g.
  * `HttpReads[Either[UpstreamErrorResponse, JsResult[A]]]`
  * `HttpReads[Try[JsResult[A]]]`,
These error encoded types are available for any response type not just json

#### HttpResponse

The trait for HttpResponse will be replaced with a case class. You should only create instances with the `HttpResponse.apply` function, and not extend it.
If your clients previously relied on an instance of `WSHttpResponse` being returned, they will have to change to use the `HttpResponse` abstraction.

#### Exceptions

The new `HttpReads` instances only return `UpstreamErrorResponse` for failures returned from upstream services. They will no longer return `HttpException` which will be reserved for problems in making the request (e.g. `GatewayTimeoutException` for timeout exceptions and `BadGatewayException` for connect exceptions), and originate in the service itself.
The trait `UpstreamErrorResponse` will be replaced with a case class, and the subclasses `Upstream4xxResponse` and `Upstream5xxResponse` have been deprecated in preparation. If you need to pattern match on these types, use `UpstreamErrorResponse.Upstream4xxResponse` and `UpstreamErrorResponse.Upstream5xxResponse` instead.


## Adding to your build

In your SBT build add:

```scala
resolvers += Resolver.bintrayRepo("hmrc", "releases")

libraryDependencies += "uk.gov.hmrc" %% "http-verbs-play-xx" % "x.x.x"
```
Where `play-xx` is `play-25`, `play-26` or `play-27` depending on your version of Play.

## Usage

Play 2.5 examples can be found [here](https://github.com/hmrc/http-verbs/blob/master/http-verbs-play-25/src/test/scala/uk/gov/hmrc/examples/Examples.scala)

Play 2.6 and 2.7 examples can be found [here](https://github.com/hmrc/http-verbs/blob/master/http-verbs-play-26/src/test/scala/uk/gov/hmrc/examples/Examples.scala)

## Test Helpers

The ResponseMatchers class provides some useful logic for testing http-related code.

In your SBT build add the following in your test dependencies:

```scala
libraryDependencies += "uk.gov.hmrc" %% "http-verbs-test-play-xx" % "x.x.x" % Test
```


## License ##

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
