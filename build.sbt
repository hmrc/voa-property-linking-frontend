/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.typesafe.sbt.web.SbtWeb.autoImport._
import play.sbt.PlayImport._
import play.sbt.routes.RoutesKeys._
import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt._
import uk.gov.hmrc.DefaultBuildSettings._
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.versioning.SbtGitVersioning
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

lazy val plugins: Seq[Plugins] = Seq(play.sbt.PlayScala, SbtDistributablesPlugin)


val compileDependencies = Seq(
  guice,
  filters,
  ws,
  "ai.x" %% "play-json-extensions" % "0.10.0",
  "uk.gov.hmrc" %% "simple-reactivemongo" % "7.22.0-play-26",
  "com.codahale.metrics" % "metrics-graphite" % "3.0.1",
  "com.google.guava" % "guava" % "18.0",
  "joda-time" % "joda-time" % "2.10.4",
  "uk.gov.hmrc" %% "bootstrap-play-26" % "1.1.0",
  "uk.gov.hmrc" %% "play-ui" % "8.3.0-play-26",
  "uk.gov.hmrc" %% "govuk-template" % "5.43.0-play-26",
  "uk.gov.hmrc" %% "http-caching-client" % "9.0.0-play-26",
  "org.typelevel" %% "cats-core" % "1.6.1",
  "uk.gov.hmrc" %% "play-conditional-form-mapping" % "1.2.0-play-26",
  "uk.gov.hmrc" %% "play-whitelist-filter" % "3.1.0-play-26",
  "uk.gov.hmrc" %% "mongo-lock" % "6.15.0-play-26",
  "uk.gov.hmrc" %% "reactive-circuit-breaker" % "3.3.0"
)

val testDependencies = Seq(
  "uk.gov.hmrc" %% "hmrctest" % "3.6.0-play-26" % Test,
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.0" % Test,
  "org.scalatest" %% "scalatest" % "3.0.6" % Test,
  "org.scalacheck" %% "scalacheck" % "1.13.4" % Test,
  "org.pegdown" % "pegdown" % "1.6.0" % "test,it",
  "org.jsoup" % "jsoup" % "1.9.1" % Test,
  "org.mockito" % "mockito-core" % "2.25.0" % Test
)

lazy val TemplateTest = config("tt") extend Test
lazy val TemplateItTest = config("tit") extend IntegrationTest

lazy val microservice = Project(appName, file("."))
  .enablePlugins(plugins: _*)
  .settings(playSettings ++ scoverageSettings: _*)
  .settings(scalaSettings: _*)
  .settings(publishingSettings: _*)
  .settings(organization := "uk.gov.hmrc")
  .settings(PlayKeys.playDefaultPort := 9523)
  .settings(majorVersion := 0)
  .settings(
    targetJvm := "jvm-1.8",
    libraryDependencies ++= compileDependencies ++ testDependencies,
    parallelExecution in Test := false,
    fork in Test := true,
    retrieveManaged := true,
    testGrouping := oneForkedJvmPerTest((definedTests in Test).value)
  )
  .settings(inConfig(TemplateTest)(Defaults.testSettings): _*)
  .configs(IntegrationTest)
  .settings(inConfig(TemplateItTest)(Defaults.itSettings): _*)
  .settings(
    Keys.fork in IntegrationTest := false,
    unmanagedSourceDirectories in IntegrationTest <<= (baseDirectory in IntegrationTest) (base => Seq(base / "it")),
    addTestReportOption(IntegrationTest, "int-test-reports"),
    parallelExecution in IntegrationTest := false)
  .settings(resolvers ++= Seq(
    Resolver.bintrayRepo("hmrc", "releases"),
    Resolver.jcenterRepo)
  )
  .settings(evictionWarningOptions in update := EvictionWarningOptions.default.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false).withWarnScalaVersionEviction(false))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)


def oneForkedJvmPerTest(tests: Seq[TestDefinition]) =
  tests map {
    test => new Group(test.name, Seq(test), SubProcess(ForkOptions(runJVMOptions = Seq("-Dtest.name=" + test.name))))
  }

val appName = "voa-property-linking-frontend"

val playSettings: Seq[Setting[_]] = Seq(
  routesImport ++= Seq("binders.propertylinks._", "binders.propertylinks.EvidenceChoices._", "binders.pagination._", "models.SortOrder", "models.messages.MessagePagination", "models.searchApi.AgentPropertiesParameters"),
  // Add the views to the dist
  unmanagedResourceDirectories in Assets += baseDirectory.value / "app" / "assets",
  // Dont include the source assets in the dist package (public folder)
  excludeFilter in Assets := "fonts" || "tasks" || "karma.conf.js" || "tests" || "gulpfile.js*" || "js*" || "src*" || "node_modules*" || "sass*" || "typescript*" || "typings*" || ".jshintrc" || "package.json" || "tsconfig.json" || "tsd.json"
) ++ JavaScriptBuild.javaScriptUiSettings

lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")

lazy val scoverageSettings = {
  // Semicolon-separated list of regexs matching classes to exclude
  import scoverage.ScoverageKeys
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;controllers.test.*;connectors.test.*;views.*;config.*;poc.view.*;poc.config.*;.*(AuthService|BuildInfo|Routes).*;.*models.*;.*modules.*;.*utils.Conditionals.*;.*auth.GovernmentGatewayProvider*;.*services.test;",
    ScoverageKeys.coverageMinimum := 50,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

addCommandAlias("precommit", ";scalafmt;test:scalafmt;coverage;test;coverageReport")

