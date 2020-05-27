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

### Version 11.0.0

#### HttpReads

The default implicits for `HttpReads` have been deprecated. There are new implicits, which need to be pulled in explicitly with
```scala
import uk.gov.hmrc.http.HttpReads.Implicits._
```
The behaviour of the implicits is not quite the same as the deprecated ones:
* You will have to explicitly state the type of the reponse - it will not resolve to `HttpResponse` if none is specified.
* The default `HttpRead[HttpResponse]` will no longer throw an exception if there is a non-2xx status code. Since the HttpResponse already encodes errors, it expects you will handle this yourself. To get the behaviour similar to previous (see Exceptions for differences), use:
```scala
implicit val legacyRawReads = HttpReads.throwOnFailure(HttpReads.readEither)
```
* There is an `HttpReads[Either[UpstreamErrorResponse, A]]` defined which will return all non-2xx reponse codes as an `UpstreamErrorResponse`
* There is an `HttpReads[Either[UpstreamErrorResponse, JsResult[A]]]` defined for reading Json responses, which additionally will return all Json errors as JsError.
* For previous behaviour, use `HttpReads[A]`, which will throw exceptions for all errors.

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

libraryDependencies += "uk.gov.hmrc" %% "http-verbs" % "x.x.x"
```

## Usage

All examples are available here:[hmrc/http-verbs-example](https://github.com/hmrc/http-verbs-example)

## License ##

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
