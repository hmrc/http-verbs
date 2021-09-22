
## Version 13.9.0

| Change | Complexity | Fix  |
|--------------------------------------------------------------|------------|-----------------------------------------------|
| WSProxy changes  | Minor  | Optional change |
| `withUserAgent` added | Minor | Optional change |
| `withTransformRequest` added | Minor | Optional change |

For the following changes, it is expected that you will be using the `uk.gov.hmrc.HttpClientImpl` implementation of `HttpClient` (which should be provided by bootstrap-play). They may not be supported on custom implementations of `HttpClient`.

### WSProxy

`WSProxy` has been deprecated, the behaviour is available on `HttpClient`.

What this means:
  * You will **not** require two HttpClient implementations to use a proxy. Instead you can call `withProxy` on the same single HttpClient.
  * It will still need enabling with `proxy.enabled` in configuration, which by default is false, for development. See `WSProxyConfiguration` for configuration changes below.

```scala
// without proxy
httpClient.GET(url)

// with proxy (and `proxy.enabled=true` in configuration)
httpClient.withProxy.GET(url)
```

`WSProxyConfiguration.apply` has been deprecated, use `WSProxyConfiguration.buildWsProxyServer` instead.

There are some differences with `WSProxyConfiguration.buildWsProxyServer` (which is used by `httpClient.withProxy`):
  * configPrefix is fixed to `proxy`.
  * `proxy.proxyRequiredForThisEnvironment` has been replaced with `proxy.enabled`, but note, it defaults to false (rather than true). This is appropriate for development and tests, but will need explicitly enabling when deployed.

### withUserAgent

The useragent defaults to `appName` from configuration. This can be overridden for a single endpoint with the `withUserAgent` function.

```scala
httpClient.withUserAgent("new-user-agent").GET(url)
```

### withTransformRequest

You can modify a request before it is executed with `withTransformRequest`. It is preferrable to use this rather than extending HttpClient to override `def buildRequest(url: String, headers: Seq[(String, String)]): play.api.libs.ws.WSRequest`.

```scala
httpClient.withTransformRequest(_.withRequestTimeout(60.seconds)).GET(url)
```

## Version 13.0.0

