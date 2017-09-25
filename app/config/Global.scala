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

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import javax.inject.{Inject, Provider}

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import com.typesafe.config.Config
import connectors.VPLAuthConnector
import net.ceedubs.ficus.Ficus._
import play.api.Play.current
import play.api._
import play.api.i18n.Messages.Implicits._
import play.api.mvc._
import play.modules.reactivemongo.ReactiveMongoComponent
import play.twirl.api.Html
import reactivemongo.api.DB
import repositories.{AgentAppointmentSessionRepository, PersonalDetailsSessionRepository, PropertyLinkingSessionRepository, SessionRepo}
import uk.gov.hmrc.circuitbreaker.CircuitBreakerConfig
import uk.gov.hmrc.play.audit.filters.FrontendAuditFilter
import uk.gov.hmrc.play.audit.http.config.LoadAuditingConfig
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.{AppName, ControllerConfig}
import uk.gov.hmrc.play.filters.{MicroserviceFilterSupport, RecoveryFilter}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.bootstrap.DefaultFrontendGlobal
import uk.gov.hmrc.play.http.HeaderNames
import uk.gov.hmrc.play.http.logging.filters.FrontendLoggingFilter
import uk.gov.hmrc.play.http.ws.WSHttp


object Global extends VPLFrontendGlobal

trait VPLFrontendGlobal extends DefaultFrontendGlobal {

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: Request[_]): Html = {
    views.html.errors.error(pageTitle, heading, message)(request, applicationMessages)
  }

  override def internalServerErrorTemplate(implicit request: Request[_]): Html = {
    views.html.errors.technicalDifficulties(extractErrorReference(request), getDateTime)
  }

  private def getDateTime: LocalDateTime = {
    val instant = Instant.ofEpochMilli(System.currentTimeMillis)
    LocalDateTime.ofInstant(instant, ZoneId.of("Europe/London"))
  }

  private def extractErrorReference(request: Request[_]): Option[String] = {
    request.headers.get(HeaderNames.xRequestId) map { _.split("-")(2) }
  }

  def auditConnector: uk.gov.hmrc.play.audit.http.connector.AuditConnector = AuditServiceConnector

  override def microserviceMetricsConfig(implicit app: Application): Option[Configuration] = app.configuration.getConfig("metrics")

  override def loggingFilter: FrontendLoggingFilter = LoggingFilter

  override def frontendAuditFilter: FrontendAuditFilter = AuditFilter

  override def filters: Seq[EssentialFilter] = super.filters.filterNot(_ == RecoveryFilter)
}

object AuditServiceConnector extends AuditConnector {
  override lazy val auditingConfig = LoadAuditingConfig("auditing")
}

object LoggingFilter extends FrontendLoggingFilter with MicroserviceFilterSupport {
  override def controllerNeedsLogging(controllerName: String): Boolean = ControllerConfiguration.paramsForController(controllerName).needsLogging
}

object AuditFilter extends FrontendAuditFilter with MicroserviceFilterSupport with AppName {
  override lazy val maskedFormFields = Seq.empty
  override lazy val applicationPort = None
  override lazy val auditConnector = AuditServiceConnector

  override def controllerNeedsAuditing(controllerName: String): Boolean = false
}

object ControllerConfiguration extends ControllerConfig {
  lazy val controllerConfigs = Play.current.configuration.underlying.as[Config]("controllers")
}

class GuiceModule(environment: Environment,
                  configuration: Configuration) extends AbstractModule {
  def configure() = {
    bind(classOf[DB]).toProvider(classOf[MongoDbProvider]).asEagerSingleton()
    bind(classOf[SessionRepo]).annotatedWith(Names.named("propertyLinkingSession")).to(classOf[PropertyLinkingSessionRepository])
    bind(classOf[SessionRepo]).annotatedWith(Names.named("agentAppointmentSession")).to(classOf[AgentAppointmentSessionRepository])
    bind(classOf[SessionRepo]).annotatedWith(Names.named("personSession")).to(classOf[PersonalDetailsSessionRepository])
    bind(classOf[WSHttp]).to(classOf[VPLHttp])
    bind(classOf[Clock]).toInstance(Clock.systemUTC())
    bind(classOf[AuthConnector]).to(classOf[VPLAuthConnector])
    bind(classOf[CircuitBreakerConfig]).toProvider(classOf[CircuitBreakerConfigProvider]).asEagerSingleton()
  }
}

class MongoDbProvider @Inject() (reactiveMongoComponent: ReactiveMongoComponent) extends Provider[DB] {
  def get = reactiveMongoComponent.mongoConnector.db()
}

class CircuitBreakerConfigProvider @Inject() (config: Configuration) extends Provider[CircuitBreakerConfig] {
  override def get(): CircuitBreakerConfig = {
    val serviceName = config.getString("circuitBreaker.serviceName").getOrElse("file-upload-frontend")
    val numberOfCallsToTriggerChange = config.getInt("circuitBreaker.numberOfCallsToTriggerStateChange")
    val unavailablePeriod = config.getInt("circuitBreaker.unavailablePeriodDuration")
    val unstablePeriod = config.getInt("circuitBreaker.unstablePeriodDuration")

    CircuitBreakerConfig(serviceName, numberOfCallsToTriggerChange, unavailablePeriod, unstablePeriod)
  }
}