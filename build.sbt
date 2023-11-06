import sbt.Keys.crossScalaVersions
import sbt._

// Disable multiple project tests running at the same time
// https://www.scala-sbt.org/1.x/docs/Parallel-Execution.html
Global / concurrentRestrictions += Tags.limitSum(1, Tags.Test, Tags.Untagged)

val scala2_12 = "2.12.18"
val scala2_13 = "2.13.12"

ThisBuild / majorVersion     := 14
ThisBuild / scalaVersion     := scala2_13
ThisBuild / isPublicArtefact := true
ThisBuild / scalacOptions    := Seq("-feature")


lazy val library = (project in file("."))
  .settings(
    publish / skip := true,
    crossScalaVersions := Seq.empty
  )
  .aggregate(
    httpVerbsPlay28, httpVerbsTestPlay28,
    httpVerbsPlay29, httpVerbsTestPlay29,
    httpVerbsPlay30, httpVerbsTestPlay30
  )
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)

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

lazy val httpVerbsPlay28 = Project("http-verbs-play-28", file("http-verbs-play-28"))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    shareSources("http-verbs-common"),
    crossScalaVersions := Seq(scala2_12, scala2_13),
    libraryDependencies ++=
      LibDependencies.coreCompileCommon(scalaVersion.value) ++
      LibDependencies.coreCompilePlay28 ++
      LibDependencies.coreTestCommon ++
      LibDependencies.coreTestPlay28,
    Test / fork := true // akka is not unloaded properly, which can affect other tests
  )
  .settings( // https://github.com/sbt/sbt-buildinfo
    buildInfoKeys := Seq[BuildInfoKey](version),
    buildInfoPackage := "uk.gov.hmrc.http"
   )

lazy val httpVerbsPlay29 = Project("http-verbs-play-29", file("http-verbs-play-29"))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    shareSources("http-verbs-common"),
    crossScalaVersions := Seq(scala2_13),
    libraryDependencies ++=
      LibDependencies.coreCompileCommon(scalaVersion.value) ++
      LibDependencies.coreCompilePlay29 ++
      LibDependencies.coreTestCommon ++
      LibDependencies.coreTestPlay29,
    Test / fork := true // akka is not unloaded properly, which can affect other tests
  )
  .settings( // https://github.com/sbt/sbt-buildinfo
    buildInfoKeys := Seq[BuildInfoKey](version),
    buildInfoPackage := "uk.gov.hmrc.http"
   )

lazy val httpVerbsPlay30 = Project("http-verbs-play-30", file("http-verbs-play-30"))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    shareSources("http-verbs-common"),
    crossScalaVersions := Seq(scala2_13),
    libraryDependencies ++=
      LibDependencies.coreCompileCommon(scalaVersion.value) ++
      LibDependencies.coreCompilePlay30 ++
      LibDependencies.coreTestCommon ++
      LibDependencies.coreTestPlay30,
    Test / fork := true // akka is not unloaded properly, which can affect other tests
  )
  .settings( // https://github.com/sbt/sbt-buildinfo
    buildInfoKeys := Seq[BuildInfoKey](version),
    buildInfoPackage := "uk.gov.hmrc.http"
   )

lazy val httpVerbsTestPlay28 = Project("http-verbs-test-play-28", file("http-verbs-test-play-28"))
  .settings(
    shareSources("http-verbs-test-common"),
    crossScalaVersions := Seq(scala2_12, scala2_13),
    libraryDependencies ++= LibDependencies.testCompilePlay28,
    Test / fork := true // required to look up wiremock resources
  )
  .dependsOn(httpVerbsPlay28)

lazy val httpVerbsTestPlay29 = Project("http-verbs-test-play-29", file("http-verbs-test-play-29"))
  .settings(
    shareSources("http-verbs-test-common"),
    crossScalaVersions := Seq(scala2_13),
    libraryDependencies ++= LibDependencies.testCompilePlay29,
    Test / fork := true // required to look up wiremock resources
  )
  .dependsOn(httpVerbsPlay29)

lazy val httpVerbsTestPlay30 = Project("http-verbs-test-play-30", file("http-verbs-test-play-30"))
  .settings(
    shareSources("http-verbs-test-common"),
    crossScalaVersions := Seq(scala2_13),
    libraryDependencies ++= LibDependencies.testCompilePlay30,
    Test / fork := true // required to look up wiremock resources
  )
  .dependsOn(httpVerbsPlay30)
