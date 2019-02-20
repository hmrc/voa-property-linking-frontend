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
    routesImport ++= Seq("models.SortOrder", "models.messages.MessagePagination", "models.searchApi.AgentPropertiesParameters"),
    // Add the views to the dist
    unmanagedResourceDirectories in Assets += baseDirectory.value / "app" / "assets",
    // Dont include the source assets in the dist package (public folder)
    excludeFilter in Assets := "fonts" || "tasks" || "karma.conf.js" || "tests" || "gulpfile.js*" || "js*" || "src*" || "node_modules*" || "sass*" || "typescript*" || "typings*" || ".jshintrc" || "package.json" || "tsconfig.json" || "tsd.json"
  )  ++ JavaScriptBuild.javaScriptUiSettings

}

private object AppDependencies {

  import play.sbt.PlayImport._

  private val playReactivemongoVersion = "5.2.0"

  private val govukTemplateVersion = "5.22.0"
  private val playUiVersion = "7.22.0"
  private val hmrcTestVersion = "3.2.0"
  private val scalaTestVersion = "3.0.4"
  private val scalaTestPlusPlayVersion = "2.0.1"
  private val pegdownVersion = "1.6.0"
  private val mockitoAllVersion = "1.10.19"
  private val httpCachingClientVersion = "7.2.0"
  private val playConditionalFormMappingVersion = "0.2.0"
  private val playLanguageVersion = "3.4.0"
  private val bootstrapVersion = "3.14.0"
  private val scalacheckVersion = "1.13.4"
  private val jsoupVersion = "1.10.3"
  private val wiremockVersion = "2.15.0"
  //private val playReactivemongoVersion = "6.2.0"
  private val reactiveCircuitBreaker = "3.3.0"
  private val catsCore = "1.3.1"


//  "ai.x" %% "play-json-extensions" % "0.9.0",
//  "uk.gov.hmrc" %% "play-reactivemongo" % playReactivemongoVersion,
//  "com.codahale.metrics" % "metrics-graphite" % "3.0.1",
//  "uk.gov.hmrc" %% "auth-client" % "2.5.0",
//  "com.google.guava" % "guava" % "18.0",
//  "joda-time" % "joda-time" % "2.8.2",
//  "uk.gov.hmrc" %% "frontend-bootstrap" % "8.22.0",
//  "uk.gov.hmrc" %% "http-caching-client" % "7.0.0",
//  "org.typelevel" %% "cats-core" % "0.8.1",
//  "uk.gov.hmrc" %% "play-conditional-form-mapping" % "0.2.0",
//  "uk.gov.hmrc" %% "play-whitelist-filter" % "2.0.0",
//  "uk.gov.hmrc" %% "mongo-lock" % "4.1.0",
//  "com.google.inject.extensions" % "guice-multibindings" % "4.0",
//  "uk.gov.hmrc" %% "reactive-circuit-breaker" % "2.1.0",
//  "com.builtamont" %% "play2-scala-pdf" % "2.0.0.P25" exclude ("com.typesafe.play", "play-logback_2.11")
  
  
  
  
  val compile = Seq(
    filters,
    ws,
    "ai.x" %% "play-json-extensions" % "0.9.0",
    "uk.gov.hmrc" %% "play-reactivemongo" % playReactivemongoVersion,
    "com.codahale.metrics" % "metrics-graphite" % "3.0.1",
    "uk.gov.hmrc" %% "auth-client" % "2.5.0",
    "com.google.guava" % "guava" % "18.0",
    "joda-time" % "joda-time" % "2.8.2",
    "uk.gov.hmrc" %% "frontend-bootstrap" % "11.3.0",
    "uk.gov.hmrc" %% "http-caching-client" % "7.0.0",
    "org.typelevel" %% "cats-core" % "0.8.1",
    "uk.gov.hmrc" %% "play-conditional-form-mapping" % "0.2.0",
    "uk.gov.hmrc" %% "play-whitelist-filter" % "2.0.0",
    "uk.gov.hmrc" %% "mongo-lock" % "5.1.1",
    "com.google.inject.extensions" % "guice-multibindings" % "4.0",
    "uk.gov.hmrc" %% "reactive-circuit-breaker" % "2.1.0",
    "com.builtamont" %% "play2-scala-pdf" % "2.0.0.P25" exclude ("com.typesafe.play", "play-logback_2.11"),
    "uk.gov.hmrc" %% "bootstrap-play-25" % "4.3.0"
    //"uk.gov.hmrc" %% "govuk-template" % govukTemplateVersion,
    //"uk.gov.hmrc" %% "play-ui" % playUiVersion,
    //"uk.gov.hmrc" %% "crypto" % "5.2.0",
    //"uk.gov.hmrc" %% "http-caching-client" % httpCachingClientVersion,
    //"uk.gov.hmrc" %% "play-conditional-form-mapping" % playConditionalFormMappingVersion,
    //"uk.gov.hmrc" %% "bootstrap-play-25" % bootstrapVersion,
    //"uk.gov.hmrc" %% "frontend-bootstrap" % "11.3.0"
    //"uk.gov.hmrc" %% "reactive-circuit-breaker" % reactiveCircuitBreaker
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test: Seq[ModuleID] = ???
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % "3.3.0" % scope,
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
