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
    libraryDependencies ++= compileDependencies ++ testDependencies
  )
  .settings(inConfig(TemplateTest)(Defaults.testSettings): _*)
  .configs(IntegrationTest)
  .settings(
    inConfig(IntegrationTest)(Defaults.itSettings),
    IntegrationTest / Keys.fork := false,
    IntegrationTest / unmanagedSourceDirectories := (IntegrationTest / baseDirectory)(base =>
    Seq(base / "it")).value,
    addTestReportOption(IntegrationTest, "int-test-reports"),
    IntegrationTest / testGrouping := oneForkedJvmPerTest((IntegrationTest / definedTests).value),
    IntegrationTest / parallelExecution := false
  )
  .settings(scalaVersion := "2.13.8")
  .settings(
    TwirlKeys.templateImports ++= Seq(
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.govukfrontend.views.html.components.implicits._"
    )
  )
  .settings(update / evictionWarningOptions := EvictionWarningOptions.default
    .withWarnTransitiveEvictions(false)
    .withWarnDirectEvictions(false)
    .withWarnScalaVersionEviction(false))
  .settings(
     // concatenate js
    Concat.groups := Seq(
      "javascripts/all-services.js" -> group((baseDirectory.value / "app" / "assets" / "javascripts" / "src" * "*.js"))
    ),
    // prevent removal of unused code which generates warning errors due to use of third-party libs
    uglifyCompressOptions := Seq("unused=false", "dead_code=false"),
    pipelineStages := Seq(digest),
    // below line required to force asset pipeline to dev / operate rather than only prod
    Assets / pipelineStages := Seq(concat,uglify),
    // only compress files generated by concat
    uglify / includeFilter := GlobFilter("all-services*.js")
  )

def oneForkedJvmPerTest(tests: Seq[TestDefinition]) = tests.map { test =>
  Group(test.name, Seq(test), SubProcess(ForkOptions().withRunJVMOptions(Vector(s"-Dtest.name=${test.name}"))))
}

val appName = "voa-property-linking-frontend"

val playSettings: Seq[Setting[_]] = Seq(
  routesImport ++= Seq(
    "binders.propertylinks._",
    "binders.propertylinks.EvidenceChoices._",
    "binders.propertylinks.ClaimPropertyReturnToPage._",
    "binders.propertylinks.ExternalPropertyLinkManagementSortField._",
    "binders.pagination._",
    "models.SortOrder",
    "models.messages.MessagePagination",
    "models.searchApi.AgentPropertiesParameters",
    "models.ClientDetails"
  )
)

lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")

lazy val scoverageSettings = {
  // Semicolon-separated list of regexs matching classes to exclude
  import scoverage.ScoverageKeys
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;.*Reverse.*;controllers.test.*;connectors.test.*;views.*;config.*;poc.view.*;poc.config.*;.*(AuthService|BuildInfo|Routes).*;.*models.*;.*modules.*;.*utils.Conditionals.*;.*auth.GovernmentGatewayProvider*;.*services.test;",
    ScoverageKeys.coverageMinimum := 70,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    Test / unmanagedSourceDirectories := (Test / baseDirectory)(base => Seq(base / "test")).value,
    Test / parallelExecution := false
  )
}

scalacOptions += "-Wconf:src=routes/.*:s"
scalacOptions += "-Wconf:cat=unused-imports&src=html/.*:s"
scalacOptions += "-Wconf:src=target/.*:s"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")


lazy val compileDependencies = Seq(
  guice,
  filters,
  ws,
  "ai.x"                 %% "play-json-extensions"          % "0.42.0",
  "com.codahale.metrics" %  "metrics-graphite"              % "3.0.1",
  "com.google.guava"     %  "guava"                         % "18.0",
  "org.typelevel"        %% "cats-core"                     % "2.6.1",
  "uk.gov.hmrc"          %% "bootstrap-frontend-play-28"    % "5.21.0",
  "uk.gov.hmrc"          %% "play-frontend-hmrc"            % "3.34.0-play-28",
  "uk.gov.hmrc"          %% "http-caching-client"           % "9.6.0-play-28",
  "uk.gov.hmrc"          %% "play-conditional-form-mapping" % "1.12.0-play-28",
  "uk.gov.hmrc.mongo"    %% "hmrc-mongo-play-28"            % "0.74.0",
  "uk.gov.hmrc"          %% "uri-template"                  % "1.11.0"
)

lazy val testDependencies = Seq(
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0"         % Test,
  "org.scalatest"          %% "scalatest"          % "3.0.8"         % Test,
  "org.scalacheck"         %% "scalacheck"         % "1.14.0"        % Test,
  "org.scalatestplus"      %% "scalacheck-1-15"    % "3.2.10.0"      % Test,
  "org.pegdown"            %  "pegdown"            % "1.6.0"         % Test,
  "org.jsoup"              %  "jsoup"              % "1.12.1"        % Test,
  "org.mockito"            %  "mockito-core"       % "2.27.0"        % Test,
  "org.scalatestplus"      %% "mockito-3-4"        % "3.2.9.0"       % Test,
  "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-28" % "0.74.0"   % Test,
  "com.vladsch.flexmark"   %  "flexmark-all"       % "0.35.10"       % Test
)

addCommandAlias("precommit", ";scalafmt;test:scalafmt;coverage;test;coverageReport")
