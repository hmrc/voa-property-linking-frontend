
resolvers ++= Seq(
  Resolver.url(
    "hmrc-sbt-plugin-releases", url("https://dl.bintray.com/hmrc/sbt-plugin-releases")
  )(Resolver.ivyStylePatterns),
  Resolver.bintrayRepo("hmrc", "releases")
)

resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("com.github.gseitz" % "sbt-release" % "0.8.5")

addSbtPlugin("uk.gov.hmrc" % "sbt-settings" % "3.2.0")

addSbtPlugin("uk.gov.hmrc" % "sbt-auto-build" % "1.0.0")

addSbtPlugin("uk.gov.hmrc" % "sbt-git-versioning" % "0.8.0")

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.8.0")

addSbtPlugin("uk.gov.hmrc" % "sbt-distributables" % "1.0.0")

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.8")
 
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")

addSbtPlugin("com.artima.supersafe" % "sbtplugin" % "1.1.0")
