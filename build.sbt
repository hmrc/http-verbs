import sbt.Keys.crossScalaVersions
import sbt._

// Disable multiple project tests running at the same time
// https://www.scala-sbt.org/1.x/docs/Parallel-Execution.html
Global / concurrentRestrictions += Tags.limitSum(1, Tags.Test, Tags.Untagged)

val scala2_12 = "2.12.17"
val scala2_13 = "2.13.10"

lazy val commonSettings = Seq(
  organization := "uk.gov.hmrc",
  majorVersion := 14,
  scalaVersion := scala2_12,
  isPublicArtefact := true,
  scalacOptions ++= Seq("-feature")
)

lazy val library = (project in file("."))
  .settings(
    commonSettings,
    publish / skip := true,
    crossScalaVersions := Seq.empty
  )
  .aggregate(
    httpVerbs,
    httpVerbsPlay28,
    httpVerbsTestPlay28
  )
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)

// empty artefact, exists to ensure eviction of previous http-verbs jar which has now moved into http-verbs-play-xx
lazy val httpVerbs = Project("http-verbs", file("http-verbs"))
  .settings(
    commonSettings,
    crossScalaVersions := Seq(scala2_12, scala2_13)
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

lazy val httpVerbsPlay28 = Project("http-verbs-play-28", file("http-verbs-play-28"))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    commonSettings,
    sharedSources,
    crossScalaVersions := Seq(scala2_12, scala2_13),
    libraryDependencies ++=
      AppDependencies.coreCompileCommon(scalaVersion.value) ++
      AppDependencies.coreCompilePlay28 ++
      AppDependencies.coreTestCommon ++
      AppDependencies.coreTestPlay28,
    Test / fork := true // akka is not unloaded properly, which can affect other tests
  )
  .settings( // https://github.com/sbt/sbt-buildinfo
    buildInfoKeys := Seq[BuildInfoKey](version),
    buildInfoPackage := "uk.gov.hmrc.http"
   )
  .dependsOn(httpVerbs)

lazy val sharedTestSources =
  shareSources("http-verbs-test-common")

lazy val httpVerbsTestPlay28 = Project("http-verbs-test-play-28", file("http-verbs-test-play-28"))
  .settings(
    commonSettings,
    sharedTestSources,
    crossScalaVersions := Seq(scala2_12, scala2_13),
    libraryDependencies ++= AppDependencies.testCompilePlay28,
    Test / fork := true // required to look up wiremock resources
  )
  .dependsOn(httpVerbsPlay28)
