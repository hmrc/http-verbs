import sbt._
import sbt.Keys._

object HmrcBuild extends Build {

  import uk.gov.hmrc._
  import DefaultBuildSettings._
  import uk.gov.hmrc.{SbtBuildInfo, ShellPrompt}
  import PublishingSettings._
  import NexusPublishing._
  import scala.util.Properties.envOrElse

  val appName = "http-verbs"
  val appVersion = envOrElse("HTTP_VERBS_VERSION", "999-SNAPSHOT")

  lazy val microservice = Project(appName, file("."))
    .settings(version := appVersion)
    .settings(scalaSettings: _*)
    .settings(defaultSettings(): _*)
    .settings(
      targetJvm := "jvm-1.7",
      shellPrompt := ShellPrompt(appVersion),
      libraryDependencies ++= AppDependencies(),
      Collaborators(),
      crossScalaVersions := Seq("2.11.5")
    )
    .settings(publishAllArtefacts : _*)
    .settings(nexusPublishingSettings : _*)
    .settings(SbtBuildInfo(): _*)
}

private object AppDependencies {

  import play.PlayImport._
  import play.core.PlayVersion

  val compile = Seq(
    "com.typesafe.play" %% "play" % PlayVersion.current,
    ws % "provided",
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
        "org.pegdown" % "pegdown" % "1.4.2" % scope,
        "com.github.tomakehurst" % "wiremock" % "1.46" % scope,

        "uk.gov.hmrc" %% "hmrctest" % "0.2.0" % scope
      )
    }.test
  }

  def apply() = compile ++ Test()
}


object Collaborators {

  def apply() = {
    pomExtra := (<url>https://www.gov.uk/government/organisations/hm-revenue-customs</url>
      <licenses>
        <license>
          <name>Apache 2</name>
          <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
        </license>
      </licenses>
      <scm>
        <connection>scm:git@github.tools.tax.service.gov.uk:HMRC/play-microservice.git</connection>
        <developerConnection>scm:git@github.tools.tax.service.gov.uk:HMRC/play-microservice.git</developerConnection>
        <url>git@github.tools.tax.service.gov.uk:HMRC/play-microservice.git</url>
      </scm>
      <developers>
        <developer>
          <id>duncancrawford</id>
          <name>Duncan Crawford</name>
          <url>http://www.equalexperts.com</url>
        </developer>
        <developer>
          <id>jakobgrunig</id>
          <name>Jakob Grunig</name>
          <url>http://www.equalexperts.com</url>
        </developer>
        <developer>
          <id>xnejp03</id>
          <name>Petr Nejedly</name>
          <url>http://www.equalexperts.com</url>
        </developer>
        <developer>
          <id>alvarovilaplana</id>
          <name>Alvaro Vilaplana</name>
          <url>http://www.equalexperts.com</url>
        </developer>
        <developer>
          <id>vaughansharman</id>
          <name>Vaughan Sharman</name>
          <url>http://www.equalexperts.com</url>
        </developer>
        <developer>
          <id>davesammut</id>
          <name>Dave Sammut</name>
          <url>http://www.equalexperts.com</url>
        </developer>
      </developers>)
  }
}