| Change | Complexity | Fix  |
|--------------------------------------------------------------|------------|-----------------------------------------------|
| Headers no longer automatically forwarded to external hosts  | **Major**  | Team decision required on individual services |
| HeaderCarrierConverter method changes                        | Medium     | [Scalafix available](https://github.com/hmrc/scalafix-rules/blob/master/http-verbs-13/rules/src/main/scala/fix/HeaderCarrier.scala) |
| Configuration no longer optional on some traits              | Medium     | [Scalafix available](https://github.com/hmrc/scalafix-rules/blob/master/http-verbs-13/rules/src/main/scala/fix/OptionalConfig.scala) |
| HeaderNames package change                                   | Minor      | [Scalafix available](https://github.com/hmrc/scalafix-rules/blob/master/http-verbs-13/rules/src/main/scala/fix/HttpVerbs13RenamePackages.scala) |
| URLs can now be supplied as `java.net.URL`                   | Minor      | Optional change |
| Removed deprecated values from `SessionKeys`                 | Minor      | Use auth client retrievals |
| All sent request headers explicitly passed to HttpHook       | Minor      | Distinction between sent headers and HeaderCarrier is useful|
| Transport layer explicitly passed all headers (e.g doPost)   | Minor      | All headers can be obtained from HeaderCarrier.headersForUrl |

To run a scalafix rule on your project, please refer to [the usage docs](https://github.com/hmrc/scalafix-rules#usage).

### Headers no longer automatically forwarded to external hosts
Explicit headers (those modelled explicitly in the `HeaderCarrier`) are no longer automatically forwarded to external hosts. **This is a potentially breaking change, dependent on the actions of a particular microservice.** We expect a majority of services to see no impact with this change.

Our services rely on common headers sent between microservices, but these are not required externally. Sending certain headers to external hosts has security implications.

To provide a header from the HeaderCarrier to an external party, the header should be provided explicitly via the *VERB* methods (GET, POST etc.).
You can look them up from the headerCarrier by name. E.g. to forward Authorization and X-Request-Id header (case insensitive):
```scala
client.GET("https://externalhost/api", headers = hc.headers("Authorization", "X-Request-Id"))
```

#### Internal hosts

There is no change in which headers are forwarded to internal hosts.

The hosts identified as internal is configured by `internalServiceHostPatterns` and now includes `localhost` as default. Platform application configuration will provide all required defaults.

Please see the [README](README.md#propagation-of-headers) for more details about usage.

### HeaderCarrierConverter method changes
HeaderCarrierConverter has moved package, and consolidated and simplified the available methods.

`HeaderCarrierConverter.fromHeadersAndSession` and `HeaderCarrierConverter.fromHeadersAndSessionAndRequest` are deprecated. The methods replacing them are `HeaderCarrierConverter.fromRequest` and `HeaderCarrierConverter.fromRequestAndSession` instead, which have less parameter permutations, and easier to discover and document.

Please see the [README](README.md#creating-headercarrier) for example usage.

A scalafix for this specific change is available to run.

### HeaderNames package change
The Headers `Authorization`, `ForwardedFor`, `RequestChain`, `RequestId`, `SessionId` have been moved from package `uk.gov.hmrc.http.logging` to `uk.gov.hmrc.http`.

We believe this was a piece of technical debt, from when these cases classes lived in a different project.

A scalafix for this specific change is available to run.

### Configuration no longer optional on some traits
`configuration` as required by some traits (`WSRequestBuilder` and `Retries`) has changed from `Option[com.typesafe.config.Config]` to `com.typesafe.config.Config`.

This change makes it easier to reason about default behaviour, both for developers, and for automated tooling. Prior to this change, the defaults were split between configuration files and fallback `getOrElse` methods in code.

If you don't want to provide any specific configuration (e.g. testing), then change `configuration = None` to `configuration = com.typesafe.config.ConfigFactory.load()`.

A scalafix for this specific change is available to run.

### URLs can now be supplied as `java.net.URL`

Providing URLs as Strings encourages using string concatenation to build URLs, which may cause unintended issues with escaping. Internally, all Strings parameters are converted to URLs, but exposing this in the API provides earlier feedback that usage may be wrong. String is still supported.

Please see the [README](README.md#urls) for more information.

### Removed deprecated values from `SessionKeys`

The deprecated values (since 2016) `SessionKeys.name`, `SessionKeys.email`, `SessionKeys.agentName`, and `SessionKeys.affinityGroup` have been deleted. [Auth client retrievals](https://github.com/hmrc/auth-client/blob/master/src/main/scala/uk/gov/hmrc/auth/core/retrieve/v2/Retrievals.scala) should be used instead.

## Version 12.0.0

### SessionKeys
The fields `authProvider` ('ap'), `userId` and `token` are no longer available in the session and these names have been removed from SessionKeys object.

### HeaderCarrier
The fields `token` and `userId` are no longer available in the session and so have been removed from HeaderCarrier

### Configuration
The configuration key `httpHeadersWhitelist` has been replaced with `bootstrap.http.headersAllowlist`. `httpHeadersWhitelist` will no longer take effect.

## Version 11.0.0

### HttpReads

The default implicits for `HttpReads` have been deprecated. There are new implicits, which need to be pulled in explicitly with
```scala
import uk.gov.hmrc.http.HttpReads.Implicits._
```
The behaviour of the predefined implicits are not quite the same as the deprecated ones, and you are encouraged to define your own HttpReads if none are apropriate. The differences are:
* You will have to explicitly state the type of the response - it will not resolve to `HttpResponse` if none is specified. (i.e. `GET(url)` will now be `GET[HttpResponse](url)`). It is deemed better to be explicit since the type will dictate how errors are handled.
* The default `HttpRead[HttpResponse]` will no longer throw an exception if there is a non-2xx status code. Since the HttpResponse already encodes errors, it expects you will handle this yourself. To get the behaviour similar to previous (see Exceptions for differences), use:
```scala
implicit val legacyRawReads = HttpReads.Implicits.throwOnFailure(HttpReads.Implicits.readEitherOf(HttpReads.Implicits.readRaw))
```
* `HttpReads[Option[A]]` only returns None for 404, and will try to parse other responses. Previously, 204 was also treated as None, consider representing this with Unit instead.
* The `HttpReads[A]` where `A` is defined by a `play.api.libs.json.Reads[A]` works in the same way as before, i.e. throws exceptions for non-2xx response codes (`UpstreamErrorResponse`), and json parsing errors (`JsValidationException`). Since the http-verbs API operates within `Future`, this is probably the simplest response type, since Future offers recovery, and if not handled, will propagate to the caller. However the HttpReads can be combined with other HttpReads to return the errors in different ways. E.g.
    * `HttpReads[Either[UpstreamErrorResponse, JsResult[A]]]`
    * `HttpReads[Try[JsResult[A]]]`,
      These error encoded types are available for any response type not just json

### HttpResponse

The trait for HttpResponse will be replaced with a case class. You should only create instances with the `HttpResponse.apply` function, and not extend it.
If your clients previously relied on an instance of `WSHttpResponse` being returned, they will have to change to use the `HttpResponse` abstraction.

### Exceptions

The new `HttpReads` instances only return `UpstreamErrorResponse` for failures returned from upstream services. They will no longer return `HttpException` which will be reserved for problems in making the request (e.g. `GatewayTimeoutException` for timeout exceptions and `BadGatewayException` for connect exceptions), and originate in the service itself.
The trait `UpstreamErrorResponse` will be replaced with a case class, and the subclasses `Upstream4xxResponse` and `Upstream5xxResponse` have been deprecated in preparation. If you need to pattern match on these types, use `UpstreamErrorResponse.Upstream4xxResponse` and `UpstreamErrorResponse.Upstream5xxResponse` instead.
