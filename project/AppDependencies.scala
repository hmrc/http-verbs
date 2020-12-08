import sbt._

object AppDependencies {

  val play26Version = "2.6.20"
  val play27Version = "2.7.4"

  // Dependencies for http-verbs-common and http-verbs-play-xxx modules
  val coreCompileCommon = Seq(
    "com.typesafe" % "config"    % "1.3.3",
    "org.slf4j"    % "slf4j-api" % "1.7.25",
    // empty http-core added to force eviction
    // as classes from this lib have been inlined in http-verbs
    "uk.gov.hmrc" %% "http-core"          % "2.4.0",
    // force dependencies due to security flaws found in jackson-databind < 2.9.x using XRay
    "com.fasterxml.jackson.core"     % "jackson-core"            % "2.10.3",
    "com.fasterxml.jackson.core"     % "jackson-databind"        % "2.10.3"
  )

  val coreCompilePlay26 = Seq(
    "com.typesafe.play" %% "play-json" % "2.6.9",
    "com.typesafe.play" %% "play-ahc-ws" % play26Version
  )

  val coreCompilePlay27 = Seq(
    "com.typesafe.play" %% "play-json" % play27Version,
    "com.typesafe.play" %% "play-ahc-ws" % play27Version
  )

  val coreTestCommon = Seq(
    "commons-codec"          % "commons-codec"   % "1.12"    % Test,
    "org.scalatest"          %% "scalatest"      % "3.1.1"   % Test,
    "org.scalatestplus"      %% "scalatestplus-mockito" % "1.0.0-M2" % Test,
    "org.scalatestplus"      %% "scalatestplus-scalacheck"  % "3.1.0.0-RC2" % Test,
    "org.scalacheck"         %% "scalacheck"     % "1.14.0"  % Test,
    "com.github.tomakehurst" % "wiremock"        % "1.58"    % Test,
    "ch.qos.logback"         % "logback-classic" % "1.2.3"   % Test,
    "ch.qos.logback"         % "logback-core"    % "1.2.3"   % Test,
    "org.mockito"            % "mockito-all"     % "1.10.19" % Test,
    "org.webbitserver"       % "webbit"          % "0.4.15"  % Test,
    "com.vladsch.flexmark"   % "flexmark-all"    % "0.35.10" % Test,
  )

  val coreTestPlay26 = Seq(
    "com.typesafe.play" %% "play-test"   % play26Version % Test
  )

  val coreTestPlay27 = Seq(
    "com.typesafe.play" %% "play-test"   % play27Version % Test
  )

  // Dependencies for http-verbs-test modules
  val testCompileCommon = Seq(
    "org.scalatest"          %% "scalatest"      % "3.1.1",
    "com.vladsch.flexmark"   % "flexmark-all"    % "0.35.10"
  )

  val testCompilePlay26 = Seq(
    "com.typesafe.play"              %% "play-ws"                % play26Version,
    "com.typesafe.play"              %% "play-json"              % "2.6.13"
  )

  val testCompilePlay27 = Seq(
    "com.typesafe.play"              %% "play-ws"                % play27Version,
    "com.typesafe.play"              %% "play-json"              % play27Version
  )
}
