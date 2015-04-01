/*
 * Copyright 2015 HM Revenue & Customs
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
import sbt.Keys._
import uk.gov.hmrc.PublishingSettings._

object HmrcBuild extends Build {

  import uk.gov.hmrc._
  import DefaultBuildSettings._
  import uk.gov.hmrc.{SbtBuildInfo, ShellPrompt}
  import uk.gov.hmrc.PublishingSettings._

  val appName = "http-verbs"
  val appVersion = "1.4.1"

  lazy val microservice = Project(appName, file("."))
    .settings(version := appVersion)
    .settings(scalaSettings : _*)
    .settings(defaultSettings() : _*)
    .settings(
      targetJvm := "jvm-1.7",
      shellPrompt := ShellPrompt(appVersion),
      libraryDependencies ++= AppDependencies(),
      crossScalaVersions := Seq("2.11.6"),
      resolvers := Seq(
        Opts.resolver.sonatypeReleases,
        Opts.resolver.sonatypeSnapshots,
        "typesafe-releases" at "http://repo.typesafe.com/typesafe/releases/",
        "typesafe-snapshots" at "http://repo.typesafe.com/typesafe/snapshots/"
      )
    )
    .settings(publishAllArtefacts: _*)
    .settings(SbtBuildInfo(): _*)
    .settings(POMMetadata(): _*)
    .settings(Headers(): _ *)
}

private object AppDependencies {

  import play.PlayImport._
  import play.core.PlayVersion

  val compile = Seq(
    "com.typesafe.play" %% "play" % PlayVersion.current,
    ws,
    "net.ceedubs" %% "ficus" % "1.1.1",
    "uk.gov.hmrc" %% "time" % "1.1.0",
    "uk.gov.hmrc" %% "http-exceptions" % "0.3.0"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test: Seq[ModuleID] = ???
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "commons-codec" % "commons-codec" % "1.7" % scope,
        "org.scalatest" %% "scalatest" % "2.2.4" % scope,
        "org.scalacheck" %% "scalacheck" % "1.12.2" % scope,
        "org.pegdown" % "pegdown" % "1.4.2" % scope,
        "com.github.tomakehurst" % "wiremock" % "1.52" % scope
      )
    }.test
  }

  def apply() = compile ++ Test()
}

object POMMetadata {

  def apply() = {
    pomExtra :=
      <url>https://www.gov.uk/government/organisations/hm-revenue-customs</url>
        <licenses>
          <license>
            <name>Apache 2</name>
            <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
          </license>
        </licenses>
        <scm>
          <connection>scm:git@github.com:hmrc/http-verbs.git</connection>
          <developerConnection>scm:git@github.com:hmrc/http-verbs.git</developerConnection>
          <url>git@github.com:hmrc/http-verbs.git</url>
        </scm>
        <developers>
          <developer>
            <id>duncancrawford</id>
            <name>Duncan Crawford</name>
            <url>http://www.equalexperts.com</url>
          </developer>
          <developer>
            <id>xnejp03</id>
            <name>Petr Nejedly</name>
            <url>http://www.equalexperts.com</url>
          </developer>
          <developer>
            <id>rama-nallamilli</id>
            <name>Rama Nallamilli</name>
            <url>http://www.equalexperts.com</url>
          </developer>
        </developers>
  }
}

object Headers {
  import de.heikoseeberger.sbtheader.SbtHeader.autoImport._
  def apply() = Seq(
    headers := Map(
      "scala" ->(
        HeaderPattern.cStyleBlockComment,
        """|/*
          | * Copyright 2015 HM Revenue & Customs
          | *
          | * Licensed under the Apache License, Version 2.0 (the "License");
          | * you may not use this file except in compliance with the License.
          | * You may obtain a copy of the License at
          | *
          | *   http://www.apache.org/licenses/LICENSE-2.0
          | *
          | * Unless required by applicable law or agreed to in writing, software
          | * distributed under the License is distributed on an "AS IS" BASIS,
          | * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
          | * See the License for the specific language governing permissions and
          | * limitations under the License.
          | */
          |
          |""".stripMargin
        )
    ),
    (compile in Compile) <<= (compile in Compile) dependsOn (createHeaders in Compile),
    (compile in Test) <<= (compile in Test) dependsOn (createHeaders in Test)
  )
}

