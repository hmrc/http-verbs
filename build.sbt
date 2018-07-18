import sbt.Keys.crossScalaVersions
import sbt._

val name = "http-verbs"

lazy val library = Project(name, file("."))
  .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning, SbtArtifactory)
  .settings(
    majorVersion        := 8,
    makePublicallyAvailableOnBintray := true,
    scalaVersion        := "2.11.12",
    crossScalaVersions  := Seq("2.11.12"),
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    scalacOptions       ++= Seq("-deprecation"),
    resolvers           :=
      Seq(
        Resolver.bintrayRepo("hmrc", "releases"),
        Resolver.typesafeRepo("releases")
      )
  )
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
