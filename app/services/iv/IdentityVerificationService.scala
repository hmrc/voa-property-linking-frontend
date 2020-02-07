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

package services.iv

import config.ApplicationConfig
import connectors.identityVerificationProxy.IdentityVerificationProxyConnector
import javax.inject.{Inject, Named}
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

trait IdentityVerificationService {

  type B

  val proxyConnector: IdentityVerificationProxyConnector
  val config: ApplicationConfig

  def start(userData: IVDetails)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Link] =
    proxyConnector
      .start(Journey("voa-property-linking", successUrl, failureUrl, ConfidenceLevel.L200, userData))

  def someCase(obj: B)(implicit request: Request[_], messages: Messages): Result

  def noneCase(implicit request: Request[_], messages: Messages): Result

  def continue(journeyId: String, userDetails: UserDetails)(
        implicit hc: HeaderCarrier,
        ec: ExecutionContext): Future[Option[B]]

  protected val successUrl: String

  private val failureUrl = config.baseUrl + controllers.routes.IdentityVerification.fail(None).url
}

class IvService @Inject()(
      val errorHandler: CustomErrorHandler,
      registrationService: RegistrationService,
      @Named("personSession") personalDetailsSessionRepo: SessionRepo,
      override val proxyConnector: IdentityVerificationProxyConnector,
      implicit val config: ApplicationConfig)
    extends IdentityVerificationService {

  type B = RegistrationResult

  protected val successUrl: String = config.baseUrl + controllers.routes.IdentityVerification.success(None).url

  def someCase(obj: RegistrationResult)(implicit request: Request[_], messages: Messages): Result = obj match {
    case RegistrationSuccess(personId) =>
      Redirect(controllers.registration.routes.RegistrationController.success(personId))
    case EnrolmentFailure => InternalServerError(errorHandler.internalServerErrorTemplate)
    case DetailsMissing   => InternalServerError(errorHandler.internalServerErrorTemplate)
  }

  def noneCase(implicit request: Request[_], messages: Messages): Result =
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
