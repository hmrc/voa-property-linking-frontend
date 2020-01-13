/*
 * Copyright 2020 HM Revenue & Customs
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
import com.google.inject.name.Names.named
import com.typesafe.config.ConfigException
import play.api._
import repositories._
import services._
import services.iv.{IdentityVerificationService, IvService}
import uk.gov.hmrc.play.bootstrap.config.{RunMode, ServicesConfig}

import scala.util.Try

class GuiceModule(
                   environment: Environment,
                   configuration: Configuration
                 ) extends AbstractModule {

  def configure() = {

    bindBoolean("feature.externalValuation", "detailed-valuation.external")
    bindBoolean("feature.assessmentSkip", "detailed-valuation.skip")
    bindBoolean("feature.newSummaryValuationRoute", "summary-valuation.newRoute")

    bind(classOf[ServicesConfig]).toInstance(new ServicesConfig(configuration, new RunMode(configuration, environment.mode)))
    bind(classOf[SessionRepo]).annotatedWith(Names.named("propertyLinkingSession")).to(classOf[PropertyLinkingSessionRepository])
    bind(classOf[SessionRepo]).annotatedWith(Names.named("personSession")).to(classOf[PersonalDetailsSessionRepository])
    bind(classOf[SessionRepo]).annotatedWith(Names.named("appointLinkSession")).to(classOf[PropertyLinksSessionRepository])
    bind(classOf[ManageDetails]).to(classOf[ManageVoaDetails])
    bind(classOf[IdentityVerificationService]).to(classOf[IvService])
    bind(classOf[Clock]).toInstance(Clock.systemUTC())
  }

  protected def bindBoolean(path: String, name: String = ""): Unit =
    bindConstant()
      .annotatedWith(named(resolveAnnotationName(path, name)))
      .to(Try(configuration.get[String](path).toBoolean).toOption.getOrElse(configException(path))) //We need to parse as string, due to the process of adding in from app-config-<env> it is seen as a string

  private def resolveAnnotationName(path: String, name: String): String = name match {
    case "" => path
    case _  => name
  }

  private def configException(path: String) = throw new ConfigException.Missing(path)
}