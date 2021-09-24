/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// TODO putting this in this package means that all clients which do
// `import uk.gov.hmrc.http._` will then have to make play imports with _root_ `import _root_.play...`
package uk.gov.hmrc.http.play

import akka.actor.ActorSystem
import com.typesafe.config.Config
import play.api.Configuration
import play.api.libs.ws.{BodyWritable, EmptyBody, InMemoryBody, SourceBody, WSClient, WSProxyServer, WSRequest, WSResponse}
import play.core.parsers.FormUrlEncodedParser
import uk.gov.hmrc.http.{BadGatewayException, GatewayTimeoutException, HeaderCarrier, HttpResponse, Retries}
import uk.gov.hmrc.play.http.ws.WSProxyConfiguration
import uk.gov.hmrc.http.hooks.{HookData, HttpHook}
import uk.gov.hmrc.http.logging.ConnectionTracing

import java.net.{ConnectException, URL}
import java.util.concurrent.TimeoutException
import scala.concurrent.{ExecutionContext, Future}

/* What does HttpVerbs actually provide?

Readme says...
    - Http Transport
    - Core Http function interfaces
    - Logging
    - Propagation of common headers
    - Executing hooks, for example Auditing
    - Request & Response de-serializations
    - Response handling, converting failure status codes into a consistent set of exceptions - allows failures to be automatically propagated to the caller

Also, retries


This version demonstrates a flat implementation that uses an HttpExecutor to centralise the execution of the request to ensure that
the common concerns occur, but delegates out the construction of the request and parsing of the response to play-ws for flexibility.
The use of HttpReads is optional.
Extension methods are provided to make common patterns easier to apply.
*/

trait HttpClient2 {
  def get(url: URL)(implicit hc: HeaderCarrier): RequestBuilder

  def post(url: URL)(implicit hc: HeaderCarrier): RequestBuilder

  def post[B: BodyWritable](url: URL, body: B)(implicit hc: HeaderCarrier): RequestBuilder

  def put(url: URL)(implicit hc: HeaderCarrier): RequestBuilder

  def put[B: BodyWritable](url: URL, body: B)(implicit hc: HeaderCarrier): RequestBuilder

  def delete(url: URL)(implicit hc: HeaderCarrier): RequestBuilder

  def patch(url: URL)(implicit hc: HeaderCarrier): RequestBuilder

  def patch[B: BodyWritable](url: URL, body: B)(implicit hc: HeaderCarrier): RequestBuilder

  def head(url: URL)(implicit hc: HeaderCarrier): RequestBuilder

  def options(url: URL)(implicit hc: HeaderCarrier): RequestBuilder
}

trait RequestBuilder {
  def transformRequest(transform: WSRequest => WSRequest): RequestBuilder

  def execute[A](
    transform: WSRequest => WSResponse => Future[A]
  )(implicit
    ec: ExecutionContext
  ): Future[A]

  def stream[A](
    transform: WSRequest => WSResponse => Future[A]
  )(implicit
    ec: ExecutionContext
  ): Future[A]

  // support functions

  def replaceHeader(header: (String, String)): RequestBuilder

  def addHeaders(headers: (String, String)*): RequestBuilder

  def withProxy: RequestBuilder

  def withBody[B : BodyWritable](body: B): RequestBuilder
}

