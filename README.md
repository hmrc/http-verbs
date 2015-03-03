http-verbs
==========

http-verbs is a Scala library providing an interface to make asynchronous HTTP calls.  The underlying implementation uses [Play WS](https://www.playframework.com/documentation/latest/ScalaWS).

It encapsulates some common concerns for calling other HTTP services on the HMRC Tax Platform, including:

* Auditing HTTP calls
* Logging HTTP calls
* Propagation of headers
* Response handling, converting failure status codes into a consistent set of exceptions - allows failures to be automatically propagated to the caller
* Request & Response de-serialization

## Usage

Include the following dependency in your SBT build
```scala
libraryDependencies += "uk.gov.hmrc" %% "http-verbs" % "1.0.0"
```

Request auditing is provided implicitly for all Http requests that are made using this library.  Each request results in an audit message being created and sent to an external auditing service for processing.  An auditing configuration is required in order to configure this service, this can be added to your Play configuration file:
```json
Prod {
  auditing {
    enabled = true
    traceRequests = true
    consumer {
      baseUri {
        host = datastream.service
        port = 80
      }
    }
  }
}
```

An implicit `HeaderCarrier` must be in scope for all HTTP requests made.  These header parameters are proxied through to every request made.

### HTTP GET

Create a HTTP GET client:
```scala
  val httpGet = new WSGet {
    override def appName: String = "my-app-name"
    override def auditConnector: Auditing = new Auditing {
      override def auditingConfig: AuditingConfig = LoadAuditingConfig(s"Prod.auditing")
    }
  }
```

#### GET JSON resource
In most cases, where JSON is used, having an implicit `Reads[A]` for your class in scope allows automatic de-serialisation to occur.
```scala
  implicit val f = Json.reads[MyCaseClass]
  httpGet.GET[MyCaseClass](url) // Returns an MyCaseClass de-serialised from JSON

  // Or if the resource is optional (204, 404 response codes are mapped to an Option value of None)
  httpGet.GET[Option[MyCaseClass]](url) // Returns an Option[MyCaseClass] de-serialised from JSON

```

#### GET Generic Http response
If access to the status code, raw body and headers are required without de-serialisation, the `HttpResponse` type can be used
```scala
  val response = httpGet.GET[HttpResponse](url) // Returns the Raw Http Response
  response.status
  response.body
  response.allHeaders
```

#### GET HTML resource
For HTML responses, Play's `Html` type can be used:
```scala                                      
  httpGet.GET[Html](url) // Returns a Play Html type
```

#### GET JSON Collection
Collections can be read using the following technique:
```scala
httpGet.GET(url)(HttpReads.readSeqFromJsonProperty[MyCaseClass]("items"), hc) // Returns a Seq[MyCaseClass] from the "items" json property
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

#### POST a JSON resource
Having an implicit `Reads[A]` for your class in scope allows automatic serialisation to occur.  Headers can be provided as a sequence of string tuples.
```scala
  implicit val f = Json.reads[MyCaseClass]
  val postBody = MyCaseClass("user", 10)
  httpPost.doPost(url, postBody, headers)
```

### Error Handling
Exceptions will be thrown for common error status code responses.

Status Code   | Exception
------------- | -------------
400           | `BadRequestException`
404           | `NotFoundException`
4xx           | `Upstream4xxResponse`
5xx           | `Upstream5xxResponse`

The future result will fail if an exception is thrown.  These can be handled using recover, for example
```scala
  httpGet.GET[MyCaseClass]("url") map {
    response =>
      //success
  } recover {
    case notFound: Upstream4xxResponse => {}
    case serverError: Upstream5xxResponse => {}
  }
```

## Implementation & Extension
We have abstracted away from using the JSON-specific `play.api.libs.json.Reads[A]`, instead an `HttpReads[A]` is required: 

```scala
def GET[A](url: String)(implicit rds: HttpReads[A], hc: HeaderCarrier)
```
This new reader is responsible for converting the raw response into either an exception or the requested type.

Implementations of `HttpReads[A]` have been provided in its companion object to replace current other GET methods:

* **`HttpReads.readFromJson`** ensures backwards-compatibility with the current API by implicitly converting `json.Reads[A]` to a `HttpReads[A]`.
* **`HttpReads.readToHtml`** returns a successful response as `play.twirl.api.Html`, which is natural for passing into other templates as partials.
* **`HttpReads.readRaw`** returns a successful response as a raw `HttpResponse` object.
* **`HttpReads.readSeqFromJsonProperty`** mirrors the current `GET_Collection` functionality. 
* **`HttpReads.readOptionOf`** expands on the current `GET_Optional`, allowing any type to be treated as optional.

The error handling currently applied to all responses (translating `400` to `BadRequestException` etc.) is used in all of these readers. All `GET_*` can now be deprecated - a message has been aded to each explaining what should be used instead.
