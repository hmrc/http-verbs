import sbt.Keys.crossScalaVersions
import sbt._

// Disable multiple project tests running at the same time
// https://www.scala-sbt.org/1.x/docs/Parallel-Execution.html
Global / concurrentRestrictions += Tags.limitSum(1, Tags.Test, Tags.Untagged)

val scala2_13 = "2.13.16"
val scala3    = "3.3.6"

ThisBuild / majorVersion     := 15
ThisBuild / scalaVersion     := scala2_13
ThisBuild / isPublicArtefact := true
ThisBuild / scalacOptions    ++= Seq("-feature")


lazy val library = (project in file("."))
  .settings(
    publish / skip := true,
    crossScalaVersions := Seq.empty
  )
  .aggregate(httpVerbsPlay30, httpVerbsTestPlay30)
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)

lazy val httpVerbsPlay30 = Project("http-verbs-play-30", file("http-verbs-play-30"))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    crossScalaVersions := Seq(scala2_13, scala3),
    libraryDependencies ++=
      LibDependencies.coreCompileCommon(scalaVersion.value) ++
      LibDependencies.coreCompilePlay30 ++
      LibDependencies.coreTestCommon ++
      LibDependencies.coreTestPlay30,
    Test / fork := true // pekko is not unloaded properly, which can affect other tests
  )
  .settings( // https://github.com/sbt/sbt-buildinfo
    buildInfoKeys := Seq[BuildInfoKey](version),
    buildInfoPackage := "uk.gov.hmrc.http"
  )

lazy val httpVerbsTestPlay30 = Project("http-verbs-test-play-30", file("http-verbs-test-play-30"))
  .settings(
    crossScalaVersions := Seq(scala2_13, scala3),
    libraryDependencies ++= LibDependencies.testCompilePlay30,
    Test / fork := true // required to look up wiremock resources
  )
  .dependsOn(httpVerbsPlay30)
