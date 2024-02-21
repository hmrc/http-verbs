http-verbs
==========


![](https://img.shields.io/github/v/release/hmrc/http-verbs)

http-verbs is a Scala library providing an interface to make asynchronous HTTP calls.

It encapsulates some common concerns for calling other HTTP services on the HMRC Tax Platform, including:

* Http Transport
* Core Http function interfaces
* Logging
* Propagation of common headers
* Executing hooks, for example Auditing
* Request & Response de-serializations
* Response handling, converting failure status codes into a consistent set of exceptions - allows failures to be automatically propagated to the caller

## Migration

See [CHANGELOG](https://github.com/hmrc/http-verbs/blob/master/CHANGELOG.md) for changes and migrations.

## Adding to your build

In your SBT build add:

```scala
resolvers += MavenRepository("HMRC-open-artefacts-maven2", "https://open.artefacts.tax.service.gov.uk/maven2")

libraryDependencies += "uk.gov.hmrc" %% "http-verbs-play-xx" % "x.x.x"
```
Where `play-xx` is your version of Play (e.g. `play-28`).

## Usage

There are two HttpClients available.

### uk.gov.hmrc.http.HttpClient

Examples can be found [here](https://github.com/hmrc/http-verbs/blob/master/http-verbs-test-common/src/test/scala/uk/gov/hmrc/http/examples/Examples.scala)

URLs can be supplied as either `java.net.URL` or `String`. We recommend supplying `java.net.URL` and using the provided [URL interpolator](#url-interpolator) for correct escaping of query and path parameters.


### uk.gov.hmrc.http.client.HttpClientV2

This client follows the same patterns as `HttpClient` - that is, it also requires a `HeaderCarrier` to represent the context of the caller, and an `HttpReads` to process the http response.

In addition, it:
- Supports streaming
- Exposes the underlying `play.api.libs.ws.WSRequest` with `transform`, making it easier to customise the request.
- Only accepts the URL as `java.net.URL`; you can make use of the provided [URL interpolator](#url-interpolator).

Examples can be found in [here](/http-verbs-common/src/test/scala/uk/gov/hmrc/http/client/HttpClientV2Spec.scala)

To migrate:

```scala
httpClient.GET[ResponseType](url)
```

becomes

```scala
httpClientV2.get(url"$url").execute[ResponseType]
```

and

```scala
httpClient.POST[ResponseType](url, payload, headers)
```

becomes

```scala
httpClientV2.post(url"$url").withBody(Json.toJson(payload)).addHeaders(headers).execute[ResponseType]
```


#### Header manipulation

With `HttpClient`, replacing a header can require providing a customised client implementation (e.g. to replace the user-agent header), or updating the `HeaderCarrier` (e.g. to replace the authorisation header). This can now all be done with the `setHeader` on `HttpClientV2` per call. e.g.

```scala
httpClientV2.get(url"$url")
  .setHeader("User-Agent" -> userAgent)
  .setHeader("Authorization" -> authorization)
  .execute[ResponseType]
```

As well as replacing existing header values, `setHeader` can be used to add new headers too, and in most cases should be used in preference to `addHeaders` where the values are merged with any existing ones (e.g. from HeaderCarrier).

Be aware that `"Content-Type"` cannot be changed once set with `WSRequest` so if you want a different one to the one defined by the implicit `BodyWriter`, you will need to set it before providing the body. e.g.
```scala
httpClientV2.post(url"$url")
  .setHeader("Content-Type" -> "application/xml")
  .withBody(<foo>bar</foo>)
```

#### Using proxy

With `HttpClient`, to use a proxy requires creating a new instance of HttpClient to mix in `WSProxy` and configure. With `HttpClientV2` this can be done with the same client, calling `withProxy` per call. e.g.

```scala
httpClientV2.get(url"$url").withProxy.execute[ResponseType]
```

* It uses `WSProxyConfiguration.buildWsProxyServer` which is enabled with `http-verbs.proxy.enabled` in configuration. It is disabled by default, which is appropriate for local development and tests, but will need enabling when deployed (if not already enabled by environmental configuration).

#### Streaming

Streaming is supported with `HttpClientV2`, and will be audited in the same way as `HttpClient`. Note that payloads will be truncated in audit logs if they exceed the max supported (as configured by `http-verbs.auditing.maxBodyLength`).

Streamed requests can simply be passed to `withBody`:

```scala
val reqStream: Source[ByteString, _] = ???
httpClientV2.post(url"$url").withBody(reqStream).execute[ResponseType]
```

For streamed responses, use `stream` rather than `execute`:

```scala
httpClientV2.get(url"$url").stream[Source[ByteString, _]]
```

#### Auditing

HttpClientV2 truncates payloads for audit logs if they exceed the max supported (as configured by `http-verbs.auditing.maxBodyLength`).

This means audits that were rejected for being too large with HttpClient will probably be accepted with HttpclientV2.

:warning: Please check any potential impact this may have on auditing performance.


### URL interpolator

A [URL interpolator](https://sttp.softwaremill.com/en/latest/model/uri.html) has been provided to help with escaping query and parameters correctly.

```scala
import uk.gov.hmrc.http.StringContextOps

url"http://localhost:8080/users/${user.id}?email=${user.email}"
```


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

For external hosts, headers should be provided explicitly to the VERB function (`GET`, `POST` etc). Only the User-Agent header from the HeaderCarrier is forwarded.

```scala
client.GET(url"https://externalhost/api", headers = Seq("Authorization" -> "Bearer token"))(hc) //explicit Authorization header for external request
```

Internal hosts are identified with the configuration [internalServiceHostPatterns](/http-verbs-common/src/main/resources/reference.conf) The headers which are forwarded, to _internal hosts_, include all the headers modelled explicitly in the `HeaderCarrier`, plus any that are listed with the configuration `bootstrap.http.headersAllowlist`.
For example, if you want to pass headers to stubs, you can use the following override for your service: `internalServiceHostPatterns= "^.*(stubs?).*(\.mdtp)$"`

When providing additional headers to http requests, if it corresponds to an explicit one on the HeaderCarrier, it is recommended to replace it, otherwise you will be sending it twice:
```scala
client.GET("https://internalhost/api")(hc.copy(authorization = Some(Authorization("Basic 1234"))))
```

For all other headers, provide them to the VERB function:
```scala
client.GET(url = url"https://internalhost/api", headers = Seq("AdditionHeader" -> "AdditionalValue"))(hc)
```

## Testing

In your SBT build add the following in your test dependencies:

```scala
libraryDependencies += "uk.gov.hmrc" %% "http-verbs-test-play-xx" % "x.x.x" % Test
```

We recommend that [Wiremock](http://wiremock.org/) is used for testing http-verbs code, with extensive assertions on the URL, Headers, and Body fields for both requests and responses. This will test most things, doesn't involve "mocking what you don't own", and ensures that changes to this library will be caught.

The `WireMockSupport` trait helps set up WireMock for your tests. It provides `wireMockHost`, `wireMockPort` and `wireMockUrl` which can be used to configure your client appropriately.

e.g. with an application:
```scala
class MyConnectorSpec extends WireMockSupport with GuiceOneAppPerSuite {
  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "connector.host" -> wireMockHost,
        "connector.port" -> wireMockPort
      ).build()

  private val connector = app.injector.instanceOf[MyConnector]
}
```

The `HttpClientSupport` trait can provide an instance of `HttpClient` as an alternative to instanciating the application:
```scala
class MyConnectorSpec extends WireMockSupport with HttpClientSupport {
  private val connector = new MyConnector(
      httpClient,
      Configuration("connector.url" -> wireMockUrl)
  )
}
```

Similarly `HttpClientV2Support` can be used to provide an instance of `HttpClientV2`.

The `ExternalWireMockSupport` trait is an alternative to `WireMockSupport` which uses `127.0.0.1` instead of `localhost` for the hostname which is treated as an external host for header forwarding rules. This should be used for tests of connectors which call endpoints external to the platform. The variable `externalWireMockHost` (or `externalWireMockUrl`) should be used to provide the hostname in configuration.

Both `WireMockSupport` and `ExternalWireMockSupport` can be used together for integration tests if required.


The `ResponseMatchers` trait provides some useful helpers for testing responses.



## License ##

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