class HttpClient2Impl(
  wsClient   : WSClient,
  actorSystem: ActorSystem,
  config     : Configuration,
  hooks      : Seq[HttpHook]
) extends HttpClient2 {

  private lazy val optProxyServer =
    WSProxyConfiguration.buildWsProxyServer(config.underlying)

  private lazy val hcConfig =
    HeaderCarrier.Config.fromConfig(config.underlying)

  override def get(url: URL)(implicit hc: HeaderCarrier): RequestBuilderImpl =
    mkRequestBuilder(url, "GET")

  override def post(url: URL)(implicit hc: HeaderCarrier): RequestBuilderImpl =
    mkRequestBuilder(url, "POST")

  // TODO or just let clients call `.withBody(body)` themselves (this is the expectation for adding headers)
  override def post[B: BodyWritable](url: URL, body: B)(implicit hc: HeaderCarrier): RequestBuilderImpl =
    mkRequestBuilder(url, "POST")
      .withBody(body)

  override def put(url: URL)(implicit hc: HeaderCarrier): RequestBuilderImpl =
    mkRequestBuilder(url, "PUT")

  override def put[B: BodyWritable](url: URL, body: B)(implicit hc: HeaderCarrier): RequestBuilderImpl =
    mkRequestBuilder(url, "PUT")
      .withBody(body)

  override def delete(url: URL)(implicit hc: HeaderCarrier): RequestBuilderImpl =
    mkRequestBuilder(url, "DELETE")

  override def patch(url: URL)(implicit hc: HeaderCarrier): RequestBuilderImpl =
    mkRequestBuilder(url, "PATCH")

  override def patch[B: BodyWritable](url: URL, body: B)(implicit hc: HeaderCarrier): RequestBuilderImpl =
    mkRequestBuilder(url, "PATCH")
      .withBody(body)

  override def head(url: URL)(implicit hc: HeaderCarrier): RequestBuilderImpl =
    mkRequestBuilder(url, "HEAD")

  override def options(url: URL)(implicit hc: HeaderCarrier): RequestBuilderImpl =
    mkRequestBuilder(url, "OPTIONS")

  private def mkRequestBuilder(
    url   : URL,
    method: String
  )(implicit
    hc: HeaderCarrier
  ): RequestBuilderImpl =
    new RequestBuilderImpl(
      actorSystem,
      config,
      optProxyServer,
      hooks
    )(
      wsClient
        .url(url.toString)
        .withMethod(method)
        .withHttpHeaders(hc.headersForUrl(hcConfig)(url.toString) : _*)
    )
  }


