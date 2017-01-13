/*
 * Copyright 2017 HM Revenue & Customs
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

import play.api.Play._
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.config.ServicesConfig

object ApplicationConfig extends RunMode with ServicesConfig {

  def baseUrl = if (env == "Prod") "" else "http://localhost:9523"

  val ggSignInUrl = getConfig("gg-sign-in.url")
  val ggRegistrationUrl = getConfig("gg-registration.url")
  val ggContinueUrl = baseUrl + controllers.routes.Dashboard.home().url
  val betaLoginRequired = getConfig("featureFlags.betaLoginRequired").toBoolean
  val betaLoginPassword = getConfig("betaLoginPassword")
  def businessRatesValuationUrl(page: String) = getConfig("business-rates-valuation.url") + s"/$page"


  private def getConfig(key: String) = configuration.getString(key).getOrElse(throw ConfigMissing(key))
}

private case class ConfigMissing(key: String) extends Exception(s"Missing config for $key")
