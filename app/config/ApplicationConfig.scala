/*
 * Copyright 2023 HM Revenue & Customs
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
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl.idFunctor
import uk.gov.hmrc.play.bootstrap.binders.{AbsoluteWithHostnameFromAllowlist, OnlyRelative, RedirectUrl}
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

  def businessRatesChallengeUrl(page: String): String =
    loadConfig("business-rates-challenge-frontend.url") + s"/$page"

  lazy val appName: String = loadConfig("appName")

  lazy val ivBaseUrl = loadConfig("microservice.services.identity-verification.url")
  lazy val vmvUrl = loadConfig("vmv-frontend.url")
  lazy val basGatewaySignInUrl: String = loadConfig("bas-gateway-sign-in.url")
  lazy val attachmentsUrl: String = loadConfig("business-rates-attachments.url")
  lazy val ggRegistrationUrl: String = loadConfig("gg-registration.url")
  lazy val serviceUrl: String = loadConfig("voa-property-linking-frontend.url")
  lazy val upliftCompletionUrl: String = loadConfig("upliftCompletion.url")
  lazy val upliftFailureUrl: String = loadConfig("upliftFailure.url")
  lazy val identityVerificationUrl: String = loadConfig("microservice.services.identity-verification-frontend.url")

  lazy val agentAppointDelay: Int = loadInt("agent.appoint.async.delay")

  // Google Analytics (GTM)
  // looks clunky, but spaces not allowed in app-config yaml, so construct the dimension here
  lazy val personIdDimension: String =
    s"VOA_person_ID (${configuration.get[String]("google-analytics.dimension.personId")})"
  lazy val loggedInDimension: String =
    s"Logged_in (${configuration.get[String]("google-analytics.dimension.loggedIn")})"
  lazy val ccaAgentDimension: String =
    s"CCA_Agent (${configuration.get[String]("google-analytics.dimension.ccaAgent")})"

  lazy val earliestEnglishStartDate: LocalDate = loadLocalDate("property-linking.default.earliestEnglishStartDate")
  lazy val earliestWelshStartDate: LocalDate = loadLocalDate("property-linking.default.earliestWelshStartDate")

  lazy val ivEnabled: Boolean = loadBooleanConfig("featureFlags.ivEnabled")
  lazy val newRegistrationJourneyEnabled: Boolean = loadBooleanConfig("featureFlags.newRegistrationJourneyEnabled")
  lazy val agentListYears: Boolean = loadBooleanConfig("feature-switch.agentListYears.enabled")
  lazy val ivUpliftEnabled: Boolean = loadBooleanConfig("feature-switch.ivUplift.enabled")
  lazy val signOutUrl =
    s"${loadConfig("sign-out.url")}?continue_url=${dashboardUrl("home")}&accountType=organisation&origin=voa"

  lazy val stubEnrolment: Boolean = loadBooleanConfig("enrolment.useStub")

  lazy val bannerContent: Option[String] =
    configuration.getOptional[String]("encodedBannerContent").map(e => new String(Base64.getUrlDecoder.decode(e)))

  lazy val bannerContentWelsh: Option[String] =
    configuration.getOptional[String]("encodedBannerContentWelsh").map(e => new String(Base64.getUrlDecoder.decode(e)))

  lazy val plannedImprovementsContent: Option[String] = configuration
    .getOptional[String]("plannedImprovementsContent")
    .map(e => new String(Base64.getUrlDecoder.decode(e)))

  val default2017AssessmentEndDate = LocalDate.of(2023, 3, 31)

  lazy val environmentHost: String = configuration.get[String]("environment-base.host")

  def safeRedirect(url: RedirectUrl): String =
    url.get(AbsoluteWithHostnameFromAllowlist(environmentHost) | OnlyRelative).url
}

private case class ConfigMissing(key: String) extends Exception(s"Missing config for $key")
