/*
 * Copyright 2016 HM Revenue & Customs
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

import com.typesafe.config.Config
import play.api.mvc.Request
import play.api.{Application, Configuration, Play}
import play.twirl.api.Html
import uk.gov.hmrc.play.audit.filters.FrontendAuditFilter
import uk.gov.hmrc.play.audit.http.config.LoadAuditingConfig
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.{AppName, ControllerConfig, RunMode, ServicesConfig}
import uk.gov.hmrc.play.frontend.bootstrap.DefaultFrontendGlobal
import uk.gov.hmrc.play.http.logging.filters.FrontendLoggingFilter
import net.ceedubs.ficus.Ficus._
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.http.ws.{WSDelete, WSGet, WSPost, WSPut}
import uk.gov.hmrc.play.http.{HttpDelete, HttpGet, HttpPut}

object Global extends DefaultFrontendGlobal {
  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: Request[_]): Html = views.html.errors.error()

  def auditConnector: uk.gov.hmrc.play.audit.http.connector.AuditConnector = AuditServiceConnector

  override def microserviceMetricsConfig(implicit app: Application): Option[Configuration] = metricsConfig

  override def loggingFilter: FrontendLoggingFilter = LoggingFilter

  override def frontendAuditFilter: FrontendAuditFilter = AuditFilter

  lazy val config = Play.current.configuration
  def metricsConfig: Option[Configuration] = config.getConfig("metrics")
}

object AuditServiceConnector extends AuditConnector {
  override lazy val auditingConfig = LoadAuditingConfig("auditing")
}

object LoggingFilter extends FrontendLoggingFilter {
  override def controllerNeedsLogging(controllerName: String): Boolean = ControllerConfiguration.paramsForController(controllerName).needsLogging
}

object AuditFilter extends FrontendAuditFilter with AppName {
  override lazy val maskedFormFields = Seq.empty
  override lazy val applicationPort = None
  override lazy val auditConnector = AuditServiceConnector

  override def controllerNeedsAuditing(controllerName: String): Boolean = false
}

object ControllerConfiguration extends ControllerConfig {
  lazy val controllerConfigs = Play.current.configuration.underlying.as[Config]("controllers")
}

object Environment extends uk.gov.hmrc.play.config.RunMode {
  def isDev = env == "Dev"
}

object Keystore extends SessionCache with AppName with ServicesConfig {
  override def defaultSource: String = appName
  override def baseUri: String = baseUrl("cachable.session-cache")
  override def domain: String = getConfString("cachable.session-cache.domain", throw new Exception("No config setting for cache domain"))
  override def http: HttpGet with HttpPut with HttpDelete = WSHttp
}

object WSHttp extends WSGet with WSPut with WSDelete with WSPost with HttpAuditing with AppName with RunMode {
  override val hooks = Seq(AuditingHook)
  override def auditConnector = AuditServiceConnector
}