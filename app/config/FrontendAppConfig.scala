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

import play.api.Play
import play.api.Play.{configuration, current}
import uk.gov.hmrc.play.config.ServicesConfig
import ConfigHelper._

object FrontendAppConfig extends ServicesConfig {
  lazy val appName = mustGetConfigString(Play.current, "appName")

  private val contactHost = getConfigString(configuration, s"contact-frontend.host").getOrElse("")
  private val contactFormServiceIdentifier = "property-linking"

  lazy val analyticsToken = mustGetConfigString(Play.current, s"google-analytics.token")
  lazy val analyticsHost = mustGetConfigString(Play.current, s"google-analytics.host")
}