// is final since tranformRequest (and derived) return instances of RequestBuilderImpl, and would loose any overrides.
final class RequestBuilderImpl(
  override val actorSystem: ActorSystem,
  config                  : Configuration,
  optProxyServer          : Option[WSProxyServer],
  hooks                   : Seq[HttpHook]
)(
  request: WSRequest
)(implicit
  hc: HeaderCarrier
) extends RequestBuilder
     with Retries
     with ConnectionTracing {

  // for Retries (TODO make it use Configuration too)
  override val configuration: Config = config.underlying

  override def transformRequest(transform: WSRequest => WSRequest): RequestBuilderImpl =
    new RequestBuilderImpl(actorSystem, config, optProxyServer, hooks)(transform(request))

  // -- Syntactic sugar --
  // TODO any implementation would be expected to implement them all
  // should they be available as extension methods? (less discoverable - require `import httpClient._` to enable)
  // they all depend on `transformRequest` - but also configuration (and other derived/cached values like proxyServer) could they move into the API interface?
  // also the variable RequestBuilder makes this tricky...

  override def replaceHeader(header: (String, String)): RequestBuilderImpl = {
    def denormalise(hdrs: Map[String, Seq[String]]): Seq[(String, String)] =
      hdrs.toList.flatMap { case (k, vs) => vs.map(k -> _) }
    val hdrsWithoutKey = request.headers.filterKeys(!_.equalsIgnoreCase(header._1)) // replace existing header
    transformRequest(_.withHttpHeaders(denormalise(hdrsWithoutKey) :+ header : _*))
  }

  override def addHeaders(headers: (String, String)*): RequestBuilderImpl =
    transformRequest(_.addHttpHeaders(headers: _*))

  override def withProxy: RequestBuilderImpl =
    transformRequest(request => optProxyServer.foldLeft(request)(_ withProxyServer _))

  override def withBody[B : BodyWritable](body: B): RequestBuilderImpl =
    (if (body == EmptyBody)
      replaceHeader(play.api.http.HeaderNames.CONTENT_LENGTH -> "0") // rejected by Akami without a Content-Length (https://jira.tools.tax.service.gov.uk/browse/APIS-5100)
    else
      this
    ).transformRequest(_.withBody(body))

  // -- Execution --

  override def execute[A](
    transformResponse: WSRequest => WSResponse => Future[A]
  )(implicit
    ec: ExecutionContext
  ): Future[A] =
    execute(isStream = false)(transformResponse)

  override def stream[A](
    transformResponse: WSRequest => WSResponse => Future[A]
  )(implicit
    ec: ExecutionContext
  ): Future[A] =
    execute(isStream = true)(transformResponse)

  private def execute[A](
    isStream: Boolean
  )(
    transformResponse: WSRequest => WSResponse => Future[A]
  )(implicit
    ec: ExecutionContext
  ): Future[A] = {
    val startAge  = System.nanoTime() - hc.age
    val responseF =
      // TODO a way for clients to define custom retries?
      retryOnSslEngineClosed(request.method, request.url)(
        // we do the execution since if clients are responsable for it (e.g. a callback), they may further modify the request outside of auditing etc.
        if (isStream) request.stream() else request.execute()
      )
    executeHooks(isStream, request, responseF)
    responseF.onComplete(logResult(hc, request.method, request.uri.toString, startAge))
    // we don't delegate the response conversion to the client
    // e.g. execute[WSResponse].transform(...) since the transform functions require access to the request (method and url)
    // given method and url are only required for error messages, is it overkill? E.g. a stacktrace should identify the function?
    mapErrors(request.method, request.url, responseF)
    responseF.flatMap(transformResponse(request))
  }

  // mapErrors could be part of the transform function. e.g. transformResponse: WSRequest => Try[WSResponse] => Future
  // but then each transformResponse would probably end up doing the same recovery? Is that a problem?
  def mapErrors(
    httpMethod: String,
    url       : String,
    f         : Future[WSResponse]
  )(implicit
    ec: ExecutionContext
  ): Future[WSResponse] =
    f.recoverWith {
      case e: TimeoutException => Future.failed(new GatewayTimeoutException(gatewayTimeoutMessage(httpMethod, url, e)))
      case e: ConnectException => Future.failed(new BadGatewayException(badGatewayMessage(httpMethod, url, e)))
    }

  def badGatewayMessage(verbName: String, url: String, e: Exception): String =
    s"$verbName of '$url' failed. Caused by: '${e.getMessage}'"

  def gatewayTimeoutMessage(verbName: String, url: String, e: Exception): String =
    s"$verbName of '$url' timed out with message '${e.getMessage}'"


  private def executeHooks(
    isStream : Boolean,
    request  : WSRequest,
    responseF: Future[WSResponse]
  )(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Unit = {
    // hooks take HttpResponse..
    def toHttpResponse(response: WSResponse) =
      HttpResponse(
        status  = response.status,
        body    = if (isStream) "<stream>" else response.body, // calling response.body on stream would load all into memory (and cause stream to be read twice..)
        headers = response.headers
      )

    def denormalise(hdrs: Map[String, Seq[String]]): Seq[(String, String)] =
      hdrs.toList.flatMap { case (k, vs) => vs.map(k -> _) }

    val body =
      request.body match {
        case EmptyBody           => None
        case InMemoryBody(bytes) => request.header("Content-Type") match {
                                      case Some("application/x-www-form-urlencoded") => Some(HookData.FromMap(FormUrlEncodedParser.parse(bytes.decodeString("UTF-8"))))
                                      case Some("application/octet-stream")          => Some(HookData.FromString("<binary>"))
                                      case _                                         => Some(HookData.FromString(bytes.decodeString("UTF-8")))
                                    }
        case SourceBody(_)       => Some(HookData.FromString("<stream>"))
      }

    hooks.foreach(
      _.apply(
        verb      = request.method,
        url       = new URL(request.url),
        headers   = denormalise(request.headers),
        body      = body,
        responseF = responseF.map(toHttpResponse)
      )
    )
  }
}
