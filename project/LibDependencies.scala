import sbt._

object LibDependencies {

  val play29Version     = "2.9.6"
  val play30Version     = "3.0.8"

  // Dependencies for http-verbs-common and http-verbs-play-xxx modules
  def coreCompileCommon(scalaVersion: String) = Seq(
    "uk.gov.hmrc"                 %% "mdc"              % "0.2.0",
    "com.typesafe"                %  "config"           % "1.4.3",
    "com.softwaremill.sttp.model" %% "core"             % "1.7.16",
    "dev.zio"                     %% "izumi-reflect"    % "2.3.8"
  )

  val coreCompilePlay29 = Seq(
    "com.typesafe.play" %% "play-json"   % "2.10.6", // version provided by play29Version
    "org.slf4j"         %  "slf4j-api"   % "2.0.9",
    "com.typesafe.play" %% "play-ahc-ws" % play29Version
  )

  val coreCompilePlay30 = Seq(
    "org.playframework" %% "play-json"   % "3.0.5", // version provided by play30Version
    "org.slf4j"         %  "slf4j-api"   % "2.0.9",
    "org.playframework" %% "play-ahc-ws" % play30Version
  )

  def coreTestCommon = Seq(
    "org.scalatest"          %% "scalatest"       % "3.2.17"      % Test,
    "org.scalatestplus"      %% "scalacheck-1-17" % "3.2.17.0"    % Test,
    "com.vladsch.flexmark"   %  "flexmark-all"    % "0.64.8"      % Test,
    "org.scalatestplus"      %% "mockito-4-11"    % "3.2.17.0"    % Test
  )

  val coreTestPlay29 = Seq(
    "com.typesafe.play"      %% "play-test"       % play29Version  % Test,
    "ch.qos.logback"         %  "logback-classic" % "1.4.11"       % Test, // should already provided by play-test, why does it fail without it?
    "com.github.tomakehurst" %  "wiremock"        % "3.0.0-beta-7" % Test,
    "org.slf4j"              %  "slf4j-simple"    % "2.0.7"        % Test
  )

  val coreTestPlay30 = Seq(
    "org.playframework"      %% "play-test"       % play30Version  % Test,
    "ch.qos.logback"         %  "logback-classic" % "1.4.11"       % Test, // should already provided by play-test, why does it fail without it?
    "com.github.tomakehurst" %  "wiremock"        % "3.0.0-beta-7" % Test,
    "org.slf4j"              %  "slf4j-simple"    % "2.0.7"        % Test
  )

  val testCompilePlay29 = Seq(
    "org.scalatest"          %% "scalatest"     % "3.2.17",       // version provided transitively is chosen for compatibility with scalatestplus-play
    "com.github.tomakehurst" %  "wiremock"      % "3.0.0-beta-7", // last version with jackson dependencies compatible with play
    "com.vladsch.flexmark"   %  "flexmark-all"  % "0.64.8" % Test
  )

  val testCompilePlay30 = Seq(
    "org.scalatest"          %% "scalatest"     % "3.2.17",       // version provided transitively is chosen for compatibility with scalatestplus-play
    "com.github.tomakehurst" %  "wiremock"      % "3.0.0-beta-7", // last version with jackson dependencies compatible with play
    "com.vladsch.flexmark"   %  "flexmark-all"  % "0.64.8" % Test
  )
}
