/*
 * Copyright 2022 HM Revenue & Customs
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

import java.time.LocalDate
import scala.util.Try

@Singleton()
class ApplicationConfig @Inject()(configuration: Configuration) {

  protected def loadConfig(key: String): String =
    configuration.getOptional[String](key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  protected def loadBooleanConfig(key: String): Boolean =
    configuration.getOptional[String](key).fold(false)(_.toBoolean)

  protected def loadInt(key: String): Int =
    configuration.getOptional[Int](key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  // ISO_LOCAL_DATE format (e.g. 2007-12-03)
  protected def loadLocalDate(key: String): LocalDate =
    Try[LocalDate] {
      LocalDate.parse(loadConfig(key))
    }.getOrElse(
      throw new Exception(s"LocalDate value badly formatted for key: $key. Should be yyyy-MM-dd (e.g. 2007-04-01)"))

  def businessRatesValuationFrontendUrl(page: String): String = loadConfig("business-rates-valuation.url") + s"/$page"

  def businessRatesCheckUrl(page: String): String = loadConfig("business-rates-check.url") + s"/$page"

  def businessTaxAccountUrl(page: String): String = loadConfig("business-tax-account.url") + s"/$page"

  def dashboardUrl(page: String): String = loadConfig("business-rates-dashboard-frontend.url") + s"/$page"

  def yourDetailsUrl(page: String): String = loadConfig("business-rates-dashboard-frontend.url") + s"/$page"

  def businessRatesCheckCaseSummaryUrl(page: String): String =
    loadConfig("business-rates-check-case-summary.url") + s"/$page"

  def businessRatesChallengeUrl(page: String): String =
    loadConfig("business-rates-challenge-frontend.url") + s"/$page"

  lazy val helpGuideUrl = loadConfig("help-guide.url")

  lazy val ivBaseUrl = loadConfig("microservice.services.identity-verification.url")
  lazy val vmvUrl = loadConfig("vmv-frontend.url")
  lazy val valuationFrontendUrl = loadConfig("business-rates-valuation.url")
  lazy val basGatewaySignInUrl: String = loadConfig("bas-gateway-sign-in.url")
  lazy val ggRegistrationUrl: String = loadConfig("gg-registration.url")
  lazy val serviceUrl: String = loadConfig("voa-property-linking-frontend.url")
  lazy val checkUrl = loadConfig("microservice.services.business-rates-check-frontend.url")
  lazy val externalCaseManagementApiUrl: String = loadConfig("external-case-management-api.url")

  lazy val agentAppointDelay: Int = loadInt("agent.appoint.async.delay")

  // Google Analytics (GTM)
  // looks clunky, but spaces not allowed in app-config yaml, so construct the dimension here
  lazy val personIdDimension: String =
    s"VOA_person_ID (${configuration.get[String]("google-analytics.dimension.personId")})"
  lazy val loggedInDimension: String =
    s"Logged_in (${configuration.get[String]("google-analytics.dimension.loggedIn")})"
  lazy val ccaAgentDimension: String =
    s"CCA_Agent (${configuration.get[String]("google-analytics.dimension.ccaAgent")})"

  lazy val pingdomToken: Option[String] = configuration.getOptional[String]("pingdom.performance.monitor.token")

  lazy val earliestEnglishStartDate: LocalDate = loadLocalDate("property-linking.default.earliestEnglishStartDate")
  lazy val earliestWelshStartDate: LocalDate = loadLocalDate("property-linking.default.earliestWelshStartDate")

  lazy val ivEnabled: Boolean = loadBooleanConfig("featureFlags.ivEnabled")
  lazy val newRegistrationJourneyEnabled: Boolean = loadBooleanConfig("featureFlags.newRegistrationJourneyEnabled")
  lazy val signOutUrl =
    s"${loadConfig("sign-out.url")}?continue_url=${dashboardUrl("home")}&accountType=organisation&origin=voa"
  lazy val signOutTimeout = loadInt("sign-out.timeout")
  lazy val signOutCountdown = loadInt("sign-out.countdown")
  lazy val keepAliveUrl = loadConfig("sign-out.keep-alive-url")

  lazy val stubEnrolment: Boolean = loadBooleanConfig("enrolment.useStub")

  lazy val bannerContent: Option[String] =
    configuration.getOptional[String]("encodedBannerContent").map(e => new String(Base64.getUrlDecoder.decode(e)))

  lazy val plannedImprovementsContent: Option[String] = configuration
    .getOptional[String]("plannedImprovementsContent")
    .map(e => new String(Base64.getUrlDecoder.decode(e)))

}

private case class ConfigMissing(key: String) extends Exception(s"Missing config for $key")
