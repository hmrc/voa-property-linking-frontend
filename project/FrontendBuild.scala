import sbt._

object FrontendBuild extends Build with MicroService {

    import com.typesafe.sbt.web.SbtWeb.autoImport._
    import play.PlayImport.PlayKeys._
    import sbt.Keys._
    import scala.util.Properties.envOrElse

  val appName = "voa-property-linking-frontend"

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()

  override lazy val plugins : Seq[Plugins] = Seq(play.PlayScala)

  override val defaultPort: Int = 9523

  override lazy val playSettings: Seq[Setting[_]] = Seq(

       // Turn off play's internal less compiler
       lessEntryPoints := Nil,
       // Turn off play's internal javascript compiler
       javascriptEntryPoints := Nil,
       // Add the views to the dist
       unmanagedResourceDirectories in Assets += baseDirectory.value / "app" / "assets",
       // Dont include the source assets in the dist package (public folder)
       excludeFilter in Assets := "tasks" || "karma.conf.js" || "tests" || "gulpfile.js*" || "js*" || "src*" || "node_modules*" || "sass*" || "typescript*" || "typings*" || ".jshintrc" || "package.json" || "tsconfig.json" || "tsd.json"
       ) ++ JavaScriptBuild.javaScriptUiSettings

  }

private object AppDependencies {

  import play.PlayImport._
  import play.core.PlayVersion

  private val playHealthVersion = "1.1.0"
  private val playUiVersion = "4.16.0"

  val compile = Seq(
    filters,
    ws,
    "com.kenshoo" %% "metrics-play" % "2.3.0_0.1.8",
    "com.codahale.metrics" % "metrics-graphite" % "3.0.1",
    "com.google.guava" % "guava" % "18.0",
    "uk.gov.hmrc" %% "govuk-template" % "4.0.0",
    "uk.gov.hmrc" %% "play-config" % "2.0.1",
    "uk.gov.hmrc" %% "play-ui" % playUiVersion,
    "uk.gov.hmrc" %% "play-health" % playHealthVersion,
    "uk.gov.hmrc" %% "frontend-bootstrap" % "6.7.0",
    "uk.gov.hmrc" %% "play-json-logger" % "2.1.0",
    "uk.gov.hmrc" %% "http-caching-client" % "5.6.0",
    "joda-time" % "joda-time" % "2.8.2",
    "uk.gov.hmrc" %% "play-authorised-frontend" % "5.8.0"
  )

  trait TestDependencies {
    lazy val scope: String = "test,it"
    lazy val test : Seq[ModuleID] = ???
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "org.scalatestplus" %% "play" % "1.2.0" % "test",
        "org.scalatest" %% "scalatest" % "2.2.2" % scope,
        "org.pegdown" % "pegdown" % "1.4.2" % scope
      )
    }.test
  }

  object IntegrationTest {
    def apply() = new TestDependencies {
      override lazy val scope = "it"

      override lazy val test = Seq(
        "org.scalatestplus" %% "play" % "1.2.0" % scope,
        "org.scalatest" %% "scalatest" % "2.2.2" % scope,
        "org.pegdown" % "pegdown" % "1.4.2" % scope,
        "org.jsoup" % "jsoup" % "1.9.1" % scope
      )
    }.test
  }

  def apply() = compile ++ Test() ++ IntegrationTest()

}
