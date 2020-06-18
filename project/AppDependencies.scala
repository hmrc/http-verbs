import sbt._

object AppDependencies {

  val play25Version = "2.5.19"
  val play26Version = "2.6.20"
  val play27Version = "2.7.4"

  // Dependencies for http-verbs-common and http-verbs-play-xxx modules
  val coreCompileCommon = Seq(
    "com.typesafe" % "config"    % "1.3.3",
    "org.slf4j"    % "slf4j-api" % "1.7.25",
    // empty http-core added to force eviction
    // as classes from this lib have been inlined in http-verbs
    "uk.gov.hmrc" %% "http-core"          % "2.2.0",
    // force dependencies due to security flaws found in jackson-databind < 2.9.x using XRay
    "com.fasterxml.jackson.core"     % "jackson-core"            % "2.10.3",
    "com.fasterxml.jackson.core"     % "jackson-databind"        % "2.10.3"
  )

  val coreCompilePlay25 = Seq(
    "com.typesafe.play" %% "play-json" % play25Version,
    "com.typesafe.play" %% "play-ws"   % play25Version,
    // force dependencies due to security flaws found in xercesImpl 2.11.0
    // only applies to play 2.5 since it was removed from play 2.6
    // https://github.com/playframework/playframework/blob/master/documentation/manual/releases/release26/migration26/Migration26.md#xercesimpl-removal
    "xerces" % "xercesImpl" % "2.12.0"
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
    "com.vladsch.flexmark"   % "flexmark-all"    % "0.35.10" % Test
  )

  val coreTestPlay25 = Seq(
    "com.typesafe.play" %% "play-test"   % play25Version % Test,
    "com.typesafe.play" %% "play-specs2" % play25Version % Test
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

  val testCompilePlay25 = Seq(
    "com.typesafe.play"              %% "play-ws"                % play25Version,
    "com.typesafe.play"              %% "play-json"              % play25Version,
    // force dependencies due to security flaws found in jackson-databind < 2.9.x using XRay
    "com.fasterxml.jackson.core"     % "jackson-core"            % "2.9.7",
    "com.fasterxml.jackson.core"     % "jackson-databind"        % "2.9.7",
    "com.fasterxml.jackson.core"     % "jackson-annotations"     % "2.9.7",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8"   % "2.9.7",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % "2.9.7",
    // force dependencies due to security flaws found in xercesImpl 2.11.0
    // only applies to play 2.5 since it was removed from play 2.6
    // https://github.com/playframework/playframework/blob/master/documentation/manual/releases/release26/migration26/Migration26.md#xercesimpl-removal
    "xerces"                        % "xercesImpl" % "2.12.0"
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
