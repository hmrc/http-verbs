http-verbs
==========



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

## Adding to your build

In your SBT build add:

```scala
resolvers += Resolver.bintrayRepo("hmrc", "releases")

libraryDependencies += "uk.gov.hmrc" %% "http-verbs-play-xx" % "x.x.x"
```
Where `play-xx` is `play-25`, `play-26` or `play-27` depending on your version of Play.

## Usage

Examples can be found [here](https://github.com/hmrc/http-verbs/blob/master/http-verbs-common/src/test/scala/uk/gov/hmrc/examples/Examples.scala)

### Headers

#### Creating HeaderCarrier

The `HeaderCarrier` should be created with `HeaderCarrierConverter` when a request is available, this will ensure that the appropriate headers are forwarded to internal hosts.

E.g. for backends:

```scala
HeaderCarrierConverter.fromRequest(request)
```

and for frontends:

```scala
HeaderCarrierConverter.fromRequestAndSession(request, request.session)
```

If a frontend endpoint is servicing an API call, it should probably use `fromRequest` since `fromRequestAndSession` will only look for an Authorization token in the session, and ignore any provided as a request header.

For asynchronous calls, where no request is available, a new HeaderCarrier can be created with default params:

```scala
HeaderCarrier()
```


#### Propagation of headers

Internal hosts are identified with the configuration `internalServiceHostPatterns`.
The headers which are forwarded include all the headers modelled explicitly in the `HeaderCarrier`, plus any that are listed with the configuration `bootstrap.http.headersAllowlist`.
For external hosts, the headers should be provided explicitly to the VERB function (`GET`, `POST` etc).

When providing additional headers to http requests, if it corresponds to an explicit one on the HeaderCarrier, it is recommended to replace it, otherwise you will be sending it twice:
```scala
client.GET("https://externalhost/api")(hc.copy(authorisation = "Basic 1234"))
```

For all other headers, provide them to the VERB function:
```scala
client.GET("https://externalhost/api", headers = Seq["AdditionHeader" -> "AdditionalValue"])(hc)
```

## Test Helpers

The ResponseMatchers class provides some useful logic for testing http-related code.

In your SBT build add the following in your test dependencies:

```scala
libraryDependencies += "uk.gov.hmrc" %% "http-verbs-test-play-xx" % "x.x.x" % Test
```


## License ##

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
