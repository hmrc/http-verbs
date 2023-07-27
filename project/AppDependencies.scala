import sbt._

object AppDependencies {

  val play28Version     = "2.8.20"

  // Dependencies for http-verbs-common and http-verbs-play-xxx modules
  def coreCompileCommon(scalaVersion: String) = Seq(
    "com.typesafe"                %  "config"           % "1.4.1",
    "org.slf4j"                   %  "slf4j-api"        % "1.7.30",
    "com.softwaremill.sttp.model" %% "core"             % "1.2.2"
  ) ++
    (CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, n)) if n >= 13 => Seq.empty
      case _ => // empty http-core added to force eviction
                // as classes from this lib have been inlined in http-verbs
                Seq("uk.gov.hmrc" %% "http-core" % "2.5.0")
    })

  val coreCompilePlay28 = Seq(
    "com.typesafe.play" %% "play-json"   % "2.8.2", // version provided by play28Version
    "com.typesafe.play" %% "play-ahc-ws" % play28Version
  )

  val coreTestCommon = Seq(
    "commons-codec"          %  "commons-codec"            % "1.15"        % Test,
    "org.scalatest"          %% "scalatest"                % "3.2.3"       % Test,
    "org.scalatestplus"      %% "scalatestplus-scalacheck" % "3.1.0.0-RC2" % Test,
    "org.scalacheck"         %% "scalacheck"               % "1.15.2"      % Test,
    "org.mockito"            %% "mockito-scala-scalatest"  % "1.16.49"     % Test,
    "com.github.tomakehurst" %  "wiremock-jre8"            % "2.26.3"      % Test,
    "ch.qos.logback"         %  "logback-classic"          % "1.2.3"       % Test,
    "ch.qos.logback"         %  "logback-core"             % "1.2.3"       % Test,
    "org.webbitserver"       %  "webbit"                   % "0.4.15"      % Test,
    "com.vladsch.flexmark"   %  "flexmark-all"             % "0.35.10"     % Test,
  )

  val coreTestPlay28 = Seq(
    "com.typesafe.play" %% "play-test" % play28Version % Test
  )

  // Dependencies for http-verbs-test-play-xxx modules
  val testCompilePlay28 = Seq(
    "org.scalatest"          %% "scalatest"     % "3.1.1", // version provided transitively is chosen for compatibility with scalatestplus-play
    "com.github.tomakehurst" %  "wiremock-jre8" % "2.26.3", // last version with jackson dependencies compatible with play
    "org.scalatest"          %% "scalatest"     % "3.2.3"   % Test,
    "com.vladsch.flexmark"   %  "flexmark-all"  % "0.35.10" % Test,
    "org.slf4j"              %  "slf4j-simple"  % "1.7.30"  % Test
    )
}
