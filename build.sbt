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

import sbt.Tests.{Group, SubProcess}
import uk.gov.hmrc.DefaultBuildSettings._
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.versioning.SbtGitVersioning
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

lazy val TemplateTest = config("tt") extend Test
lazy val TemplateItTest = config("tit") extend IntegrationTest

lazy val microservice = Project(appName, file("."))
  .enablePlugins(
    play.sbt.PlayScala,
    SbtAutoBuildPlugin,
    SbtGitVersioning,
    SbtDistributablesPlugin,
    SbtWeb)
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .settings(playSettings ++ scoverageSettings: _*)
  .settings(scalaSettings: _*)
  .settings(publishingSettings: _*)
  .settings(PlayKeys.playDefaultPort := 9523)
  .settings(majorVersion := 0)
  .settings(
    targetJvm := "jvm-1.8",
    libraryDependencies ++= compileDependencies ++ testDependencies,
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.7.0" cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % "1.7.0" % Provided cross CrossVersion.full
    ),
    parallelExecution in Test := false,
    fork in Test := true,
    testGrouping := oneForkedJvmPerTest((definedTests in Test).value)
  )
  .settings(scalaVersion := "2.12.12")
  .settings(inConfig(TemplateTest)(Defaults.testSettings): _*)
  .configs(IntegrationTest)
  .settings(inConfig(TemplateItTest)(Defaults.itSettings): _*)
  .settings(
    Keys.fork in IntegrationTest := false,
    unmanagedSourceDirectories in IntegrationTest := { (baseDirectory in IntegrationTest)(base => Seq(base / "it")) }.value,
    addTestReportOption(IntegrationTest, "int-test-reports"),
    parallelExecution in IntegrationTest := false
  )
  .settings(resolvers += "hmrc-releases" at "https://artefacts.tax.service.gov.uk/artifactory/hmrc-releases/")
  .settings(
    TwirlKeys.templateImports ++= Seq(
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.govukfrontend.views.html.helpers._"
    )
  )
  .settings(evictionWarningOptions in update := EvictionWarningOptions.default
    .withWarnTransitiveEvictions(false)
    .withWarnDirectEvictions(false)
    .withWarnScalaVersionEviction(false))

def oneForkedJvmPerTest(tests: Seq[TestDefinition]) = tests.map { test =>
  Group(test.name, Seq(test), SubProcess(ForkOptions().withRunJVMOptions(Vector(s"-Dtest.name=${test.name}"))))
}

val appName = "voa-property-linking-frontend"

val playSettings: Seq[Setting[_]] = Seq(
  routesImport ++= Seq(
    "binders.propertylinks._",
    "binders.propertylinks.EvidenceChoices._",
    "binders.propertylinks.ExternalPropertyLinkManagementSortField._",
    "binders.pagination._",
    "models.SortOrder",
    "models.messages.MessagePagination",
    "models.searchApi.AgentPropertiesParameters",
    "models.ClientDetails"
  ),
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
    ScoverageKeys.coverageExcludedPackages := "<empty>;.*Reverse.*;controllers.test.*;connectors.test.*;views.*;config.*;poc.view.*;poc.config.*;.*(AuthService|BuildInfo|Routes).*;.*models.*;.*modules.*;.*utils.Conditionals.*;.*auth.GovernmentGatewayProvider*;.*services.test;",
    ScoverageKeys.coverageMinimum := 70,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

// silence all warnings on autogenerated files
scalacOptions += "-P:silencer:pathFilters=target/.*"
// Make sure you only exclude warnings for the project directories, i.e. make builds reproducible
scalacOptions += s"-P:silencer:sourceRoots=${baseDirectory.value.getCanonicalPath}"
scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation", "-feature")


lazy val compileDependencies = Seq(
  guice,
  filters,
  ws,
  "ai.x"                 %% "play-json-extensions"          % "0.10.0",
  "com.codahale.metrics" %  "metrics-graphite"              % "3.0.1",
  "com.google.guava"     %  "guava"                         % "18.0",
  "org.typelevel"        %% "cats-core"                     % "1.6.1",
  "uk.gov.hmrc"          %% "bootstrap-frontend-play-27"    % "5.11.0",
  "uk.gov.hmrc"          %% "govuk-template"                % "5.61.0-play-27",
  "uk.gov.hmrc"          %% "play-frontend-hmrc"            % "0.76.0-play-27",
  "uk.gov.hmrc"          %% "play-ui"                       % "8.20.0-play-27",
  "uk.gov.hmrc"          %% "http-caching-client"           % "9.2.0-play-27",
  "uk.gov.hmrc"          %% "mongo-lock"                    % "6.24.0-play-27",
  "uk.gov.hmrc"          %% "play-conditional-form-mapping" % "1.5.0-play-27",
  "uk.gov.hmrc"          %% "play-whitelist-filter"         % "3.4.0-play-27",
  "uk.gov.hmrc"          %% "reactive-circuit-breaker"      % "3.5.0",
  "uk.gov.hmrc"          %% "simple-reactivemongo"          % "7.31.0-play-27",
  "uk.gov.hmrc"          %% "uri-template"                  % "1.7.0"
)

lazy val testDependencies = Seq(
  "uk.gov.hmrc"            %% "hmrctest"           % "3.9.0-play-26" % Test,
  "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3"         % Test,
  "org.scalatest"          %% "scalatest"          % "3.0.8"         % Test,
  "org.scalacheck"         %% "scalacheck"         % "1.13.4"        % Test,
  "org.pegdown"            %  "pegdown"            % "1.6.0"         % "test,it",
  "org.jsoup"              %  "jsoup"              % "1.12.1"        % Test,
  "org.mockito"            %  "mockito-core"       % "2.27.0"        % Test
)

addCommandAlias("precommit", ";scalafmt;test:scalafmt;coverage;test;coverageReport")
