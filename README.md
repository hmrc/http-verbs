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

See [CHANGELOG](CHANGELOG.md) for changes and migrations.

## Adding to your build

In your SBT build add:

```scala
resolvers += MavenRepository("HMRC-open-artefacts-maven2", "https://open.artefacts.tax.service.gov.uk/maven2")

libraryDependencies += "uk.gov.hmrc" %% "http-verbs-play-xx" % "x.x.x"
```
Where `play-xx` is your version of Play (e.g. `play-30`).

## Usage

There are two HttpClients available, but `HttpClient` and related API have been deprecated. Please use `uk.gov.hmrc.http.client.HttpClientV2` instead.

### uk.gov.hmrc.http.client.HttpClientV2

This client has more features than the original `HttpClient` and is simpler to use.

It requires a `HeaderCarrier` to represent the context of the caller, and an `HttpReads` to process the http response.

It also :
- Supports streaming
- Exposes the underlying `play.api.libs.ws.WSRequest` with `transform`, making it easier to customise the request.
- Only accepts the URL as `java.net.URL`; you can make use of the provided [URL interpolator](#url-interpolator).

Examples can be found [here](http-verbs-test-play-30/src/test/scala/uk/gov/hmrc/http/examples/Examples.scala) and [here](http-verbs-play-30/src/test/scala/uk/gov/hmrc/http/client/HttpClientV2Spec.scala)


### uk.gov.hmrc.http.HttpClient

This client has been deprecated. You can migrate as follows:

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
httpClientV2.post(url"$url").withBody(Json.toJson(payload)).setHeader(headers).execute[ResponseType]
```

If you were previously creating multiple HttpClients to configure proxies or change the user-agent, this will no-longer be necessary since these can all be controlled with the HttpClientV2 API per call.

#### Header manipulation

The `HeaderCarrier` should largely be treated as an immutable representation of the caller. If you need to manipulate the headers being sent in requests, you can do this with the `HttpClientV2` API.

For example to override the default User-Agent or the Authorization header defined in the HeaderCarrier, you can use `setHeader` which will replace any existing ones.

```scala
httpClientV2.get(url"$url")
  .setHeader("User-Agent" -> userAgent)
  .setHeader("Authorization" -> authorization)
  .execute[ResponseType]
```

If you want to append to default headers, then you can access `addHttpHeaders` on the underlying `WSClient` with `transform`. e.g.
```scala
httpClientV2.get(url"$url")
  .transform(_.addHttpHeaders("User-Agent" -> userAgent))
  .execute[ResponseType]
```

Be aware that `"Content-Type"` cannot be changed once set with `WSRequest` so if you want a different one to that defined by the implicit `BodyWriter`, you will need to set it before providing the body. e.g.
```scala
httpClientV2.post(url"$url")
  .setHeader("Content-Type" -> "application/xml")
  .withBody(<foo>bar</foo>)
```

#### Using proxy

With `HttpClient`, to use a proxy required creating a new instance of HttpClient to mix in `WSProxy` and configure. With `HttpClientV2` this can be done with the same client, calling `withProxy` per call. e.g.

```scala
httpClientV2.get(url"$url").withProxy.execute[ResponseType]
```

* It uses `WSProxyConfiguration.buildWsProxyServer` which is enabled with `http-verbs.proxy.enabled` in configuration. It is disabled by default, which is appropriate for local development and tests, but will need enabling when deployed (if not already enabled by environmental configuration).

#### Streaming

Streaming is supported with `HttpClientV2`, and will be audited in the same way as non-streamed calls. Note that payloads will be truncated in audit logs if they exceed the max supported (as configured by `http-verbs.auditing.maxBodyLength`).

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

This means audits that were rejected for being too large with HttpClient will probably be accepted with HttpClientV2.

:warning: Please check any potential impact this may have on auditing behaviour.


### URL interpolator

A [URL interpolator](https://sttp.softwaremill.com/en/latest/model/uri.html) has been provided to help with escaping query and path parameters correctly.

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

Headers are forwarded differently to hosts depending on whether they are _internal_ or _external_. This distinction is made by the [internalServiceHostPatterns](http-verbs-play-30/src/main/resources/reference.conf) configuration.

##### External hosts

For external hosts, only the User-Agent header is sent. Any other headers should be provided explicitly to the VERB function (`get`, `post` etc).

```scala
httpClientV2
  .get(url"https://externalhost/api")(hc)
  .setHeader("Authorization" -> "Bearer token") // explicit Authorization header for external request
```

##### Internal hosts

In addition to the User agent, all headers that are modelled _explicitly_ in the `HeaderCarrier` are forwarded to internal hosts. It will also forward any other headers in the `HeaderCarrier` if listed in the `bootstrap.http.headersAllowlist` configuration.

You can replace any of these implicitly forwarded headers or add any others by providing them to the `setHeader` function.


Note, for the original `HttpClient`, headers provided to the VERB function are sent in _addition_ to those in the HeaderCarrier, so if you want to replace one, you will have to manipulate the `HeaderCarrier` e.g.:
  ```scala
  client.GET("https://internalhost/api")(hc.copy(authorization = Some(Authorization("Basic 1234"))))
  ```

### Deserialising Response

The Response is deserialised by an instance of [HttpReads](http-verbs-play-30/src/main/scala/uk/gov/hmrc/http/HttpReads.scala).

You can either create your own instances or use the provided instances with

```scala
import uk.gov.hmrc.http.HttpReads.Implicits._
```

The default implicits (without explicit import) have been deprecated. See [here](CHANGELOG.md#version-1100) for more details.

The `HttpReads` describes how to convert a `HttpResponse` into your model using the status code and response body.

The [provided instances](http-verbs-play-30/src/main/scala/uk/gov/hmrc/http/HttpReadsInstances.scala), brought into scope with the above import, allow you to:

  - Request raw HttpResponse:
    ```scala
    client.GET[HttpResponse](url)
    ```
  - Convert the response body from Json using a play json reads:
    ```scala
    implicit val reads: Reads[MyModel] = ???
    client.get[MyModel](url)
    ```
    Note this instance will return failed Futures with `UpstreamErrorResponse` for non-success status codes. Json parsing failures will similarly be returned as `JsValidationException` These exceptions can be recovered from if required.
  - Handle 404s with `None`
    ```scala
    implict val reads: Reads[MyModel] = ???
    client.get[Option[MyModel]](url)
    ```
  - Return non-success status codes as `UpstreamErrorResponse` in `Either`
    ```scala
    implict val reads: Reads[MyModel] = ???
    client.get[Either[UpstreamErrorResponse, MyModel]](url)
    ```


## Testing

In your SBT build add the following in your test dependencies:

```scala
libraryDependencies += "uk.gov.hmrc" %% "http-verbs-test-play-xx" % "x.x.x" % Test
```

We recommend that [Wiremock](http://wiremock.org/) is used for testing http-verbs code, with extensive assertions on the URL, Headers, and Body fields for both requests and responses. This will test most things, doesn't involve "mocking what you don't own", and ensures that changes to this library will be caught. I.e. that the result of using this library is what was intended, not just if the library was invoked as expected.

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

The `HttpClientV2Support` trait can provide an instance of `HttpClientV2` as an alternative to instanciating the application:
```scala
class MyConnectorSpec extends WireMockSupport with HttpClientV2Support {
  private val connector = new MyConnector(
      httpClientV2,
      Configuration("connector.url" -> wireMockUrl)
  )
}
```

The `ExternalWireMockSupport` trait is an alternative to `WireMockSupport` which uses `127.0.0.1` instead of `localhost` for the hostname which is treated as an external host for header forwarding rules. This should be used for tests of connectors which call endpoints external to the platform. The variable `externalWireMockHost` (or `externalWireMockUrl`) should be used to provide the hostname in configuration.

Both `WireMockSupport` and `ExternalWireMockSupport` can be used together for integration tests if required.


The `ResponseMatchers` trait provides some useful helpers for testing responses.



## License ##

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
