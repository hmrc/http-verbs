import sbt._

object AppDependencies {

  val play26Version = "2.6.25"
  val play27Version = "2.7.9"
  val play28Version = "2.8.7"

  // Dependencies for http-verbs-common and http-verbs-play-xxx modules
  val coreCompileCommon = Seq(
    "com.typesafe"                %  "config"           % "1.4.1",
    "org.slf4j"                   %  "slf4j-api"        % "1.7.30",
    // empty http-core added to force eviction
    // as classes from this lib have been inlined in http-verbs
    "uk.gov.hmrc"                 %% "http-core"        % "2.5.0",
    // force dependencies due to security flaws found in jackson-databind < 2.9.x using XRay
    "com.fasterxml.jackson.core"  %  "jackson-core"     % "2.10.3",
    "com.fasterxml.jackson.core"  %  "jackson-databind" % "2.10.3",
    "com.softwaremill.sttp.model" %% "core"             % "1.2.2"
  )

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
    "org.scalatestplus"      %% "scalatestplus-mockito"    % "1.0.0-M2"    % Test,
    "org.scalatestplus"      %% "scalatestplus-scalacheck" % "3.1.0.0-RC2" % Test,
    "org.scalacheck"         %% "scalacheck"               % "1.15.2"      % Test,
    "com.github.tomakehurst" %  "wiremock"                 % "1.58"        % Test,
    "ch.qos.logback"         %  "logback-classic"          % "1.2.3"       % Test,
    "ch.qos.logback"         %  "logback-core"             % "1.2.3"       % Test,
    "org.mockito"            %  "mockito-all"              % "1.10.19"     % Test,
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

  // Dependencies for http-verbs-test modules
  val testCompileCommon = Seq(
    "org.scalatest"          %% "scalatest"    % "3.2.3",
    "com.github.tomakehurst" %  "wiremock"     % "1.58",
    "com.vladsch.flexmark"   %  "flexmark-all" % "0.35.10"
  )

  val testCompilePlay26 = Seq(
    "com.typesafe.play" %% "play-ws"   % play26Version,
    "com.typesafe.play" %% "play-json" % "2.6.14"
  )

  val testCompilePlay27 = Seq(
    "com.typesafe.play" %% "play-ws"   % play27Version,
    "com.typesafe.play" %% "play-json" % "2.7.4"
  )

  val testCompilePlay28 = Seq(
    "com.typesafe.play" %% "play-ws"   % play28Version,
    "com.typesafe.play" %% "play-json" % "2.8.1"
  )
}
