import sbt._

object AppDependencies {

  sealed trait PlayVersion {
    def version: String
    def name: String
  }

  case object Play25 extends PlayVersion {
    override def version: String = "2.5.19"
    override def name: String = "play-25"
  }

  case object Play26 extends PlayVersion {
    override def version: String = "2.6.20"
    override def name: String = "play-26"
  }

  case object Play27 extends PlayVersion {
    override def version: String = "2.7.4"
    override def name: String = "play-27"
  }

  def compileCommon(playVersion: PlayVersion): Seq[ModuleID] = {

    val playDeps = playVersion match {
      case Play25 => compilePlay25
      case Play26 => compilePlay26
      case Play27 => compilePlay27
    }

    Seq(
      "com.typesafe"               % "config"           % "1.3.3",
      "org.slf4j"                  % "slf4j-api"        % "1.7.25",
      // empty http-core added to force eviction
      // as classes from this lib have been inlined in http-verbs
      "uk.gov.hmrc"                %% "http-core"       % "2.2.0",
      // force dependencies due to security flaws found in jackson-databind < 2.9.x using XRay
      "com.fasterxml.jackson.core" % "jackson-core"     % "2.10.3",
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.10.3"
    ) ++ playDeps
  }

  val compilePlay25 = Seq(
    "com.typesafe.play" %% "play-json" % Play25.version,
    "com.typesafe.play" %% "play-ws"   % Play25.version,
    // force dependencies due to security flaws found in xercesImpl 2.11.0
    // only applies to play 2.5 since it was removed from play 2.6
    // https://github.com/playframework/playframework/blob/master/documentation/manual/releases/release26/migration26/Migration26.md#xercesimpl-removal
    "xerces"            % "xercesImpl" % "2.12.0"
  )

  val compilePlay26 = Seq(
    "com.typesafe.play" %% "play-json"   % "2.6.9",
    "com.typesafe.play" %% "play-ahc-ws" % Play26.version
  )

  val compilePlay27 = Seq(
    "com.typesafe.play" %% "play-json"   % Play27.version,
    "com.typesafe.play" %% "play-ahc-ws" % Play27.version
  )

  def testCommon(playVersion: PlayVersion): Seq[ModuleID] = {
    val playDeps = playVersion match {
      case Play25 => testPlay25
      case Play26 => testPlay26
      case Play27 => testPlay27
    }

    Seq(
      "commons-codec"          % "commons-codec"             % "1.12"        % Test,
      "org.scalatest"          %% "scalatest"                % "3.1.1"       % Test,
      "org.scalatestplus"      %% "scalatestplus-mockito"    % "1.0.0-M2"    % Test,
      "org.scalatestplus"      %% "scalatestplus-scalacheck" % "3.1.0.0-RC2" % Test,
      "org.scalacheck"         %% "scalacheck"               % "1.14.0"      % Test,
      "com.github.tomakehurst" %  "wiremock"                 % "1.58"        % Test,
      "ch.qos.logback"         %  "logback-classic"          % "1.2.3"       % Test,
      "ch.qos.logback"         %  "logback-core"             % "1.2.3"       % Test,
      "org.mockito"            %  "mockito-all"              % "1.10.19"     % Test,
      "org.webbitserver"       %  "webbit"                   % "0.4.15"      % Test,
      "com.vladsch.flexmark"   %  "flexmark-all"             % "0.35.10"     % Test
    ) ++ playDeps
  }

  val testPlay25 = Seq(
    "com.typesafe.play" %% "play-test"   % Play25.version % Test,
    "com.typesafe.play" %% "play-specs2" % Play25.version % Test
  )

  val testPlay26 = Seq(
    "com.typesafe.play" %% "play-test"   % Play26.version % Test
  )

  val testPlay27 = Seq(
    "com.typesafe.play" %% "play-test"   % Play27.version % Test
  )
}
