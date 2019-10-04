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

import auditing.AuditingService
import auth.{GgAction, VoaAction}
import com.builtamont.play.pdf.PdfGenerator
import com.google.inject.AbstractModule
import com.google.inject.name.Names
import com.typesafe.config.Config
import connectors.VPLAuthConnector
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


    bind(classOf[DB]).toProvider(classOf[MongoDbProvider]).asEagerSingleton()
    bind(classOf[SessionRepo]).annotatedWith(Names.named("propertyLinkingSession")).to(classOf[PropertyLinkingSessionRepository])
    bind(classOf[SessionRepo]).annotatedWith(Names.named("personSession")).to(classOf[PersonalDetailsSessionRepository])
    bind(classOf[SessionRepo]).annotatedWith(Names.named("appointLinkSession")).to(classOf[PropertyLinksSessionRepository])
    bind(classOf[VoaAction]).to(classOf[GgAction])
    bind(classOf[ManageDetails]).to(classOf[ManageVoaDetails])
    bind(classOf[IdentityVerificationService]).to(classOf[IvService])
    bind(classOf[Clock]).toInstance(Clock.systemUTC())
    bind(classOf[AuthConnector]).to(classOf[VPLAuthConnector])
    bind(classOf[CircuitBreakerConfig]).toProvider(classOf[CircuitBreakerConfigProvider]).asEagerSingleton()
    bind(classOf[PdfGenerator]).toInstance(new PdfGenerator(environment))
  }

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