/*
 * Copyright 2018 HM Revenue & Customs
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
import actions.{AuthImpl, EnrolmentAuth, NonEnrolmentAuth}
import auditing.AuditingService
import auth.{GGAction, GGActionEnrolment, VoaAction}
import com.builtamont.play.pdf.PdfGenerator
import com.google.inject.AbstractModule
import com.google.inject.binder.ScopedBindingBuilder
import com.google.inject.name.Names
import com.typesafe.config.Config
import connectors.VPLAuthConnector
import controllers.IdentityVerification
import controllers.manageDetails.{Details, EnrolmentDetails, NonEnrolmentDetails}
import net.ceedubs.ficus.Ficus._
import play.api._
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.i18n.Messages.Implicits._
import play.api.mvc._
import play.modules.reactivemongo.ReactiveMongoComponent
import play.twirl.api.Html
import reactivemongo.api.DB
import repositories._
import services._
import services.iv.{IdentityVerificationService, IdentityVerificationServiceEnrolment, IdentityVerificationServiceNonEnrolment}
import uk.gov.hmrc.auth.core.PlayAuthConnector
import uk.gov.hmrc.circuitbreaker.CircuitBreakerConfig
import uk.gov.hmrc.http.HeaderNames
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.{AppName, ControllerConfig, ServicesConfig}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.bootstrap.DefaultFrontendGlobal
import uk.gov.hmrc.play.frontend.config.LoadAuditingConfig
import uk.gov.hmrc.play.frontend.filters.{FrontendAuditFilter, FrontendLoggingFilter, MicroserviceFilterSupport, RecoveryFilter}

object Global extends VPLFrontendGlobal

trait VPLFrontendGlobal extends DefaultFrontendGlobal {

  implicit lazy val appConfig = Play.current.injector.instanceOf[ApplicationConfig]
  implicit lazy val messageApi = Play.current.injector.instanceOf[MessagesApi]
  implicit lazy val messages = messageApi.preferred(Seq(Lang.defaultLang))

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: Request[_]): Html = {
    views.html.errors.error(pageTitle, heading, message)(request, messages, appConfig)
  }

  override def internalServerErrorTemplate(implicit request: Request[_]): Html = {
    views.html.errors.technicalDifficulties(extractErrorReference(request), getDateTime)
  }

  private def getDateTime: LocalDateTime = {
    val instant = Instant.ofEpochMilli(System.currentTimeMillis)
    LocalDateTime.ofInstant(instant, ZoneId.of("Europe/London"))
  }

  private def extractErrorReference(request: Request[_]): Option[String] = {
    request.headers.get(HeaderNames.xRequestId) map {
      _.split("-")(2)
    }
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

  override def controllerNeedsAuditing(controllerName: String): Boolean = ControllerConfiguration.paramsForController(controllerName).needsAuditing
}

object ControllerConfiguration extends ControllerConfig {
  lazy val controllerConfigs = Play.current.configuration.underlying.as[Config]("controllers")
}

class GuiceModule(environment: Environment,
                  configuration: Configuration) extends AbstractModule {
  def configure() = {
    bind(classOf[AuditingService]).toInstance(AuditingService)
    bind(classOf[DB]).toProvider(classOf[MongoDbProvider]).asEagerSingleton()
    bind(classOf[SessionRepo]).annotatedWith(Names.named("propertyLinkingSession")).to(classOf[PropertyLinkingSessionRepository])
    bind(classOf[SessionRepo]).annotatedWith(Names.named("agentAppointmentSession")).to(classOf[AgentAppointmentSessionRepository])
    bind(classOf[SessionRepo]).annotatedWith(Names.named("personSession")).to(classOf[PersonalDetailsSessionRepository])
    bind(classOf[SessionRepo]).annotatedWith(Names.named("registrationSession")).to(classOf[RegistrationDetailsSessionRepository])
    bind(classOf[WSHttp]).to(classOf[VPLHttp])
    enrolment()
    bind(classOf[Clock]).toInstance(Clock.systemUTC())
    bind(classOf[uk.gov.hmrc.auth.core.AuthConnector]).to(classOf[AuthConnectorImpl])
    bind(classOf[AuthConnector]).to(classOf[VPLAuthConnector])
    bind(classOf[CircuitBreakerConfig]).toProvider(classOf[CircuitBreakerConfigProvider]).asEagerSingleton()
    bind(classOf[PdfGenerator]).toInstance(new PdfGenerator(environment))
  }

  private def enrolment(): ScopedBindingBuilder = {
    if (configuration.getString("featureFlags.enrolment").getOrElse(throw ConfigMissing("featureFlags.enrolment")).toBoolean) {
      bind(classOf[Details]).to(classOf[EnrolmentDetails])
      bind(classOf[VoaAction]).to(classOf[GGActionEnrolment])
      bind(classOf[AuthImpl]).to(classOf[EnrolmentAuth])
      bind(classOf[ManageDetails]).to(classOf[ManageDetailsWithEnrolments])
      bind(classOf[IdentityVerificationService]).to(classOf[IdentityVerificationServiceEnrolment])
    } else {
      bind(classOf[Details]).to(classOf[NonEnrolmentDetails])
      bind(classOf[VoaAction]).to(classOf[GGAction])
      bind(classOf[AuthImpl]).to(classOf[NonEnrolmentAuth])
      bind(classOf[ManageDetails]).to(classOf[ManageDetailsWithoutEnrolments])
      bind(classOf[IdentityVerificationService]).to(classOf[IdentityVerificationServiceNonEnrolment])

    }
  }
}

class AuthConnectorImpl @Inject()(val http: WSHttp) extends PlayAuthConnector with ServicesConfig {
  override val serviceUrl: String = baseUrl("auth")
}

class MongoDbProvider @Inject()(reactiveMongoComponent: ReactiveMongoComponent) extends Provider[DB] {
  def get = reactiveMongoComponent.mongoConnector.db()
}

class CircuitBreakerConfigProvider @Inject()(config: Configuration) extends Provider[CircuitBreakerConfig] {
  override def get(): CircuitBreakerConfig = {
    val serviceName = config.getString("circuitBreaker.serviceName").getOrElse("file-upload-frontend")
    val numberOfCallsToTriggerChange = config.getInt("circuitBreaker.numberOfCallsToTriggerStateChange")
    val unavailablePeriod = config.getInt("circuitBreaker.unavailablePeriodDuration")
    val unstablePeriod = config.getInt("circuitBreaker.unstablePeriodDuration")

    CircuitBreakerConfig(serviceName, numberOfCallsToTriggerChange, unavailablePeriod, unstablePeriod)
  }
}