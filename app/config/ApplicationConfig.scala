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

package config

import java.util.Base64

import com.google.inject.{Inject, Singleton}
import controllers.routes
import play.api.Mode.Mode
import play.api.{Configuration, Environment, Play}
import uk.gov.hmrc.play.config.{RunMode, ServicesConfig}

@Singleton()
class ApplicationConfig @Inject()(override val runModeConfiguration: Configuration, environment: Environment) extends ServicesConfig {

  private def loadConfig(key: String) = runModeConfiguration.getString(key).getOrElse(throw new Exception(s"Missing configuration key: $key"))
  private def loadBooleanConfig(key: String) = runModeConfiguration.getString(key).fold(false)(_.toBoolean)

  lazy val baseUrl = if (mode == play.api.Mode.Prod) "" else "http://localhost:9523"

  def businessRatesValuationUrl(page: String): String = loadConfig("business-rates-valuation.url") + s"/$page"
  def businessRatesCheckUrl(page: String): String = loadConfig("business-rates-check.url") + s"/$page"
  def businessTaxAccountUrl(page: String): String = loadConfig("business-tax-account.url") + s"/$page"
  def newDashboardUrl(page: String): String = loadConfig("business-rates-dashboard-frontend.url") + s"/$page"
  def businessRatesCheckCaseSummaryUrl(page: String):String = loadConfig("business-rates-check-case-summary.url") + s"/$page"
  def businessRatesChallengeStartPageUrl(page: String) :String = loadConfig("business-rates-challenge-start-page.url") + s"/$page"

  lazy val helpGuideUrl = loadConfig("help-guide.url")

  lazy val ivBaseUrl = loadConfig("microservice.services.identity-verification.url")
  lazy val vmvUrl = loadConfig("vmv-frontend.url")
  lazy val ggSignInUrl: String = loadConfig("gg-sign-in.url")
  lazy val ggRegistrationUrl: String = loadConfig("gg-registration.url")
  lazy val ggContinueUrl: String = baseUrl + routes.Dashboard.home().url
  lazy val fileUploadUrl: String = loadConfig("file-upload-frontend.url")
  lazy val serviceUrl: String = loadConfig("voa-property-linking-frontend.url")
  lazy val checkUrl = loadConfig("microservice.services.business-rates-check-frontend.url")
  lazy val externalCaseManagementApiUrl :String = loadConfig("external-case-management-api.url")

  lazy val analyticsTagManagerCode = loadConfig("google-analytics.tag.managerCode")
  lazy val analyticsToken: String = loadConfig("google-analytics.token")
  lazy val analyticsHost: String = loadConfig("google-analytics.host")
  lazy val voaPersonID: String = loadConfig("google-analytics.dimensions.voaPersonId")
  lazy val loggedInUser:  Option[String] = runModeConfiguration.getString("google-analytics.dimensions.loggedInUser")
  lazy val isAgentLoggedIn:  Option[String] = runModeConfiguration.getString("google-analytics.dimensions.isAgentLoggedIn")
  lazy val pingdomToken: Option[String] = runModeConfiguration.getString("pingdom.performance.monitor.token")

  lazy val editNameEnabled: Boolean = loadBooleanConfig("featureFlags.editNameEnabled")
  lazy val ivEnabled: Boolean = loadBooleanConfig("featureFlags.ivEnabled")
  lazy val fileUploadEnabled: Boolean = loadBooleanConfig("featureFlags.fileUploadEnabled")
  lazy val downtimePageEnabled: Boolean = loadBooleanConfig("featureFlags.downtimePageEnabled")
  lazy val dvrEnabled: Boolean = loadBooleanConfig("featureFlags.dvrEnabled")

  lazy val stubEnrolment: Boolean = loadBooleanConfig("enrolment.useStub")

  lazy val bannerContent: Option[String] = runModeConfiguration.getString("encodedBannerContent").map(
    e => new String(Base64.getUrlDecoder.decode(e)))

  lazy val plannedImprovementsContent: Option[String] = runModeConfiguration.getString("plannedImprovementsContent").map(e =>
    new String(Base64.getUrlDecoder.decode(e)))

  override protected def mode: Mode = environment.mode
}

object ApplicationConfig

private case class ConfigMissing(key: String) extends Exception(s"Missing config for $key")
