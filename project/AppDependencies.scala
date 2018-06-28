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

  val compile = Seq(
    "com.typesafe.play" %% "play-json" % "2.5.16",
    "uk.gov.hmrc"       %% "http-core" % "0.9.0",
    "org.slf4j"         % "slf4j-api"  % "1.7.25"
  )

  val test = Seq(
    "commons-codec"          % "commons-codec"   % "1.7"     % Test,
    "org.scalatest"          %% "scalatest"      % "3.0.3"   % Test,
    "org.scalacheck"         %% "scalacheck"     % "1.13.4"  % Test,
    "org.pegdown"            % "pegdown"         % "1.6.0"   % Test,
    "com.github.tomakehurst" % "wiremock"        % "1.52"    % Test,
    "ch.qos.logback"         % "logback-classic" % "1.1.7"   % Test,
    "org.mockito"            % "mockito-all"     % "1.10.19" % Test
  )

}
