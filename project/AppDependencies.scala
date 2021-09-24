import sbt._

object AppDependencies {

  val play26Version = "2.6.25"
  val play27Version = "2.7.9"
  val play28Version = "2.8.8"

  // Dependencies for http-verbs-common and http-verbs-play-xxx modules
  def coreCompileCommon(scalaVersion: String) = Seq(
    "com.typesafe"                %  "config"           % "1.4.1",
    "org.slf4j"                   %  "slf4j-api"        % "1.7.30",
    // force dependencies due to security flaws found in jackson-databind < 2.9.x using XRay
    "com.fasterxml.jackson.core"  %  "jackson-core"     % "2.10.3",
    "com.fasterxml.jackson.core"  %  "jackson-databind" % "2.10.3",
    "com.softwaremill.sttp.model" %% "core"             % "1.2.2"
  ) ++
    (CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, n)) if n >= 13 => Seq.empty
      case _ => // empty http-core added to force eviction
                // as classes from this lib have been inlined in http-verbs
                Seq("uk.gov.hmrc" %% "http-core" % "2.5.0")
    })

  val coreCompilePlay26 = Seq(
    "com.typesafe.play" %% "play-json"   % "2.6.14",
    "com.typesafe.play" %% "play-ahc-ws" % "2.6.25"
  )

  val coreCompilePlay27 = Seq(
    "com.typesafe.play" %% "play-json"   % "2.7.4",
    "com.typesafe.play" %% "play-ahc-ws" % play27Version
  )

  val coreCompilePlay28 = Seq(
    "com.typesafe.play" %% "play-json"   % "2.8.1",
    "com.typesafe.play" %% "play-ahc-ws" % play28Version
  )

  val coreTestCommon = Seq(
    "commons-codec"          %  "commons-codec"            % "1.15"        % Test,
    "org.scalatest"          %% "scalatest"                % "3.2.3"       % Test,
    "org.scalatestplus"      %% "scalatestplus-scalacheck" % "3.1.0.0-RC2" % Test,
    "org.scalacheck"         %% "scalacheck"               % "1.15.2"      % Test,
    "org.mockito"            %% "mockito-scala-scalatest"  % "1.16.46"     % Test,
    "com.github.tomakehurst" %  "wiremock-jre8"            % "2.26.3"      % Test,
    "ch.qos.logback"         %  "logback-classic"          % "1.2.3"       % Test,
    "ch.qos.logback"         %  "logback-core"             % "1.2.3"       % Test,
    "org.webbitserver"       %  "webbit"                   % "0.4.15"      % Test,
    "com.vladsch.flexmark"   %  "flexmark-all"             % "0.35.10"     % Test,
  )

  val coreTestPlay26 = Seq(
    "com.typesafe.play" %% "play-test" % play26Version % Test
  )

  val coreTestPlay27 = Seq(
    "com.typesafe.play" %% "play-test" % play27Version % Test
  )

  val coreTestPlay28 = Seq(
    "com.typesafe.play" %% "play-test" % play28Version % Test
  )

  private def scalaTestVerson(playVersion: String): String =
    if      (playVersion == play26Version) "3.0.8" // scalatestplus-play "3.1.3"
    else if (playVersion == play27Version) "3.0.8" // scalatestplus-play "4.0.3"
    else                                   "3.1.1" // scalatestplus-play "5.1.0"

  // Dependencies for http-verbs-test modules
  def testCompileCommon(playVersion: String) = Seq(
    "org.scalatest"          %% "scalatest"     % scalaTestVerson(playVersion), // version provided transitively is chosen for compatibility with scalatestplus-play
    "com.github.tomakehurst" %  "wiremock-jre8" % "2.26.3", // last version with jackson dependencies compatible with play
    "org.scalatest"          %% "scalatest"     % "3.2.3"   % Test,
    "com.vladsch.flexmark"   %  "flexmark-all"  % "0.35.10" % Test,
    "org.slf4j"              %  "slf4j-simple"  % "1.7.30"  % Test
  )

  val testCompilePlay26 =
    testCompileCommon(play26Version) ++ Seq(
      "com.typesafe.play" %% "play-ws"   % play26Version,
      "com.typesafe.play" %% "play-json" % "2.6.14"
    )

  val testCompilePlay27 =
    testCompileCommon(play27Version) ++ Seq(
      "com.typesafe.play" %% "play-ws"   % play27Version,
      "com.typesafe.play" %% "play-json" % "2.7.4"
    )

  val testCompilePlay28 =
    testCompileCommon(play28Version) ++ Seq(
      "com.typesafe.play" %% "play-ws"   % play28Version,
      "com.typesafe.play" %% "play-json" % "2.8.1"
    )
}
