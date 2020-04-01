# What are all the traits?

Let's look at making a GET request. http-verbs has this:

    trait HttpGet extends CoreGet
     with GetHttpTransport
      with HttpVerb
       with ConnectionTracing
        with HttpHooks
         with Retries

Which is just a big trait of other traits, plus it implements the method below from `CoreGet` to combine things together, such as tracing, retries, executing hooks, and some mapping of exceptions.

    trait CoreGet {
      def GET[A](
        url: String,
        queryParams: Seq[(String, String)],
        headers: Seq[(String, String)])(
          implicit rds: HttpReads[A],
          hc: HeaderCarrier,
          ec: ExecutionContext): Future[A]

      ... plus some more helper/overloaded methods that call the above one.
    }

The `GetHttpTransport` seems to actually define how to make a GET request, but has no implementation.

    trait GetHttpTransport {
      def doGet(
        url: String,
        headers: Seq[(String, String)] = Seq.empty)(
          implicit hc: HeaderCarrier,
          ec: ExecutionContext): Future[HttpResponse]
    }

So `HttpGet` is the trait that configures how to do all the MDTP things for GET. All except for actually making a request. That resides in the parallel set of traits, `WSGet` and `WSHttp`

    trait WSGet extends CoreGet with GetHttpTransport with WSRequest with WSExecute {

      override def doGet(
        url: String,
        headers: Seq[(String, String)])(
          implicit hc: HeaderCarrier,
          ec: ExecutionContext): Future[HttpResponse] =
        execute(buildRequest(url, headers), "GET")
          .map(new WSHttpResponse(_))
    }

So the WS traits implement the Core ones, but use WS itself...

... and then bootstrap has this, where everything comes together in a proper http client with all the methods, and a `WSClient` instance.

    trait HttpClient extends HttpGet with HttpPut with HttpPost with HttpDelete with HttpPatch

    @Singleton
    class DefaultHttpClient @Inject()(
      config: Configuration,
      val httpAuditing: HttpAuditing,
      override val wsClient: WSClient,
      override protected val actorSystem: ActorSystem)
        extends HttpClient
        with WSHttp {

      override lazy val configuration: Option[Config] = Option(config.underlying)

      override val hooks: Seq[HttpHook] = Seq(httpAuditing.AuditingHook)

    }

# Why is it like this?
Notably, bootstrap pulls in `play-auditing` to construct this client. Moving this class to http-verbs would mean changing that dependency too.

The [`http-verbs-example` project](https://github.com/hmrc/http-verbs-example) has a description of why this exists: decoupling business-layer from transport layer; presumably to aid in testing. Teams don't necessarily need a WS client if they rely on HttpGet for instance.