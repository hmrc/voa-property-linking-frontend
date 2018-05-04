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

package services

import javax.inject.Inject

import config.ApplicationConfig
import connectors.{Addresses, GroupAccounts, IndividualAccounts, VPLAuthConnector}
import controllers.GroupAccountDetails
import models.enrolment._
import models.{DetailedIndividualAccount, GroupAccount, IVDetails, IndividualAccountSubmission}
import play.api.mvc.{AnyContent, Request}
import services.email.EmailService
import services.iv.IdentityVerificationService
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.auth.core
import uk.gov.hmrc.auth.core.{Admin, Assistant, User}

import scala.concurrent.{ExecutionContext, Future}


class RegistrationService @Inject()(groupAccounts: GroupAccounts,
                                    individualAccounts: IndividualAccounts,
                                    enrolmentService: EnrolmentService,
                                    auth: VPLAuthConnector,
                                    addresses: Addresses,
                                    emailService: EmailService,
                                    identityVerificationService: IdentityVerificationService,
                                    config: ApplicationConfig
                                   ) {

  def create[A](
                 groupDetails: GroupAccountDetails,
                 iVDetails: IVDetails,
                 ctx: A
               )
               (individual: UserDetails => Long => Option[Long] => IndividualAccountSubmission)
               (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RegistrationResult] = {
    for {
      user      <- auth.userDetails(ctx)
      groupId   <- auth.getGroupId(ctx)
      id        <- addresses.registerAddress(groupDetails)
      _         <- voaRegister(groupId, acc => individualAccounts.create(individual(user)(id)(Some(acc.id))), groupAccounts.create(groupId, id, groupDetails, individual(user)(id)(None)))
      personId  <- individualAccounts.withExternalId(user.externalId) //This is used to get the personId back for the group accounts create.
      res       <- enrol(personId, id, iVDetails)(user)
    } yield res
  }

  private def voaRegister(
                           groupId: String,
                           groupExists: GroupAccount => Future[Int],
                           noGroup: => Future[Long])
                         (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Long] = {
    groupAccounts.withGroupId(groupId).flatMap {
      case Some(acc) => groupExists(acc).map(_.toLong)
      case _         => noGroup
    }
  }

  private def enrol(
                     option: Option[DetailedIndividualAccount],
                     addressId: Long,
                     iVDetails: IVDetails)
                   (userDetails: UserDetails)
                   (implicit hc: HeaderCarrier, ex: ExecutionContext): Future[RegistrationResult] = (option, userDetails.userInfo.credentialRole) match {
    case (Some(detailIndiv), Assistant)  => success(userDetails, detailIndiv, iVDetails)
    case (Some(detailIndiv), _)  => enrolmentService.enrol(detailIndiv.individualId, addressId).flatMap {
      case Success => success(userDetails, detailIndiv, iVDetails)
      case Failure => Future.successful(EnrolmentFailure)
    }
    case (None, _)              => Future.successful(DetailsMissing)
  }

  private def success(
                       userDetails: UserDetails,
                       detailedIndividualAccount: DetailedIndividualAccount,
                       iVDetails: IVDetails)
                     (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RegistrationResult] = {

    (userDetails.userInfo.affinityGroup, userDetails.userInfo.credentialRole) match {
      case (Organisation, Assistant)       =>
        Future
          .successful(IVNotRequired(detailedIndividualAccount.individualId))
      case _  if config.ivEnrolmentEnabled =>
        emailService
          .sendNewEnrolmentSuccess(userDetails.userInfo.email, detailedIndividualAccount)

        identityVerificationService
          .start(iVDetails)
          .map(EnrolmentSuccess(_, detailedIndividualAccount.individualId))
      case _                                =>
          Future
            .successful(IVNotRequired(detailedIndividualAccount.individualId))
    }
  }
}

