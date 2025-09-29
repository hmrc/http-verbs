import sbt._

object LibDependencies {

  val play30Version     = "3.0.9"

  // Dependencies for http-verbs-common and http-verbs-play-xxx modules
  def coreCompileCommon(scalaVersion: String) = Seq(
    "uk.gov.hmrc"                 %% "mdc"              % "0.2.0",
    "com.typesafe"                %  "config"           % "1.4.5",
    "com.softwaremill.sttp.model" %% "core"             % "1.7.16",
    "dev.zio"                     %% "izumi-reflect"    % "3.0.6"
  )

  val coreCompilePlay30 = Seq(
    "org.playframework" %% "play-json"   % "3.0.5", // version provided by play30Version
    "org.slf4j"         %  "slf4j-api"   % "2.0.17",
    "org.playframework" %% "play-ahc-ws" % play30Version
  )

  def coreTestCommon = Seq(
    "org.scalatest"          %% "scalatest"       % "3.2.19"        % Test,
    "org.scalatestplus"      %% "scalacheck-1-17" % "3.2.18.0"      % Test,
    "com.vladsch.flexmark"   %  "flexmark-all"    % "0.64.8"        % Test,
    "org.scalatestplus"      %% "mockito-4-11"    % "3.2.18.0"      % Test
  )

  val coreTestPlay30 = Seq(
    "org.playframework"      %% "play-test"       % play30Version   % Test,
    "ch.qos.logback"         %  "logback-classic" % "1.5.18"        % Test, // should already provided by play-test, why does it fail without it?
    "com.github.tomakehurst" %  "wiremock"        % "3.0.1"         % Test,
    "org.slf4j"              %  "slf4j-simple"    % "2.0.17"        % Test
  )

  val testCompilePlay30 = Seq(
    "org.scalatest"          %% "scalatest"       % "3.2.19",       // version provided transitively is chosen for compatibility with scalatestplus-play
    "com.github.tomakehurst" %  "wiremock"        % "3.0.1",        // last version with jackson dependencies compatible with play
    "com.vladsch.flexmark"   %  "flexmark-all"    % "0.64.8"        % Test
  )
}
