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
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, Retries}
import uk.gov.hmrc.play.http.ws.WSProxyConfiguration
import uk.gov.hmrc.http.hooks.{HookData, HttpHook}
import uk.gov.hmrc.http.logging.ConnectionTracing

import java.net.URL
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

trait Executor {
  def execute[A](
    request : WSRequest,
    isStream: Boolean
  )(
    transformResponse: (WSRequest, Future[WSResponse]) => Future[A]
  )(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[A]
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

  override protected def mkRequestBuilder(
    url   : URL,
    method: String
  )(implicit
    hc: HeaderCarrier
  ): RequestBuilderImpl =
    new RequestBuilderImpl(
      config,
      optProxyServer,
      new ExecutorImpl(actorSystem, config, hooks)
    )(
      wsClient
        .url(url.toString)
        .withMethod(method)
        .withHttpHeaders(hc.headersForUrl(hcConfig)(url.toString) : _*)
    )
}


// is final since `tranform` (and derived functions) return instances of RequestBuilderImpl, and any overrides would be lost.
final class RequestBuilderImpl(
  config        : Configuration,
  optProxyServer: Option[WSProxyServer],
  executor      : Executor
)(
  request: WSRequest
)(implicit
  hc: HeaderCarrier
) extends RequestBuilder {

  override def transform(transform: WSRequest => WSRequest): RequestBuilderImpl =
    new RequestBuilderImpl(config, optProxyServer, executor)(transform(request))

  // -- Transform helpers --

  override def replaceHeader(header: (String, String)): RequestBuilderImpl = {
    def denormalise(hdrs: Map[String, Seq[String]]): Seq[(String, String)] =
      hdrs.toList.flatMap { case (k, vs) => vs.map(k -> _) }
    val hdrsWithoutKey = request.headers.filterKeys(!_.equalsIgnoreCase(header._1)).toMap // replace existing header
    transform(_.withHttpHeaders(denormalise(hdrsWithoutKey) :+ header : _*))
  }

  override def addHeaders(headers: (String, String)*): RequestBuilderImpl =
    transform(_.addHttpHeaders(headers: _*))

  override def withProxy: RequestBuilderImpl =
    transform(request => optProxyServer.foldLeft(request)(_ withProxyServer _))

  override def withBody[B : BodyWritable](body: B): RequestBuilderImpl =
    (if (body == EmptyBody)
      replaceHeader(play.api.http.HeaderNames.CONTENT_LENGTH -> "0") // rejected by Akami without a Content-Length (https://jira.tools.tax.service.gov.uk/browse/APIS-5100)
    else
      this
    ).transform(_.withBody(body))

  // -- Execution --

  override def execute[A](
    transformResponse: (WSRequest, Future[WSResponse]) => Future[A]
  )(implicit
    ec: ExecutionContext
  ): Future[A] =
    executor.execute(request, isStream = false)(transformResponse)

  override def stream[A](
    transformResponse: (WSRequest, Future[WSResponse]) => Future[A]
  )(implicit
    ec: ExecutionContext
  ): Future[A] =
    executor.execute(request, isStream = true)(transformResponse)
}

class ExecutorImpl(
  override val actorSystem: ActorSystem, // for Retries
  config: Configuration,
  hooks : Seq[HttpHook]
) extends Executor
     with Retries
     with ConnectionTracing {

  // for Retries
  override val configuration: Config = config.underlying

  def execute[A](
    request : WSRequest,
    isStream: Boolean
  )(
    transformResponse: (WSRequest, Future[WSResponse]) => Future[A]
  )(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[A] = {
    val startAge  = System.nanoTime() - hc.age
    val responseF =
      retryOnSslEngineClosed(request.method, request.url)(
        // we do the execution since if clients are responsable for it (e.g. a callback), they may further modify the request outside of auditing etc.
        if (isStream) request.stream() else request.execute()
      )
    executeHooks(isStream, request, responseF)
    responseF.onComplete(logResult(hc, request.method, request.uri.toString, startAge))
    // we don't delegate the response conversion to the client
    // (i.e. return Future[WSResponse] to be handled with Future.transform/transformWith(...))
    // since the transform functions require access to the request (method and url)
    transformResponse(request, responseF)
  }

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
        body    = if (isStream)
                    // calling response.body on stream would load all into memory (and stream would need to be broadcast to be able
                    // to read twice - although we could cap it like in uk.gov.hmrc.play.bootstrap.filters.RequestBodyCaptor)
                    "<stream>"
                  else response.body,
        headers = response.headers.mapValues(_.toSeq).toMap
      )

    def denormalise(hdrs: Map[String, Seq[String]]): Seq[(String, String)] =
      hdrs.toList.flatMap { case (k, vs) => vs.map(k -> _) }

    // TODO discuss changes with CIP
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

    println(s"""AUDIT:
        verb      = ${request.method},
        url       = ${new URL(request.url)},
        headers   = ${denormalise(request.headers)},
        body      = $body,
        responseF = ${responseF.map(toHttpResponse)}
        """)
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
