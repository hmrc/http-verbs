import sbt._

object LibDependencies {

  val play28Version     = "2.8.21"
  val play29Version     = "2.9.2"
  val play30Version     = "3.0.2"

  // Dependencies for http-verbs-common and http-verbs-play-xxx modules
  def coreCompileCommon(scalaVersion: String) = Seq(
    "com.typesafe"                %  "config"           % "1.4.3",
    "com.softwaremill.sttp.model" %% "core"             % "1.7.2",
    "dev.zio"                     %% "izumi-reflect"    % "2.3.8"
  ) ++
    (CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, 12)) => // empty http-core added to force eviction
                            // as classes from this lib have been inlined in http-verbs
                            Seq("uk.gov.hmrc" %% "http-core" % "2.5.0")
      case _             => Seq.empty
    })

  val coreCompilePlay28 = Seq(
    "com.typesafe.play" %% "play-json"   % "2.8.2", // version provided by play28Version
    "org.slf4j"         %  "slf4j-api"   % "1.7.30",
    "com.typesafe.play" %% "play-ahc-ws" % play28Version
  )

  val coreCompilePlay29 = Seq(
    "com.typesafe.play" %% "play-json"   % "2.10.4", // version provided by play29Version
    "org.slf4j"         %  "slf4j-api"   % "2.0.9",
    "com.typesafe.play" %% "play-ahc-ws" % play29Version
  )

  val coreCompilePlay30 = Seq(
    "org.playframework" %% "play-json"   % "3.0.2", // version provided by play30Version
    "org.slf4j"         %  "slf4j-api"   % "2.0.9",
    "org.playframework" %% "play-ahc-ws" % play30Version
  )

  def coreTestCommon = Seq(
    "org.scalatest"          %% "scalatest"                % "3.2.17"      % Test,
    "org.scalatestplus"      %% "scalacheck-1-17"          % "3.2.17.0"    % Test,
    "com.vladsch.flexmark"   %  "flexmark-all"             % "0.64.8"      % Test,
    // mockito-scala is not available for Scala 3 https://github.com/mockito/mockito-scala/issues/364
    // use java build + scalatestplus:mockito
    //"org.scalatestplus"      %% "mockito-3-4"              % "3.2.10.0"    % Test // recommended by play docs https://www.playframework.com/documentation/3.0.x/ScalaTestingWithScalaTest
    // or https://mvnrepository.com/artifact/eu.monniot/scala3mock
  ) ++
    (CrossVersion.partialVersion(scalaVersion) match {
      case Some((3, _)) => Seq(("org.mockito" %% "mockito-scala-scalatest" % "1.17.14" % Test).cross(CrossVersion.for3Use2_13).exclude("org.scalactic","scalactic_2.13"))
      case _            => Seq(("org.mockito" %% "mockito-scala-scalatest" % "1.17.14" % Test).cross(CrossVersion.for3Use2_13)) // the cross version is unnecessary here
    })

  val coreTestPlay28 = Seq(
    "com.typesafe.play"      %% "play-test"       % play28Version % Test,
    "ch.qos.logback"         %  "logback-classic" % "1.2.12"      % Test, // should already provided by play-test, why does it fail without it?
    "com.github.tomakehurst" %  "wiremock-jre8"   % "2.27.2"      % Test,
    "org.slf4j"              %  "slf4j-simple"    % "1.7.30"      % Test
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

  val testCompilePlay28 = Seq(
    "org.scalatest"          %% "scalatest"     % "3.1.1",  // version provided transitively is chosen for compatibility with scalatestplus-play
    "com.github.tomakehurst" %  "wiremock-jre8" % "2.27.2", // last version with jackson dependencies compatible with play
    "org.scalatest"          %% "scalatest"     % "3.2.17" % Test,
    "com.vladsch.flexmark"   %  "flexmark-all"  % "0.64.8" % Test
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
