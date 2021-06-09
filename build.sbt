import sbt.Keys.crossScalaVersions
import sbt._

// Disable multiple project tests running at the same time
// https://www.scala-sbt.org/1.x/docs/Parallel-Execution.html
Global / concurrentRestrictions += Tags.limitSum(1, Tags.Test, Tags.Untagged)

val silencerVersion = "1.7.1"

lazy val commonSettings = Seq(
  organization := "uk.gov.hmrc",
  majorVersion := 13,
  scalaVersion := "2.12.12",
  isPublicArtefact := true,
  scalacOptions ++= Seq("-feature"),
  libraryDependencies ++= Seq(
    compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
    "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
  )
)

lazy val library = (project in file("."))
  .settings(
    commonSettings,
    publish / skip := true,
    crossScalaVersions := Seq.empty
  )
  .aggregate(
    httpVerbs,
    httpVerbsPlay26,
    httpVerbsPlay27,
    httpVerbsPlay28,
    httpVerbsTestPlay26,
    httpVerbsTestPlay27,
    httpVerbsTestPlay28
  )
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)

// empty artefact, exists to ensure eviction of previous http-verbs jar which has now moved into http-verbs-play-xx
lazy val httpVerbs = Project("http-verbs", file("http-verbs"))
  .settings(
    commonSettings
  )

def shareSources(location: String) = Seq(
  Compile / unmanagedSourceDirectories   += baseDirectory.value / s"../$location/src/main/scala",
  Compile / unmanagedResourceDirectories += baseDirectory.value / s"../$location/src/main/resources",
  Test    / unmanagedSourceDirectories   += baseDirectory.value / s"../$location/src/test/scala",
  Test    / unmanagedResourceDirectories += baseDirectory.value / s"../$location/src/test/resources"
)
def copySources(module: Project) = Seq(
  Compile / scalaSource       := (module / Compile / scalaSource      ).value,
  Compile / resourceDirectory := (module / Compile / resourceDirectory).value,
  Test    / scalaSource       := (module / Test    / scalaSource      ).value,
  Test    / resourceDirectory := (module / Test    / resourceDirectory).value
)

lazy val sharedSources =
  shareSources("http-verbs-common")

lazy val httpVerbsPlay26 = Project("http-verbs-play-26", file("http-verbs-play-26"))
  .settings(
    commonSettings,
    sharedSources,
    libraryDependencies ++=
      AppDependencies.coreCompileCommon ++
      AppDependencies.coreCompilePlay26 ++
      AppDependencies.coreTestCommon ++
      AppDependencies.coreTestPlay26,
    Test / fork := true // akka is not unloaded properly, which can affect other tests
  )
  .dependsOn(httpVerbs)

lazy val httpVerbsPlay27 = Project("http-verbs-play-27", file("http-verbs-play-27"))
  .settings(
    commonSettings,
    sharedSources,
    copySources(httpVerbsPlay26),
    libraryDependencies ++=
      AppDependencies.coreCompileCommon ++
      AppDependencies.coreCompilePlay27 ++
      AppDependencies.coreTestCommon ++
      AppDependencies.coreTestPlay27,
    Test / fork := true // akka is not unloaded properly, which can affect other tests
  )
  .dependsOn(httpVerbs)

lazy val httpVerbsPlay28 = Project("http-verbs-play-28", file("http-verbs-play-28"))
  .settings(
    commonSettings,
    sharedSources,
    libraryDependencies ++=
      AppDependencies.coreCompileCommon ++
      AppDependencies.coreCompilePlay28 ++
      AppDependencies.coreTestCommon ++
      AppDependencies.coreTestPlay28,
    Test / fork := true // akka is not unloaded properly, which can affect other tests
  )
  .dependsOn(httpVerbs)

lazy val sharedTestSources =
  shareSources("http-verbs-test-common")

lazy val httpVerbsTestPlay26 = Project("http-verbs-test-play-26", file("http-verbs-test-play-26"))
  .settings(
    commonSettings,
    sharedTestSources,
    libraryDependencies ++= AppDependencies.testCompileCommon ++ AppDependencies.testCompilePlay26
  )
  .dependsOn(httpVerbsPlay26)

lazy val httpVerbsTestPlay27 = Project("http-verbs-test-play-27", file("http-verbs-test-play-27"))
  .settings(
    commonSettings,
    sharedTestSources,
    libraryDependencies ++= AppDependencies.testCompileCommon ++ AppDependencies.testCompilePlay27
  )
  .dependsOn(httpVerbsPlay27)

lazy val httpVerbsTestPlay28 = Project("http-verbs-test-play-28", file("http-verbs-test-play-28"))
  .settings(
    commonSettings,
    sharedTestSources,
    libraryDependencies ++= AppDependencies.testCompileCommon ++ AppDependencies.testCompilePlay28
  )
  .dependsOn(httpVerbsPlay28)
