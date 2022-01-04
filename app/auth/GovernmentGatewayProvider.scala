/*
 * Copyright 2022 HM Revenue & Customs
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

package auth

import config.ApplicationConfig
import javax.inject.Inject
import play.api.mvc._
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.Future

class GovernmentGatewayProvider @Inject()(
      override val env: Environment,
      override val config: Configuration
)(applicationConfig: ApplicationConfig)
    extends AuthRedirects { this: ServicesConfig =>
  def additionalLoginParameters: Map[String, Seq[String]] = Map("accountType" -> Seq("organisation"))

  def redirectToLogin(implicit request: Request[_]): Future[Result] =
    Future.successful(toGGLogin(request.uri))
}
