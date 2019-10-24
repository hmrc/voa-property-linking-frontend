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

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import play.api._
import repositories._
import services._
import services.iv.{IdentityVerificationService, IvService}
import uk.gov.hmrc.play.bootstrap.config.{RunMode, ServicesConfig}

class GuiceModule(
                   environment: Environment,
                   configuration: Configuration
                 ) extends AbstractModule {

  def configure() = {

    bind(classOf[ServicesConfig]).toInstance(new ServicesConfig(configuration, new RunMode(configuration, environment.mode)))
    bind(classOf[SessionRepo]).annotatedWith(Names.named("propertyLinkingSession")).to(classOf[PropertyLinkingSessionRepository])
    bind(classOf[SessionRepo]).annotatedWith(Names.named("personSession")).to(classOf[PersonalDetailsSessionRepository])
    bind(classOf[SessionRepo]).annotatedWith(Names.named("appointLinkSession")).to(classOf[PropertyLinksSessionRepository])
    bind(classOf[ManageDetails]).to(classOf[ManageVoaDetails])
    bind(classOf[IdentityVerificationService]).to(classOf[IvService])
    bind(classOf[Clock]).toInstance(Clock.systemUTC())
  }

}