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

import java.time.Clock

import auth.{GgAction, VoaAction}
import com.builtamont.play.pdf.PdfGenerator
import com.google.inject.AbstractModule
import com.google.inject.name.Names
import connectors.VPLAuthConnector
import play.api.Mode.Mode
import play.api._
import repositories._
import services._
import services.iv.{IdentityVerificationService, IvService}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

class GuiceModule(
                   environment: Environment,
                   configuration: Configuration
                 ) extends AbstractModule {

  def configure() = {

    bind(classOf[ServicesConfig]).toInstance(new ServicesConfig {
      override protected def mode: Mode = environment.mode

      override protected def runModeConfiguration: Configuration = configuration
    })

    bind(classOf[SessionRepo]).annotatedWith(Names.named("propertyLinkingSession")).to(classOf[PropertyLinkingSessionRepository])
    bind(classOf[SessionRepo]).annotatedWith(Names.named("personSession")).to(classOf[PersonalDetailsSessionRepository])
    bind(classOf[SessionRepo]).annotatedWith(Names.named("appointLinkSession")).to(classOf[PropertyLinksSessionRepository])
    bind(classOf[VoaAction]).to(classOf[GgAction])
    bind(classOf[ManageDetails]).to(classOf[ManageVoaDetails])
    bind(classOf[IdentityVerificationService]).to(classOf[IvService])
    bind(classOf[Clock]).toInstance(Clock.systemUTC())
    bind(classOf[AuthConnector]).to(classOf[VPLAuthConnector])
    bind(classOf[PdfGenerator]).toInstance(new PdfGenerator(environment))
  }

}