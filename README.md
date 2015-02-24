http-verbs
==========

Library which encapsulates common concerns for calling other HTTP services. This includes:

* Auditing
* Tracing
* Propagation of headers from the external caller
* Response handling, converting failure status codes into a consistent set of exceptions - allows failures to be automatically propagated to the caller
* Request & Response de-serialization

### Usage

Using each of the verbs is natural. In most cases, where JSON is used, have an implicit `Reads[A]` for your class in scope:

```
implicit val f = Json.reads[MyCaseClass]
httpGet.GET[MyCaseClass](url) \\ Returns an MyCaseClass 
                              \\ deserialised from JSON using 
                              \\ readToHtml
```

When it is expected that the resource may not be found or have no content, you can make the type optional:

```
implicit val f = Json.reads[MyCaseClass]
httpGet.GET[Option[MyCaseClass]](url) \\ Returns an Option[MyCaseClass] 
                                      \\ deserialised from JSON using 
                                      \\ readOptionOf(readToHtml)
```

For HTML responses, Play's `Html` type can be used:

```                                      
httpGet.GET[Html](url) \\ Returns an Html using readToHtml

httpGet.GET[Option[Html]](url) \\ Returns an Option[Html] using 
                               \\ readOptionOf(readToHtml)
```

Collections can be read using the following technique:

```
httpGet.GET(url)(readSeqFromJsonProperty[MyCaseClass]("items"), hc) 
                                      \\ Returns a Seq[MyCaseClass] read
                                      \\ from the "items" json property 
``` 

### Implementation & Extension

We have abstracted away from using the JSON-specific `play.api.libs.json.Reads[A]`, instead an `HttpReads[A]` is required: 

```
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
