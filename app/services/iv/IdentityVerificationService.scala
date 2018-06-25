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

package services.iv

import config.{ApplicationConfig, Global}
import connectors.identityVerificationProxy.IdentityVerificationProxyConnector
import connectors.{Addresses, GroupAccounts, IndividualAccounts, VPLAuthConnector}
import javax.inject.{Inject, Named}
import models._
import models.registration._
import models.identityVerificationProxy.Journey
import play.api.i18n.Messages
import play.api.mvc.Results._
import play.api.mvc.{Request, Result}
import repositories.SessionRepo
import services.RegistrationService
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}
import uk.gov.hmrc.auth.core.{Admin, User}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.connectors.domain.ConfidenceLevel

import scala.concurrent.{ExecutionContext, Future}

trait IdentityVerificationService {

  type B

  val proxyConnector: IdentityVerificationProxyConnector
  val config: ApplicationConfig

  def start(userData: IVDetails)(implicit hc: HeaderCarrier, ec: ExecutionContext) =
    proxyConnector
      .start(Journey("voa-property-linking", successUrl, failureUrl, ConfidenceLevel.L200, userData))

  def someCase(obj: B)(implicit request: Request[_], messages: Messages): Result

  def noneCase(implicit request: Request[_], messages: Messages): Result

  def continue[A](journeyId: String)(implicit ctx: A, hc: HeaderCarrier, ec: ExecutionContext): Future[Option[B]]

  protected val successUrl: String

  private val failureUrl = config.baseUrl + controllers.routes.IdentityVerification.fail().url
}

class IvService @Inject()(
                           auth: VPLAuthConnector,
                           registrationService: RegistrationService,
                           @Named("personSession") personalDetailsSessionRepo: SessionRepo,
                           val proxyConnector: IdentityVerificationProxyConnector,
                           implicit val config: ApplicationConfig
                         ) extends IdentityVerificationService {

  type B = RegistrationResult

  protected val successUrl: String = config.baseUrl + controllers.routes.IdentityVerification.success().url

  def someCase(obj: RegistrationResult)(implicit request: Request[_], messages: Messages) = obj match {
    case EnrolmentSuccess(personId) => Redirect(controllers.registration.routes.RegistrationController.success(personId))
    case EnrolmentFailure => InternalServerError(Global.internalServerErrorTemplate)
    case DetailsMissing => InternalServerError(Global.internalServerErrorTemplate)
  }

  def noneCase(implicit request: Request[_], messages: Messages) = InternalServerError(Global.internalServerErrorTemplate)

  def continue[A](journeyId: String)(implicit ctx: A, hc: HeaderCarrier, ec: ExecutionContext): Future[Option[RegistrationResult]] = {
    auth.userDetails(ctx).flatMap(user =>
      (user.userInfo.affinityGroup, user.userInfo.credentialRole) match {
        case (Organisation, User | Admin) =>
          for {
            organisationDetailsOpt <- personalDetailsSessionRepo.get[AdminOrganisationAccountDetails]
            organisationDetails = organisationDetailsOpt.getOrElse(throw new Exception("details not found"))
            registrationResult <- registrationService.create(organisationDetails.toGroupDetails, ctx)(organisationDetails.toIndividualAccountSubmission(journeyId))
          } yield Some(registrationResult)
        case (Individual, _) =>
          for {
            individualDetailsOpt <- personalDetailsSessionRepo.get[IndividualUserAccountDetails]
            individualDetails = individualDetailsOpt.getOrElse(throw new Exception("details not found"))
            registrationResult <- registrationService.create(individualDetails.toGroupDetails, ctx)(individualDetails.toIndividualAccountSubmission(journeyId))
          } yield Some(registrationResult)
      })
  }

}


