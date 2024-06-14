/*
 * Copyright 2024 HM Revenue & Customs
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
import uk.gov.hmrc.DefaultBuildSettings
import uk.gov.hmrc.DefaultBuildSettings.*
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "2.13.12"

lazy val TemplateTest = config("tt") extend Test

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin, SbtWeb)
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .settings((playSettings ++ scoverageSettings) *)
  .settings(scalaSettings *)
  .settings(PlayKeys.playDefaultPort := 9523)
  .settings(
    targetJvm := "jvm-11",
    libraryDependencies ++= compileDependencies ++ testDependencies
  )
  .settings(inConfig(TemplateTest)(Defaults.testSettings) *)
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
    uglifyOps := UglifyOps.singleFile,
    // prevent removal of unused code which generates warning errors due to use of third-party libs
    uglifyCompressOptions := Seq("unused=false", "dead_code=false"),
    pipelineStages := Seq(digest),
    // below line required to force asset pipeline to dev / operate rather than only prod
    Assets / pipelineStages := Seq(concat, uglify),
    // only compress files generated by concat
    uglify / includeFilter := GlobFilter("all-services*.js")
  )
  .settings(routesImport ++= Seq(
     "uk.gov.hmrc.play.bootstrap.binders.RedirectUrl"
  ))

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(DefaultBuildSettings.itSettings())
  .settings(libraryDependencies ++= testDependencies)
  .settings(Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.AllLibraryJars)

val appName = "voa-property-linking-frontend"

val playSettings: Seq[Setting[?]] = Seq(
  routesImport ++= Seq(
    "binders.propertylinks._",
    "binders.propertylinks.EvidenceChoices._",
    "models.upscan.FileStatus._",
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
    ScoverageKeys.coverageMinimumStmtTotal := 70,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    Test / unmanagedSourceDirectories := (Test / baseDirectory)(base => Seq(base / "test")).value,
    Test / parallelExecution := false,
  )
}

scalacOptions += "-Wconf:src=routes/.*:s"
scalacOptions += "-Wconf:cat=unused-imports&src=html/.*:s"
scalacOptions += "-Wconf:src=target/.*:s"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

ThisBuild / excludeDependencies ++= Seq(
  // As of Play 3.0, groupId has changed to org.playframework; exclude transitive dependencies to the old artifacts
  // Specifically affects play-json-extensions
  ExclusionRule(organization = "com.typesafe.play")
)

val bootstrapPlayVersion = "8.5.0"
val hmrcMongoVersion = "1.9.0"

lazy val compileDependencies = Seq(
  guice,
  filters,
  ws,
  "ai.x"                 %% "play-json-extensions"                  % "0.42.0",
  "com.google.guava"     % "guava"                                  % "18.0",
  "org.typelevel"        %% "cats-core"                             % "2.10.0",
  "uk.gov.hmrc"          %% "bootstrap-frontend-play-30"            % bootstrapPlayVersion,
  "uk.gov.hmrc"          %% "play-frontend-hmrc-play-30"            % "8.5.0",
  "uk.gov.hmrc"          %% "http-caching-client-play-30"           % "11.2.0",
  "uk.gov.hmrc"          %% "play-conditional-form-mapping-play-30" % "2.0.0",
  "uk.gov.hmrc.mongo"    %% "hmrc-mongo-play-30"                    % hmrcMongoVersion,
  "uk.gov.hmrc"          %% "uri-template"                          % "1.12.0",
  "uk.gov.hmrc"          %% "business-rates-values"                 % "3.0.0"
)

lazy val testDependencies = Seq(
  "uk.gov.hmrc"            %% "bootstrap-test-play-30"  % bootstrapPlayVersion % Test,
  "org.scalacheck"         %% "scalacheck"              % "1.18.0"             % Test,
  "org.scalatestplus"      %% "scalacheck-1-15"         % "3.2.11.0"           % Test,
  "org.pegdown"            % "pegdown"                  % "1.6.0"              % Test,
  "org.jsoup"              % "jsoup"                    % "1.17.2"             % Test,
  "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-30" % hmrcMongoVersion     % Test
)

addCommandAlias("precommit", ";scalafmt;test:scalafmt;it/test:scalafmt;coverage;test;it/test;coverageReport")