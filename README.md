http-verbs
==========

[![Build Status](https://travis-ci.org/hmrc/http-verbs.svg)](https://travis-ci.org/hmrc/http-verbs)

http-verbs is a Scala library providing an interface to make asynchronous HTTP calls.  The underlying implementation uses [Play WS](https://www.playframework.com/documentation/latest/ScalaWS).

It encapsulates some common concerns for calling other HTTP services on the HMRC Tax Platform, including:

* Auditing
* Logging
* Propagation of common headers
* Response handling, converting failure status codes into a consistent set of exceptions - allows failures to be automatically propagated to the caller
* Request & Response de-serialization

## Adding to your service

Include the following dependency in your SBT build

```scala
resolvers += Resolver.bintrayRepo("hmrc", "releases")

libraryDependencies += "uk.gov.hmrc" %% "http-verbs" % "1.3.0"
```

Request auditing is provided for all HTTP requests that are made using this library. Each request/response pair results in an audit message being created and sent to an external auditing service for processing.  To configure this service, your Play configuration file needs to include:

```javascript
Prod {
  auditing {
    enabled = true
    traceRequests = true
    consumer {
      baseUri {
        host = <auditing host name>
        port = <auditing host port>
      }
    }
  }
}
```

## Usage

All calls require an implicit `HeaderCarrier` to be in scope. HTTP headers in this class are added every request made, which allows for common headers to be set on all requests; principally to allow for headers to be propagate from an initiating request.

### HTTP GET

Create a HTTP GET client:
```scala
  val httpGet = new WSGet {
    override def appName = "my-app-name"
    override def auditConnector = new Auditing {
      override def auditingConfig = LoadAuditingConfig(s"Prod.auditing")
    }
  }
```
Responses can be returned in many formats. In all cases, responses statuses which indicate errors are converted to failed `Future`s with typed exceptions. This allows failures to be propagated back to the original requestor.

Status Code   | Exception
------------- | -------------
400           | `BadRequestException`
404           | `NotFoundException`
4xx           | `Upstream4xxResponse`
5xx           | `Upstream5xxResponse`

If some failure status codes are expected in normal flow, then special response readers are available, or the returned future can be recovered: 
```scala
  httpGet.GET[MyCaseClass]("url") map {
    response =>
      //success
  } recover {
    case notFound: NotFoundException => {}
    case serverError: Upstream5xxResponse => {}
  }
```


#### JSON responses
In most cases, where JSON is used, having an implicit `play.api.libs.json.Reads[A]` for your class in scope allows automatic de-serialisation to occur.

```scala
  implicit val f = Json.reads[MyCaseClass]
  httpGet.GET[MyCaseClass](url) // Returns an MyCaseClass de-serialised from JSON
```

#### HTML responses
For HTML responses, Play's `Html` type can be used:
```scala                                      
  httpGet.GET[Html](url) // Returns a Play Html type
```

#### Potentially empty responses
If you expect to receive a `204` or `404` response in some circumstances, then you can add `Option[...]` to your return type:

```scala
  httpGet.GET[Option[MyCaseClass]](url) // Returns a None, or Some[MyCaseClass] de-serialised from JSON
  httpGet.GET[Option[Html]](url) // Returns a None, or a Play Html type

```

#### Plain HTTP response
If access to the status code, raw body and headers are required without de-serialisation, the `HttpResponse` type can be used
```scala
  val response = httpGet.GET[HttpResponse](url) // Returns the Http Response
  response.status
  response.body
  response.allHeaders
```

### HTTP POST
Create a HTTP POST client:

```scala
  val httpPost = new WSPost {
    override def appName: String = "my-app"
    override def auditConnector: Auditing = new Auditing {
      override def auditingConfig: AuditingConfig = LoadAuditingConfig(s"Prod.auditing")
    }
  }
```

#### JSON requests
Having an implicit `Reads[A]` for your class in scope allows automatic serialisation to occur.  Headers can be provided as a sequence of string tuples.
```scala
  implicit val f = Json.reads[MyCaseClass]
  val postBody = MyCaseClass("user", 10)
  httpPost.doPost(url, postBody, headers)
```

## Implementation & Extension
Response handling is implemented via the `HttpReads[A]` typeclass, which is responsible for converting the raw response into either an exception or the specified type. Default implementations of `HttpReads[A]` have been provided in its companion object to cover common use cases, but clients may provide their own implementations if required. 
bbb error handling currently applied to all responses (translating `400` to `BadRequestException` etc.) is used in all of these readers. All `GET_*` can now be deprecated - a message has been aded to each explaining what should be used instead.

## Configuration
```HttpAuditing``` now provides ```def auditDisabledForPattern = ("""http://.*\.service""").r``` which client applications may chose to override when mixing in ```HttpAuditing```.

_NOTE:_ This configuration used to be provided by reading Play configuration property ```<env>.http-client.audit.disabled-for``` which is now obsolete.

