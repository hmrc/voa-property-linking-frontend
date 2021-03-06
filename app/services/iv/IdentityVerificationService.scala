/*
 * Copyright 2021 HM Revenue & Customs
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
import javax.inject.{Inject, Named, Singleton}
import models._
import models.identityVerificationProxy.{Journey, Link}
import models.registration._
import play.api.i18n.Messages
import play.api.mvc.Results._
import play.api.mvc.{Request, Result}
import repositories.SessionRepo
import services.RegistrationService
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}
import uk.gov.hmrc.auth.core.{Assistant, ConfidenceLevel}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IdentityVerificationService @Inject()(
      val errorHandler: CustomErrorHandler,
      registrationService: RegistrationService,
      @Named("personSession") personalDetailsSessionRepo: SessionRepo,
      val proxyConnector: IdentityVerificationProxyConnector,
      implicit val config: ApplicationConfig) {

  // lazy is required here to ensure that the reverse route lookup
  // includes the context (/business-rates-property-linking) in the URL
  lazy val successUrl: String = controllers.routes.IdentityVerification.success(None).url
  lazy val failureUrl: String = controllers.routes.IdentityVerification.fail(None).url

  def start(userData: IVDetails)(implicit hc: HeaderCarrier): Future[Link] =
    proxyConnector
      .start(Journey("voa-property-linking", successUrl, failureUrl, ConfidenceLevel.L200, userData))

  def someCase(obj: RegistrationResult)(implicit request: Request[_]): Result = obj match {
    case RegistrationSuccess(personId) =>
      Redirect(controllers.registration.routes.RegistrationController.success(personId))
    case EnrolmentFailure => InternalServerError(errorHandler.internalServerErrorTemplate)
    case DetailsMissing   => InternalServerError(errorHandler.internalServerErrorTemplate)
  }

  def noneCase(implicit request: Request[_]): Result =
    InternalServerError(errorHandler.internalServerErrorTemplate)

  def continue(journeyId: String, userDetails: UserDetails)(
        implicit hc: HeaderCarrier,
        ec: ExecutionContext): Future[Option[RegistrationResult]] =
    (userDetails.affinityGroup, userDetails.credentialRole) match {
      case (Organisation, role) if role != Assistant =>
        for {
          organisationDetailsOpt <- personalDetailsSessionRepo.get[AdminOrganisationAccountDetails]
          organisationDetails = organisationDetailsOpt.getOrElse(throw new Exception("details not found"))
          registrationResult <- registrationService
                                 .create(organisationDetails.toGroupDetails, userDetails, Some(Organisation))(
                                   organisationDetails.toIndividualAccountSubmission(journeyId))
        } yield Some(registrationResult)
      case (Individual, _) =>
        for {
          individualDetailsOpt <- personalDetailsSessionRepo.get[IndividualUserAccountDetails]
          individualDetails = individualDetailsOpt.getOrElse(throw new Exception("details not found"))
          registrationResult <- registrationService
                                 .create(individualDetails.toGroupDetails, userDetails, Some(Individual))(
                                   individualDetails.toIndividualAccountSubmission(journeyId))
        } yield Some(registrationResult)
    }

}
