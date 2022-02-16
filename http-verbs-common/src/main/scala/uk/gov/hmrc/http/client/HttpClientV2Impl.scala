/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.http.client

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.typesafe.config.Config
import play.api.Configuration
import play.api.libs.ws.{BodyWritable, EmptyBody, InMemoryBody, SourceBody, WSClient, WSProxyServer, WSRequest, WSResponse}
import play.core.parsers.FormUrlEncodedParser
import uk.gov.hmrc.http.{BadGatewayException, BuildInfo, GatewayTimeoutException, HeaderCarrier, HttpReads, HttpResponse, Retries}
import uk.gov.hmrc.play.http.BodyCaptor
import uk.gov.hmrc.play.http.ws.WSProxyConfiguration
import uk.gov.hmrc.http.hooks.{HookData, HttpHook}
import uk.gov.hmrc.http.logging.ConnectionTracing

import java.net.{ConnectException, URL}
import java.util.concurrent.TimeoutException
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.reflect.runtime.universe.{TypeTag, typeOf}
import scala.util.{Failure, Success}


trait Executor {
  def execute[A](
    request  : WSRequest,
    hookDataF: Option[Future[Option[HookData]]],
    isStream : Boolean,
    r        : HttpReads[A]
  )(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[A]
}

class HttpClientV2Impl(
  wsClient   : WSClient,
  actorSystem: ActorSystem,
  config     : Configuration,
  hooks      : Seq[HttpHook]
) extends HttpClientV2 {

  private lazy val optProxyServer =
    WSProxyConfiguration.buildWsProxyServer(config)

  private val hcConfig =
    HeaderCarrier.Config.fromConfig(config.underlying)

  protected val executor =
    new ExecutorImpl(actorSystem, config, hooks)

  private val clientVersionHeader =
    "Http-ClientV2-Version" -> BuildInfo.version

  override protected def mkRequestBuilder(
    url   : URL,
    method: String
  )(implicit
    hc: HeaderCarrier
  ): RequestBuilderImpl =
    new RequestBuilderImpl(
      config,
      optProxyServer,
      executor
    )(
      wsClient
        .url(url.toString)
        .withMethod(method)
        .withHttpHeaders(hc.headersForUrl(hcConfig)(url.toString) :+ clientVersionHeader : _*),
      None
    )
}


// is final since `transform` (and derived functions) return instances of RequestBuilderImpl, and any overrides would be lost.
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
                                      case _                                                     => val auditedBody = BodyCaptor.bodyUpto(bytes, maxBodyLength, loggingContext, isStream = false).decodeString("UTF-8")
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

  override def execute[A](implicit r: HttpReads[A], ec: ExecutionContext): Future[A] =
    executor.execute(request, hookDataF, isStream = false, r)

  override def stream[A](implicit r: StreamHttpReads[A], ec: ExecutionContext): Future[A] =
    executor.execute(request, hookDataF, isStream = true, r)
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

  final def execute[A](
    request     : WSRequest,
    optHookDataF: Option[Future[Option[HookData]]],
    isStream    : Boolean,
    httpReads   : HttpReads[A]
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
    mapErrors(request, httpResponseF)
      .map(httpReads.read(request.method, request.url, _))
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
        status   =  response.status
        headers  =  response.headers.mapValues(_.toSeq).toMap
      } yield {
        def httpResponse(body : Either[Source[ByteString, _], String]): HttpResponse =
          body match {
            case Left(src) => HttpResponse(
                                status       = response.status,
                                bodyAsSource = src,
                                headers      = response.headers.mapValues(_.toSeq).toMap
                              )
            case Right(str) => HttpResponse(
                                status       = response.status,
                                body         = str,
                                headers      = response.headers.mapValues(_.toSeq).toMap
                              )
          }
        if (isStream) {
          val source =
            response.bodyAsSource
              .alsoTo(
                BodyCaptor.sink(
                  loggingContext   = loggingContext,
                  maxBodyLength    = maxBodyLength,
                  withCapturedBody = body => auditResponseF.success(httpResponse(Right(body.decodeString("UTF-8"))))
                )
              )
              .recover {
                case e => auditResponseF.failure(e); throw e
              }
          httpResponse(Left(source))
        } else {
          auditResponseF.success(
            httpResponse(Right(
              BodyCaptor.bodyUpto(response.body, maxBodyLength, loggingContext, isStream = false)
            ))
          )
          httpResponse(Right(response.body))
        }
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

  protected def mapErrors(
    request  : WSRequest,
    responseF: Future[HttpResponse]
  )(implicit
    ec: ExecutionContext
  ): Future[HttpResponse] =
    responseF.recoverWith {
      case e: TimeoutException => Future.failed(new GatewayTimeoutException(s"${request.method} of '${request.url}' timed out with message '${e.getMessage}'"))
      case e: ConnectException => Future.failed(new BadGatewayException(s"${request.method} of '${request.url}' failed. Caused by: '${e.getMessage}'"))
    }
}