credentials += Credentials(Path.userHome / ".sbt" / ".credentials")

val hmrcRepoHost = java.lang.System.getProperty("hmrc.repo.host", "https://nexus-preview.tax.service.gov.uk")

resolvers ++= Seq("hmrc-snapshots" at hmrcRepoHost + "/content/repositories/hmrc-snapshots",
                  "hmrc-releases" at hmrcRepoHost + "/content/repositories/hmrc-releases",
                  "typesafe-releases" at hmrcRepoHost + "/content/repositories/typesafe-releases")

addSbtPlugin("uk.gov.hmrc" % "sbt-utils" % "2.1.0")

addSbtPlugin("uk.gov.hmrc" % "hmrc-resolvers" % "0.4.0")

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.7")