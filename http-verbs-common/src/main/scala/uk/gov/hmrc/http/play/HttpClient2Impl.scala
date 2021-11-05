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

package uk.gov.hmrc.http.play

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.typesafe.config.Config
import play.api.Configuration
import play.api.libs.ws.{BodyWritable, EmptyBody, InMemoryBody, SourceBody, WSClient, WSProxyServer, WSRequest, WSResponse}
import play.core.parsers.FormUrlEncodedParser
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, Retries}
import uk.gov.hmrc.play.http.ws.WSProxyConfiguration
import uk.gov.hmrc.http.hooks.{HookData, HttpHook}
import uk.gov.hmrc.http.logging.ConnectionTracing

import java.net.URL
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.reflect.runtime.universe.{TypeTag, typeOf}
import scala.util.{Failure, Success}

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
    request  : WSRequest,
    hookDataF: Option[Future[Option[HookData]]],
    isStream : Boolean
  )(
    transformResponse: (WSRequest, Future[HttpResponse]) => Future[A]
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
        .withHttpHeaders(hc.headersForUrl(hcConfig)(url.toString) : _*),
      None
    )
}


// is final since `tranform` (and derived functions) return instances of RequestBuilderImpl, and any overrides would be lost.
final class RequestBuilderImpl(
  config        : Configuration,
  optProxyServer: Option[WSProxyServer],
  executor      : Executor
)(
  request  : WSRequest,
  hookDataF: Option[Future[Option[HookData]]]
)(implicit
  hc: HeaderCarrier
) extends RequestBuilder {

  override def transform(transform: WSRequest => WSRequest): RequestBuilderImpl =
    new RequestBuilderImpl(config, optProxyServer, executor)(transform(request), hookDataF)

  // -- Transform helpers --

  private def replaceHeaderOnRequest(request: WSRequest, header: (String, String)): WSRequest = {
    def denormalise(hdrs: Map[String, Seq[String]]): Seq[(String, String)] =
      hdrs.toList.flatMap { case (k, vs) => vs.map(k -> _) }
    val hdrsWithoutKey = request.headers.filterKeys(!_.equalsIgnoreCase(header._1)).toMap // replace existing header
    request.withHttpHeaders(denormalise(hdrsWithoutKey) :+ header : _*)
  }

  override def replaceHeader(header: (String, String)): RequestBuilderImpl =
    transform(replaceHeaderOnRequest(_, header))

  override def addHeaders(headers: (String, String)*): RequestBuilderImpl =
    transform(_.addHttpHeaders(headers: _*))

  override def withProxy: RequestBuilderImpl =
    transform(request => optProxyServer.foldLeft(request)(_ withProxyServer _))

  private def withHookData(hookDataF: Future[Option[HookData]]): RequestBuilderImpl =
    new RequestBuilderImpl(config, optProxyServer, executor)(request, Some(hookDataF))

  // for erasure
  private object IsMap {
    def unapply[B: TypeTag](b: B): Option[Map[String, Seq[String]]] =
      typeOf[B] match {
        case _ if typeOf[B] =:= typeOf[Map[String, String]]      => Some(b.asInstanceOf[Map[String, String]].map { case (k, v) => k -> Seq(v) })
        case _ if typeOf[B] =:= typeOf[Map[String, Seq[String]]] => Some(b.asInstanceOf[Map[String, Seq[String]]])
        case _                                                   => None
      }
  }

  override def withBody[B : BodyWritable : TypeTag](body: B): RequestBuilderImpl = {
    val hookDataP      = Promise[Option[HookData]]()
    val maxBodyLength  = config.get[Int]("http-verbs.auditing.maxBodyLength")
    val loggingContext = s"outgoing ${request.method} ${request.url} request"
    transform { req =>
      val req2 = req.withBody(body)
      req2.body match {
        case EmptyBody           => hookDataP.success(None)
                                    replaceHeaderOnRequest(req2, play.api.http.HeaderNames.CONTENT_LENGTH -> "0") // rejected by Akami without a Content-Length (https://jira.tools.tax.service.gov.uk/browse/APIS-5100)
        case InMemoryBody(bytes) => // we can't guarantee that the default BodyWritables have been used - so rather than relying on content-type alone, we identify form data
                                    // by provided body type (Map) or content-type (e.g. form data as a string)
                                    (body, req2.header("Content-Type")) match {
                                      case (IsMap(m), _                                        ) => hookDataP.success(Some(HookData.FromMap(m)))
                                      case (_       , Some("application/x-www-form-urlencoded")) => hookDataP.success(Some(HookData.FromMap(FormUrlEncodedParser.parse(bytes.decodeString("UTF-8")))))
                                      case _                                                     => val auditedBody = BodyCaptor.bodyUpto(bytes, maxBodyLength, loggingContext).decodeString("UTF-8")
                                                                                                    hookDataP.success(Some(HookData.FromString(auditedBody)))
                                    }
                                    req2
        case SourceBody(source)  => val src2: Source[ByteString, _] =
                                      source
                                        .alsoTo(
                                          BodyCaptor.sink(
                                            loggingContext   = loggingContext,
                                            maxBodyLength    = maxBodyLength,
                                            withCapturedBody = body => hookDataP.success(Some(HookData.FromString(body.decodeString("UTF-8"))))
                                          )
                                        ).recover {
                                          case e => hookDataP.failure(e); throw e
                                        }
                                    // preserve content-type (it may have been set with a different body writeable - e.g. play.api.libs.ws.WSBodyWritables.bodyWritableOf_Multipart)
                                    req2.header("Content-Type") match {
                                      case Some(contentType) => replaceHeaderOnRequest(req2.withBody(src2), "Content-Type" -> contentType)
                                      case _                 => req2.withBody(src2)
                                    }
      }
    }.withHookData(hookDataP.future)
  }

  // -- Execution --

  override def execute[A](
    transformResponse: (WSRequest, Future[HttpResponse]) => Future[A]
  )(implicit
    ec: ExecutionContext
  ): Future[A] =
    executor.execute(request, hookDataF, isStream = false)(transformResponse)

  override def stream[A](
    transformResponse: (WSRequest, Future[HttpResponse]) => Future[A]
  )(implicit
    ec: ExecutionContext
  ): Future[A] =
    executor.execute(request, hookDataF, isStream = true)(transformResponse)
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

  private val maxBodyLength = config.get[Int]("http-verbs.auditing.maxBodyLength")

  def execute[A](
    request     : WSRequest,
    optHookDataF: Option[Future[Option[HookData]]],
    isStream    : Boolean
  )(
    transformResponse: (WSRequest, Future[HttpResponse]) => Future[A]
  )(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[A] = {
    val hookDataF =
      optHookDataF match {
        case None if request.body != EmptyBody =>
          sys.error(s"There is no audit data available. Please ensure you call `withBody` on the RequestBuilder rather than `transform(_.withBody)`")
        case None    => Future.successful(None)
        case Some(f) => f
      }

    val startAge  = System.nanoTime() - hc.age
    val responseF =
      retryOnSslEngineClosed(request.method, request.url)(
        // we do the execution since if clients are responsable for it (e.g. a callback), they may further modify the request outside of auditing etc.
        if (isStream) request.stream() else request.execute()
      )

    val (httpResponseF, auditResponseF) = toHttpResponse(isStream, request, responseF)
    executeHooks(isStream, request, hookDataF, auditResponseF)
    httpResponseF.onComplete(logResult(hc, request.method, request.uri.toString, startAge))
    // we don't delegate the response conversion to the client
    // (i.e. return Future[WSResponse] to be handled with Future.transform/transformWith(...))
    // since the transform functions require access to the request (method and url)
    transformResponse(request, httpResponseF)
  }

  // unfortunate return type - first HttpResponse is the full response, the second HttpResponse is truncated for auditing...
  private def toHttpResponse(
    isStream : Boolean,
    request  : WSRequest,
    responseF: Future[WSResponse]
  )(implicit ec: ExecutionContext
  ): (Future[HttpResponse], Future[HttpResponse]) = {
    val auditResponseF = Promise[HttpResponse]()
    val loggingContext = s"outgoing ${request.method} ${request.url} response"
    val httpResponseF =
      for {
        response <- responseF
      } yield
        if (isStream) {
          val source =
            response.bodyAsSource
              .alsoTo(
                BodyCaptor.sink(
                  loggingContext   = loggingContext,
                  maxBodyLength    = maxBodyLength,
                  withCapturedBody = body =>
                                       auditResponseF.success(
                                         HttpResponse(
                                           status  = response.status,
                                           body    = body.decodeString("UTF-8"),
                                           headers = response.headers.mapValues(_.toSeq).toMap
                                         )
                                       )
                )
              )
              .recover {
                case e => auditResponseF.failure(e); throw e
              }
          HttpResponse(
            status       = response.status,
            bodyAsSource = source,
            headers      = response.headers.mapValues(_.toSeq).toMap
          )
        } else {
          auditResponseF.success(
            HttpResponse(
              status  = response.status,
              body    = BodyCaptor.bodyUpto(response.body, maxBodyLength, loggingContext),
              headers = response.headers.mapValues(_.toSeq).toMap
            )
          )
          HttpResponse(
            status  = response.status,
            body    = response.body,
            headers = response.headers.mapValues(_.toSeq).toMap
          )
        }
    (httpResponseF, auditResponseF.future)
  }

  private def executeHooks(
    isStream        : Boolean,
    request         : WSRequest,
    hookDataF       : Future[Option[HookData]],
    auditedResponseF: Future[HttpResponse] // play-auditing expects the body to be a String
  )(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Unit = {
    def denormalise(hdrs: Map[String, Seq[String]]): Seq[(String, String)] =
      hdrs.toList.flatMap { case (k, vs) => vs.map(k -> _) }

    def executeHooksWithHookData(hookData: Option[HookData]) =
      hooks.foreach(
        _.apply(
          verb      = request.method,
          url       = new URL(request.url),
          headers   = denormalise(request.headers),
          body      = hookData,
          responseF = auditedResponseF
        )
      )

    hookDataF.onComplete {
      case Success(hookData) => executeHooksWithHookData(hookData)
      case Failure(e)        => // this is unlikely, but we want best attempt at auditing
                                executeHooksWithHookData(None)
    }
  }
}
