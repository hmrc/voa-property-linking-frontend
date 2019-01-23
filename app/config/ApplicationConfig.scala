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
import play.api.{Configuration, Play}
import uk.gov.hmrc.play.config.inject.RunMode

@Singleton()
class ApplicationConfig @Inject()(configuration: Configuration, runMode: RunMode) {
  def baseUrl: String = if (runMode.env == "Prod") "" else "http://localhost:9523"
  def businessRatesValuationUrl(page: String): String = getConfig("business-rates-valuation.url") + s"/$page"
  def businessRatesCheckUrl(page: String): String = getConfig("business-rates-check.url") + s"/$page"
  def businessTaxAccountUrl(page: String): String = getConfig("business-tax-account.url") + s"/$page"
  def newDashboardUrl(page: String): String = getConfig("business-rates-dashboard-frontend.url") + s"/$page"
  def businessRatesCheckCaseSummaryUrl(page: String):String = getConfig("business-rates-check-case-summary.url") + s"/$page"
  def businessRatesChallengeStartPageUrl(page: String) :String = getConfig("business-rates-challenge-start-page.url") + s"/$page"

  lazy val helpGuideUrl = getConfig("help-guide.url")

  lazy val ivBaseUrl = getConfig("microservice.services.identity-verification.url")
  lazy val vmvUrl: String = getConfig("vmv-frontend.url")
  lazy val ggSignInUrl: String = getConfig("gg-sign-in.url")
  lazy val ggRegistrationUrl: String = getConfig("gg-registration.url")
  lazy val ggContinueUrl: String = baseUrl + routes.Dashboard.home().url
  lazy val fileUploadUrl: String = getConfig("file-upload-frontend.url")
  lazy val serviceUrl: String = getConfig("voa-property-linking-frontend.url")
  lazy val checkUrl = getConfig("microservice.services.business-rates-check-frontend.url")
  lazy val externalCaseManagementApiUrl :String = getConfig("external-case-management-api.url")

  lazy val analyticsTagManagerCode = getConfig("google-analytics.tag.managerCode")
  lazy val analyticsToken: String = getConfig("google-analytics.token")
  lazy val analyticsHost: String = getConfig("google-analytics.host")
  lazy val voaPersonID: String = getConfig("google-analytics.dimensions.voaPersonId")
  lazy val loggedInUser:  Option[String] = getOptionalConfig("google-analytics.dimensions.loggedInUser")
  lazy val isAgentLoggedIn:  Option[String] = getOptionalConfig("google-analytics.dimensions.isAgentLoggedIn")
  lazy val pingdomToken: Option[String] = getOptionalConfig("pingdom.performance.monitor.token")

  lazy val editNameEnabled: Boolean = getConfig("featureFlags.editNameEnabled").toBoolean
  lazy val ivEnabled: Boolean = getConfig("featureFlags.ivEnabled").toBoolean
  lazy val fileUploadEnabled: Boolean = getConfig("featureFlags.fileUploadEnabled").toBoolean
  lazy val downtimePageEnabled: Boolean = getConfig("featureFlags.downtimePageEnabled").toBoolean
  lazy val dvrEnabled: Boolean = getConfig("featureFlags.dvrEnabled").toBoolean

  lazy val stubEnrolment: Boolean = getConfig("enrolment.useStub").toBoolean

  lazy val bannerContent: Option[String] = configuration.getString("encodedBannerContent") map { e =>
    new String(Base64.getUrlDecoder.decode(e))
  }

  lazy val plannedImprovementsContent: Option[String] = configuration.getString("plannedImprovementsContent") map { e =>
    new String(Base64.getUrlDecoder.decode(e))
  }

  private def getConfig(key: String) = configuration.getString(key).getOrElse(throw ConfigMissing(key))
  private def getOptionalConfig(key: String) = configuration.getString(key)

}

object ApplicationConfig

private case class ConfigMissing(key: String) extends Exception(s"Missing config for $key")
