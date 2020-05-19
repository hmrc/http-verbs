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

The default implicits for `HttpRead` have been deprecated. There are new implicits, which need to be pulled in explicitly with
```scala
import uk.gov.hmrc.http.HttpReads.Implicits._
```
The behaviour of the implicits is not quite the same as the deprecated ones:
* You will have to explicitly state the type of the reponse - it will not resolve to `HttpResponse` if none is specified.
* The default `HttpRead[HttpResponse]` will no longer throw an exception if there is a non-2xx status code. Since the HttpResponse already encodes errors, it expects you will handle this yourself. You may get the previous behaviour with
```scala
implicit val legacyRawReads = HttpReads.throwOnFailure(HttpReads.readEither)
```
* There is an `HttpReads[Either[UpstreamErrorResponse, A]]` defined which will return all non-2xx reponse codes as an `UpstreamErrorResponse`
* There is an `HttpReads[Either[UpstreamErrorResponse, JsResult[A]]]` defined for reading Json responses, which additionally will return all Json errors as JsError.
* For previous behaviour, use `HttpReads[A]`, which will throw exceptions for all errors.



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
