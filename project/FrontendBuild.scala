import sbt._

object FrontendBuild extends Build with MicroService {

  import com.typesafe.sbt.web.SbtWeb.autoImport._
  import play.sbt.routes.RoutesKeys._
  import sbt.Keys._

  val appName = "voa-property-linking-frontend"

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()

  override lazy val plugins: Seq[Plugins] = Seq(play.sbt.PlayScala)

  override val defaultPort: Int = 9523

  override lazy val playSettings: Seq[Setting[_]] = Seq(
    routesImport ++= Seq("binders.propertylinks._", "binders.propertylinks.EvidenceChoices._", "binders.pagination._", "models.SortOrder", "models.messages.MessagePagination", "models.searchApi.AgentPropertiesParameters"),
    // Add the views to the dist
    unmanagedResourceDirectories in Assets += baseDirectory.value / "app" / "assets",
    // Dont include the source assets in the dist package (public folder)
    excludeFilter in Assets := "fonts" || "tasks" || "karma.conf.js" || "tests" || "gulpfile.js*" || "js*" || "src*" || "node_modules*" || "sass*" || "typescript*" || "typings*" || ".jshintrc" || "package.json" || "tsconfig.json" || "tsd.json"
  )  ++ JavaScriptBuild.javaScriptUiSettings

}

private object AppDependencies {

  import play.sbt.PlayImport._

  private val bootstrapVersion = "1.1.0"

  val compile = Seq(
    guice,
    filters,
    ws,
    "ai.x" %% "play-json-extensions" % "0.10.0",
    "uk.gov.hmrc" %% "simple-reactivemongo" % "7.20.0-play-26",
    "com.codahale.metrics" % "metrics-graphite" % "3.0.1",
    "com.google.guava" % "guava" % "18.0",
    "joda-time" % "joda-time" % "2.10.4",
    "uk.gov.hmrc" %% "bootstrap-play-26" % bootstrapVersion,
    "uk.gov.hmrc" %% "play-ui" % "8.3.0-play-26",
    "uk.gov.hmrc" %% "govuk-template" % "5.43.0-play-26",
    "uk.gov.hmrc" %% "http-caching-client" % "9.0.0-play-26",
    "org.typelevel" %% "cats-core" % "1.6.1",
    "uk.gov.hmrc" %% "play-conditional-form-mapping" % "1.2.0-play-26",
    "uk.gov.hmrc" %% "play-whitelist-filter" % "3.1.0-play-26",
    "uk.gov.hmrc" %% "mongo-lock" % "6.15.0-play-26",
    "uk.gov.hmrc" %% "reactive-circuit-breaker" % "3.3.0"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test: Seq[ModuleID] = ???
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % "3.6.0-play-26" % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.0" % scope,
        "org.scalatest" %% "scalatest" % "3.0.6" % scope,
        "org.scalacheck" %% "scalacheck" % "1.13.4" % scope,
        "org.pegdown" % "pegdown" % "1.6.0" % "test,it",
        "org.jsoup" % "jsoup" % "1.9.1" % scope,
        "org.mockito" % "mockito-core" % "2.25.0" % scope
      )
    }.test
  }

  def apply() = compile ++ Test()

}
