resolvers ++= Seq("Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/",
					"bintray-sbt-plugin-releases" at "https://dl.bintray.com/content/sbt/sbt-plugin-releases")

addSbtPlugin("uk.gov.hmrc" % "sbt-utils" % "2.1.0")

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.8")

addSbtPlugin("me.lessis" % "bintray-sbt" % "0.2.0")