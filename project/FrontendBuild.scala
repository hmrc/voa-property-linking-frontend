import sbt._

object FrontendBuild extends Build with MicroService {

    import com.typesafe.sbt.web.SbtWeb.autoImport._
    import play.sbt.PlayImport.PlayKeys._
    import sbt.Keys._
    import scala.util.Properties.envOrElse

  val appName = "voa-property-linking-frontend"

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()

  override lazy val plugins : Seq[Plugins] = Seq(play.sbt.PlayScala)

  override val defaultPort: Int = 9523

  override lazy val playSettings: Seq[Setting[_]] = Seq(

       // Add the views to the dist
       unmanagedResourceDirectories in Assets += baseDirectory.value / "app" / "assets",
       // Dont include the source assets in the dist package (public folder)
       excludeFilter in Assets := "tasks" || "karma.conf.js" || "tests" || "gulpfile.js*" || "js*" || "src*" || "node_modules*" || "sass*" || "typescript*" || "typings*" || ".jshintrc" || "package.json" || "tsconfig.json" || "tsd.json"
       ) ++ JavaScriptBuild.javaScriptUiSettings

  }

private object AppDependencies {

  import play.sbt.PlayImport._
  import play.core.PlayVersion

  val compile = Seq(
    filters,
    ws,
    "com.codahale.metrics" % "metrics-graphite" % "3.0.1",
    "com.google.guava" % "guava" % "18.0",
    "joda-time" % "joda-time" % "2.8.2",
    "uk.gov.hmrc" %% "frontend-bootstrap" % "7.5.0",
    "uk.gov.hmrc" %% "govuk-template" % "5.0.0",
    "uk.gov.hmrc" %% "http-caching-client" % "6.0.0",
    "uk.gov.hmrc" %% "play-authorised-frontend" % "6.1.0",
    "uk.gov.hmrc" %% "play-config" % "3.0.0",
    "uk.gov.hmrc" %% "play-health" % "2.0.0",
    "uk.gov.hmrc" %% "play-json-logger" % "3.0.0",
    "uk.gov.hmrc" %% "play-ui" % "5.2.0"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % scope,
        "org.scalatest" %% "scalatest" % "2.2.6" % scope,
        "org.pegdown" % "pegdown" % "1.6.0" % scope,
        "org.jsoup" % "jsoup" % "1.9.1",
        "org.mockito" % "mockito-core" % "2.2.9"
      )
    }.test
  }

  def apply() = compile ++ Test()

}
