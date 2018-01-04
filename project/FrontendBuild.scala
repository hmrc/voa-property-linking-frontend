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
    routesImport ++= Seq("models.SortOrder", "models.messages.MessagePagination"),
    // Add the views to the dist
    unmanagedResourceDirectories in Assets += baseDirectory.value / "app" / "assets",
    // Dont include the source assets in the dist package (public folder)
    excludeFilter in Assets := "fonts" || "tasks" || "karma.conf.js" || "tests" || "gulpfile.js*" || "js*" || "src*" || "node_modules*" || "sass*" || "typescript*" || "typings*" || ".jshintrc" || "package.json" || "tsconfig.json" || "tsd.json"
  ) ++ JavaScriptBuild.javaScriptUiSettings

}

private object AppDependencies {

  import play.sbt.PlayImport._

  private val playReactivemongoVersion = "5.0.0"
  val compile = Seq(
    filters,
    ws,
    "uk.gov.hmrc" %% "play-reactivemongo" % playReactivemongoVersion,
    "com.codahale.metrics" % "metrics-graphite" % "3.0.1",
    "uk.gov.hmrc" %% "auth-client" % "2.3.0",
    "com.google.guava" % "guava" % "18.0",
    "joda-time" % "joda-time" % "2.8.2",
    "uk.gov.hmrc" %% "frontend-bootstrap" % "8.11.0",
    "uk.gov.hmrc" %% "http-caching-client" % "7.0.0",
    "org.typelevel" %% "cats-core" % "0.8.1",
    "uk.gov.hmrc" %% "play-conditional-form-mapping" % "0.2.0",
    "uk.gov.hmrc" %% "play-whitelist-filter" % "2.0.0",
    "uk.gov.hmrc" %% "mongo-lock" % "4.1.0",
    "com.google.inject.extensions" % "guice-multibindings" % "4.0",
    "uk.gov.hmrc" %% "reactive-circuit-breaker" % "2.1.0",
    "com.builtamont" %% "play2-scala-pdf" % "2.0.0.P25" exclude ("com.typesafe.play", "play-logback_2.11")
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test: Seq[ModuleID] = ???
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % "2.2.0" % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0-M1" % scope,
        "org.scalatest" %% "scalatest" % "3.0.1",
        "org.scalatest" %% "scalatest" % "3.0.1" % scope,
        "org.scalacheck" %% "scalacheck" % "1.13.4" % scope,
        "org.pegdown" % "pegdown" % "1.6.0" % "test,it",
        "org.jsoup" % "jsoup" % "1.9.1",
        "org.mockito" % "mockito-core" % "2.2.9"
      )
    }.test
  }

  def apply() = compile ++ Test()

}
