addSbtPlugin("uk.gov.hmrc" % "sbt-utils" % "2.1.0")

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.8")


resolvers += Resolver.url(
"bintray-sbt-plugin-releases",
    url("https://dl.bintray.com/content/sbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns)

addSbtPlugin("me.lessis" % "bintray-sbt" % "0.2.0")