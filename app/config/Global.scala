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

import java.time.{Clock, Instant, LocalDateTime, ZoneId}

import actions.{Auth, VoaAuth}
import auditing.AuditingService
import auth.{GgAction, VoaAction}
import com.builtamont.play.pdf.PdfGenerator
import com.google.inject.AbstractModule
import com.google.inject.name.Names
import com.typesafe.config.Config
import connectors.VPLAuthConnector
import controllers.manageDetails.{Details, VoaDetails}
import javax.inject.{Inject, Provider}
import net.ceedubs.ficus.Ficus._
import play.api.Mode.Mode
import play.api._
import play.api.i18n.{Lang, MessagesApi}
import play.api.mvc._
import play.modules.reactivemongo.ReactiveMongoComponent
import play.twirl.api.Html
import reactivemongo.api.DB
import repositories._
import services._
import services.iv.{IdentityVerificationService, IvService}
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

  override  def auditConnector: AuditConnector = AuditServiceConnector

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

  override protected def appNameConfiguration: Configuration = Play.current.configuration
}

object ControllerConfiguration extends ControllerConfig {
  lazy val controllerConfigs = Play.current.configuration.underlying.as[Config]("controllers")
}

class GuiceModule(environment: Environment,
                  configuration: Configuration) extends AbstractModule {
  def configure() = {

    bind(classOf[ServicesConfig]).toInstance(new ServicesConfig {
      override protected def mode: Mode = environment.mode

      override protected def runModeConfiguration: Configuration = configuration
    })


    bind(classOf[AuditingService]).toInstance(AuditingService)
    bind(classOf[DB]).toProvider(classOf[MongoDbProvider]).asEagerSingleton()
    bind(classOf[SessionRepo]).annotatedWith(Names.named("propertyLinkingSession")).to(classOf[PropertyLinkingSessionRepository])
    bind(classOf[SessionRepo]).annotatedWith(Names.named("personSession")).to(classOf[PersonalDetailsSessionRepository])
    bind(classOf[WSHttp]).to(classOf[VPLHttp])
    bind(classOf[Details]).to(classOf[VoaDetails])
    bind(classOf[VoaAction]).to(classOf[GgAction])
    bind(classOf[Auth]).to(classOf[VoaAuth])
    bind(classOf[ManageDetails]).to(classOf[ManageVoaDetails])
    bind(classOf[IdentityVerificationService]).to(classOf[IvService])
    bind(classOf[Clock]).toInstance(Clock.systemUTC())
    bind(classOf[uk.gov.hmrc.auth.core.AuthConnector]).to(classOf[AuthConnectorImpl])
    bind(classOf[AuthConnector]).to(classOf[VPLAuthConnector])
    bind(classOf[CircuitBreakerConfig]).toProvider(classOf[CircuitBreakerConfigProvider]).asEagerSingleton()
    bind(classOf[PdfGenerator]).toInstance(new PdfGenerator(environment))
  }

}

class AuthConnectorImpl @Inject()(val http: WSHttp, override val runModeConfiguration: Configuration) extends PlayAuthConnector with ServicesConfig {
  override val serviceUrl: String = baseUrl("auth")

  override protected def mode: Mode = Play.current.mode
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