import sbt.Keys.crossScalaVersions
import sbt._

val name = "http-verbs"

val scala2_11 = "2.11.12"
val scala2_12 = "2.12.8"

// Disable multiple project tests running at the same time: https://stackoverflow.com/questions/11899723/how-to-turn-off-parallel-execution-of-tests-for-multi-project-builds
// TODO: restrict parallelExecution to tests only (the obvious way to do this using Test scope does not seem to work correctly)
parallelExecution in Global := false

lazy val commonSettings = Seq(
  organization := "uk.gov.hmrc",
  majorVersion := 11,
  makePublicallyAvailableOnBintray := true,
  resolvers := Seq(
    Resolver.bintrayRepo("hmrc", "releases"),
    Resolver.typesafeRepo("releases")
  )
)

lazy val library = (project in file("."))
  .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning, SbtArtifactory)
  .settings(
    commonSettings,
    publish := {},
    publishAndDistribute := {},
    crossScalaVersions := Seq.empty
  )
  .aggregate(
    httpVerbsPlay25,
    httpVerbsPlay26,
    httpVerbsPlay27
  )
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)

lazy val httpVerbsPlay25 = Project("http-verbs-play-25", file("http-verbs-play-25"))
  .enablePlugins(SbtAutoBuildPlugin, SbtArtifactory)
  .settings(
    commonSettings,
    Compile / unmanagedSourceDirectories += baseDirectory.value / "../http-verbs-common/src/main/scala",
    Test    / unmanagedSourceDirectories += baseDirectory.value / "../http-verbs-common/src/test/scala",
    crossScalaVersions := Seq(scala2_11),
    libraryDependencies ++= AppDependencies.compileCommon ++ AppDependencies.compilePlay25 ++ AppDependencies.testCommon ++ AppDependencies.testPlay25,
    Test / fork := true // akka is not unloaded properly, which can affect other tests
  )

lazy val httpVerbsPlay26 = Project("http-verbs-play-26", file("http-verbs-play-26"))
  .enablePlugins(SbtAutoBuildPlugin, SbtArtifactory)
  .settings(
    commonSettings,
    crossScalaVersions := Seq(scala2_11, scala2_12),
    libraryDependencies ++= AppDependencies.compileCommon ++ AppDependencies.compilePlay26 ++ AppDependencies.testCommon ++ AppDependencies.testPlay26,
    Compile / unmanagedSourceDirectories += baseDirectory.value / "../http-verbs-common/src/main/scala",
    Test    / unmanagedSourceDirectories += baseDirectory.value / "../http-verbs-common/src/test/scala",
    Test / fork := true // akka is not unloaded properly, which can affect other tests
  )

lazy val httpVerbsPlay27 = Project("http-verbs-play-27", file("http-verbs-play-27"))
  .enablePlugins(SbtAutoBuildPlugin, SbtArtifactory)
  .settings(
    commonSettings,
    crossScalaVersions := Seq(scala2_11, scala2_12),
    libraryDependencies ++= AppDependencies.compileCommon ++ AppDependencies.compilePlay27 ++ AppDependencies.testCommon ++ AppDependencies.testPlay27,
    Compile / unmanagedSourceDirectories += baseDirectory.value / "../http-verbs-common/src/main/scala",
    Test    / unmanagedSourceDirectories += baseDirectory.value / "../http-verbs-common/src/test/scala",
    Compile / scalaSource := (httpVerbsPlay26 / Compile / scalaSource).value,
    Test    / scalaSource := (httpVerbsPlay26 / Test    / scalaSource).value,
    Test    / fork := true // akka is not unloaded properly, which can affect other tests
  )
