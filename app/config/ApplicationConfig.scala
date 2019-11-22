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
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.{RunMode, ServicesConfig}

@Singleton()
class ApplicationConfig @Inject()(configuration: Configuration, runMode: RunMode) extends ServicesConfig(configuration, runMode) {

  protected def loadConfig(key: String): String = configuration.getOptional[String](key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  protected def loadBooleanConfig(key: String): Boolean = configuration.getOptional[String](key).fold(false)(_.toBoolean)

  protected def loadInt(key: String): Int = configuration.getOptional[Int](key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  def businessRatesValuationFrontendUrl(page: String): String = loadConfig("business-rates-valuation.url") + s"/$page"

  def businessRatesCheckUrl(page: String): String = loadConfig("business-rates-check.url") + s"/$page"

  def businessTaxAccountUrl(page: String): String = loadConfig("business-tax-account.url") + s"/$page"

  def newDashboardUrl(page: String): String = loadConfig("business-rates-dashboard-frontend.url") + s"/$page"

  def yourDetailsUrl(page: String): String = loadConfig("business-rates-dashboard-frontend.url") + s"/$page"

  def businessRatesCheckCaseSummaryUrl(page: String): String = loadConfig("business-rates-check-case-summary.url") + s"/$page"

  def businessRatesChallengeStartPageUrl(page: String): String = loadConfig("business-rates-challenge-start-page.url") + s"/$page"

  lazy val helpGuideUrl = loadConfig("help-guide.url")

  lazy val ivBaseUrl = loadConfig("microservice.services.identity-verification.url")
  lazy val vmvUrl = loadConfig("vmv-frontend.url")
  lazy val ggSignInUrl: String = loadConfig("gg-sign-in.url")
  lazy val ggRegistrationUrl: String = loadConfig("gg-registration.url")
  lazy val fileUploadUrl: String = loadConfig("file-upload-frontend.url")
  lazy val serviceUrl: String = loadConfig("voa-property-linking-frontend.url")
  lazy val checkUrl = loadConfig("microservice.services.business-rates-check-frontend.url")
  lazy val externalCaseManagementApiUrl: String = loadConfig("external-case-management-api.url")

  lazy val agentAppointDelay: Int = loadInt("agent.appoint.async.delay")

  lazy val analyticsTagManagerCode = loadConfig("google-analytics.tag.managerCode")
  lazy val analyticsToken: String = loadConfig("google-analytics.token")
  lazy val analyticsHost: String = loadConfig("google-analytics.host")
  lazy val voaPersonID: String = loadConfig("google-analytics.dimensions.voaPersonId")
  lazy val loggedInUser: Option[String] = configuration.getOptional[String]("google-analytics.dimensions.loggedInUser")
  lazy val isAgentLoggedIn: Option[String] = configuration.getOptional[String]("google-analytics.dimensions.isAgentLoggedIn")
  lazy val pingdomToken: Option[String] = configuration.getOptional[String]("pingdom.performance.monitor.token")

  lazy val editNameEnabled: Boolean = loadBooleanConfig("featureFlags.editNameEnabled")
  lazy val ivEnabled: Boolean = loadBooleanConfig("featureFlags.ivEnabled")
  lazy val fileUploadEnabled: Boolean = loadBooleanConfig("featureFlags.fileUploadEnabled")
  lazy val downtimePageEnabled: Boolean = loadBooleanConfig("featureFlags.downtimePageEnabled")
  lazy val dvrEnabled: Boolean = loadBooleanConfig("featureFlags.dvrEnabled")
  lazy val externalDetailedValuationApiEnabled: Boolean = loadBooleanConfig("featureFlags.externalDetailedValuationApiEnabled")

  lazy val stubEnrolment: Boolean = loadBooleanConfig("enrolment.useStub")

  lazy val bannerContent: Option[String] = configuration.getOptional[String]("encodedBannerContent").map(
    e => new String(Base64.getUrlDecoder.decode(e)))

  lazy val plannedImprovementsContent: Option[String] = configuration.getOptional[String]("plannedImprovementsContent").map(e =>
    new String(Base64.getUrlDecoder.decode(e)))

  val baseUrl: String = if (Set("Dev", "Test").contains(runMode.env)) "http://localhost:9523" else ""

}

private case class ConfigMissing(key: String) extends Exception(s"Missing config for $key")
