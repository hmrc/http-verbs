import sbt.Keys.crossScalaVersions
import sbt._

val name = "http-verbs"

val scala2_11 = "2.11.12"
val scala2_12 = "2.12.8"

// Disable multiple project tests running at the same time: https://stackoverflow.com/questions/11899723/how-to-turn-off-parallel-execution-of-tests-for-multi-project-builds
// TODO: restrict parallelExecution to tests only (the obvious way to do this using Test scope does not seem to work correctly)
parallelExecution in Global := false

val silencerVersion = "1.4.4"

lazy val commonSettings = Seq(
  organization := "uk.gov.hmrc",
  majorVersion := 13,
  makePublicallyAvailableOnBintray := true,
  scalacOptions ++= Seq("-feature"),
  libraryDependencies ++= Seq(
    compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
    "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
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
    httpVerbs,
    httpVerbsPlay26,
    httpVerbsPlay27,
    httpVerbsTestPlay26,
    httpVerbsTestPlay27
  )
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)

// empty artefact, exists to ensure eviction of previous http-verbs jar which has now moved into http-verbs-play-xx
lazy val httpVerbs = Project("http-verbs", file("http-verbs"))
  .enablePlugins(SbtAutoBuildPlugin, SbtArtifactory)
  .settings(
    commonSettings,
    crossScalaVersions := Seq(scala2_11, scala2_12)
  )

def sharedSources = Seq(
  Compile / unmanagedSourceDirectories   += baseDirectory.value / "../http-verbs-common/src/main/scala",
  Compile / unmanagedResourceDirectories += baseDirectory.value / "../http-verbs-common/src/main/resources",
  Test    / unmanagedSourceDirectories   += baseDirectory.value / "../http-verbs-common/src/test/scala",
  Test    / unmanagedResourceDirectories += baseDirectory.value / "../http-verbs-common/src/test/resources",
)

def copySources(module: Project) = Seq(
  Compile / scalaSource       := (module / Compile / scalaSource      ).value,
  Compile / resourceDirectory := (module / Compile / resourceDirectory).value,
  Test    / scalaSource       := (module / Test    / scalaSource      ).value,
  Test    / resourceDirectory := (module / Test    / resourceDirectory).value
)

lazy val httpVerbsPlay26 = Project("http-verbs-play-26", file("http-verbs-play-26"))
  .enablePlugins(SbtAutoBuildPlugin, SbtArtifactory)
  .settings(
    commonSettings,
    sharedSources,
    crossScalaVersions := Seq(scala2_11, scala2_12),
    libraryDependencies ++= AppDependencies.coreCompileCommon ++
      AppDependencies.coreCompilePlay26 ++
      AppDependencies.coreTestCommon ++
      AppDependencies.coreTestPlay26,
    Test / fork := true // akka is not unloaded properly, which can affect other tests
  )
  .dependsOn(httpVerbs)

lazy val httpVerbsPlay27 = Project("http-verbs-play-27", file("http-verbs-play-27"))
  .enablePlugins(SbtAutoBuildPlugin, SbtArtifactory)
  .settings(
    commonSettings,
    crossScalaVersions := Seq(scala2_11, scala2_12),
    sharedSources,
    copySources(httpVerbsPlay26),
    libraryDependencies ++= AppDependencies.coreCompileCommon ++
      AppDependencies.coreCompilePlay27 ++
      AppDependencies.coreTestCommon ++
      AppDependencies.coreTestPlay27,
    Test    / fork := true // akka is not unloaded properly, which can affect other tests
  )
  .dependsOn(httpVerbs)

lazy val httpVerbsTestCommon = Project("http-verbs-test-common", file("http-verbs-test-common"))
  .enablePlugins(SbtAutoBuildPlugin, SbtArtifactory)
  .settings(
    commonSettings,
    libraryDependencies ++= AppDependencies.testCompileCommon,
    crossScalaVersions := Seq(scala2_11, scala2_12)
  )

lazy val httpVerbsTestPlay26 = Project("http-verbs-test-play-26", file("http-verbs-test-play-26"))
  .enablePlugins(SbtAutoBuildPlugin, SbtArtifactory)
  .settings(
    commonSettings,
    Compile / scalaSource := (httpVerbsTestCommon / Compile / scalaSource).value,
    crossScalaVersions := Seq(scala2_11, scala2_12),
    libraryDependencies ++= AppDependencies.testCompileCommon ++ AppDependencies.testCompilePlay26
  )
  .dependsOn(httpVerbsPlay26)

lazy val httpVerbsTestPlay27 = Project("http-verbs-test-play-27", file("http-verbs-test-play-27"))
  .enablePlugins(SbtAutoBuildPlugin, SbtArtifactory)
  .settings(
    commonSettings,
    Compile / scalaSource := (httpVerbsTestCommon / Compile / scalaSource).value,
    crossScalaVersions := Seq(scala2_11, scala2_12),
    libraryDependencies ++= AppDependencies.testCompileCommon ++ AppDependencies.testCompilePlay27
  )
  .dependsOn(httpVerbsPlay27)
