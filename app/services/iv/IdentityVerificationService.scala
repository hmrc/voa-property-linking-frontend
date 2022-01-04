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

package services.iv

import config.ApplicationConfig
import connectors.identityVerificationProxy.IdentityVerificationProxyConnector
import models._
import models.identityVerificationProxy.{Journey, Link}
import models.registration._
import services.RegistrationService
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IdentityVerificationService @Inject()(
      val errorHandler: CustomErrorHandler,
      registrationService: RegistrationService,
      val proxyConnector: IdentityVerificationProxyConnector,
      implicit val config: ApplicationConfig) {

  // lazy is required here to ensure that the reverse route lookup
  // includes the context (/business-rates-property-linking) in the URL
  lazy val successUrl: String = controllers.routes.IdentityVerification.success(None).url
  lazy val failureUrl: String = controllers.routes.IdentityVerification.fail(None).url

  // TODO switch to IV Frontend "uplift" endpoint
  def start(userData: IVDetails)(implicit hc: HeaderCarrier): Future[Link] =
    proxyConnector
      .start(Journey("voa-property-linking", successUrl, failureUrl, ConfidenceLevel.L200, userData))

  def continue(journeyId: Option[String], userDetails: UserDetails)(
        implicit hc: HeaderCarrier,
        ec: ExecutionContext): Future[Option[RegistrationResult]] =
    registrationService.continue(journeyId, userDetails)

}
