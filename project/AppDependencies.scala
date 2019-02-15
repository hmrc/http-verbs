/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import sbt._

object AppDependencies {

  val play25Version = "2.5.19"
  val play26Version = "2.6.20"

  val compile: Seq[ModuleID] =
    PlayCrossCompilation.dependencies(
      shared = Seq(
        "com.typesafe" % "config"    % "1.3.3",
        "org.slf4j"    % "slf4j-api" % "1.7.25",
        // empty http-core added to force eviction 
        // as classes from this lib have been inlined in http-verbs
        "uk.gov.hmrc" %% "http-core"          % "2.2.0",
        // force dependencies due to security flaws found in jackson-databind < 2.9.x using XRay
        "com.fasterxml.jackson.core"     % "jackson-core"            % "2.9.8",
        "com.fasterxml.jackson.core"     % "jackson-databind"        % "2.9.8",
        "com.fasterxml.jackson.core"     % "jackson-annotations"     % "2.9.8",
        "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8"   % "2.9.8",
        "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % "2.9.8"
      ),
      play25 = Seq(
        "com.typesafe.play" %% "play-json" % play25Version,
        "com.typesafe.play" %% "play-ws"   % play25Version,
        // force dependencies due to security flaws found in xercesImpl 2.11.0
        // only applies to play 2.5 since it was removed from play 2.6 
        // https://github.com/playframework/playframework/blob/master/documentation/manual/releases/release26/migration26/Migration26.md#xercesimpl-removal
        "xerces" % "xercesImpl" % "2.12.0",

        // empty http-verbs-play-25 added to force eviction
        // as classes from this lib have been inlined in http-verbs
        "uk.gov.hmrc" %% "http-verbs-play-25" % "1.0.0"
      ),
      play26 = Seq(
        "com.typesafe.play" %% "play-json" % "2.6.9",
        "com.typesafe.play" %% "play-ws"   % play26Version
      )
    )

  val test: Seq[ModuleID] = PlayCrossCompilation.dependencies(
    shared = Seq(
      "commons-codec"          % "commons-codec"   % "1.12"     % Test,
      "org.scalatest"          %% "scalatest"      % "3.0.5"   % Test,
      "org.scalacheck"         %% "scalacheck"     % "1.14.0"  % Test,
      "org.pegdown"            % "pegdown"         % "1.6.0"   % Test,
      "com.github.tomakehurst" % "wiremock"        % "1.58"    % Test,
      "ch.qos.logback"         % "logback-classic" % "1.2.3"   % Test,
      "ch.qos.logback"         % "logback-core"    % "1.2.3"   % Test,
      "org.mockito"            % "mockito-all"     % "1.10.19" % Test,
      "org.webbitserver"       % "webbit"          % "0.4.15"  % Test
    ),
    play25 = Seq(
      "com.typesafe.play" %% "play-test"   % play25Version % Test,
      "com.typesafe.play" %% "play-specs2" % play25Version % Test
    ),
    play26 = Seq(
      "com.typesafe.play" %% "play-test"   % play26Version % Test,
      "com.typesafe.play" %% "play-specs2" % play26Version % Test,
      "com.typesafe.play" %% "play-ahc-ws" % play26Version % Test
    )
  )

}
