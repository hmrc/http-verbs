import sbt._

object LibDependencies {

  val play28Version     = "2.8.20"
  val play29Version     = "2.9.0"

  // Dependencies for http-verbs-common and http-verbs-play-xxx modules
  def coreCompileCommon(scalaVersion: String) = Seq(
    "com.typesafe"                %  "config"           % "1.4.3",
    "com.softwaremill.sttp.model" %% "core"             % "1.7.2"
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
    "com.typesafe.play" %% "play-json"   % "2.10.2", // version provided by play29Version
    "org.slf4j"         %  "slf4j-api"   % "2.0.9",
    "com.typesafe.play" %% "play-ahc-ws" % play29Version
  )

  val coreTestCommon = Seq(
    "org.scalatest"          %% "scalatest"                % "3.2.17"      % Test,
    "org.scalatestplus"      %% "scalacheck-1-17"          % "3.2.17.0"    % Test,
    "org.mockito"            %% "mockito-scala-scalatest"  % "1.17.14"     % Test,
    "com.vladsch.flexmark"   %  "flexmark-all"             % "0.64.8"      % Test
  )

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
}
