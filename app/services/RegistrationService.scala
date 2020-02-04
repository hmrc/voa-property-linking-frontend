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

package services

import config.ApplicationConfig
import connectors.{Addresses, GroupAccounts, IndividualAccounts}
import javax.inject.Inject
import models.registration._
import models.{DetailedIndividualAccount, GroupAccount, IndividualAccountSubmission}
import play.api.Logger
import services.email.EmailService
import uk.gov.hmrc.auth.core.{AffinityGroup, Assistant}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class RegistrationService @Inject()(
      groupAccounts: GroupAccounts,
      individualAccounts: IndividualAccounts,
      enrolmentService: EnrolmentService,
      addresses: Addresses,
      emailService: EmailService,
      config: ApplicationConfig) {

  def create(
        groupDetails: GroupAccountDetails,
        userDetails: UserDetails,
        affinityGroupOpt: Option[AffinityGroup] = None
  )(individual: UserDetails => Long => Option[Long] => IndividualAccountSubmission)(
        implicit hc: HeaderCarrier,
        ec: ExecutionContext): Future[RegistrationResult] =
    for {
      id <- addresses.registerAddress(groupDetails)
      _ <- register(
            groupId = userDetails.groupIdentifier,
            groupExists = acc => individualAccounts.create(individual(userDetails)(id)(Some(acc.id))),
            noGroup = groupAccounts.create(
              groupId = userDetails.groupIdentifier,
              addressId = id,
              details = groupDetails,
              individualAccountSubmission = individual(userDetails)(id)(None))
          )
      personId     <- individualAccounts.withExternalId(userDetails.externalId)
      groupAccount <- groupAccounts.withGroupId(userDetails.groupIdentifier)
      res          <- enrol(personId, id, groupAccount, affinityGroupOpt)(userDetails)
    } yield res

  private def register(groupId: String, groupExists: GroupAccount => Future[Int], noGroup: => Future[Long])(
        implicit hc: HeaderCarrier,
        ec: ExecutionContext): Future[Long] =
    groupAccounts.withGroupId(groupId).flatMap {
      case Some(acc) => groupExists(acc).map(_.toLong)
      case _         => noGroup
    }

  private def enrol(
        option: Option[DetailedIndividualAccount],
        addressId: Long,
        groupAccount: Option[GroupAccount],
        affinityGroupOpt: Option[AffinityGroup] = None)(
        userDetails: UserDetails)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[RegistrationResult] =
    if (config.stubEnrolment) {
      option match {
        case Some(detailIndiv) => success(userDetails, detailIndiv, groupAccount, affinityGroupOpt)
        case _                 => Future.successful(DetailsMissing)
      }
    } else {
      (option, userDetails.credentialRole) match {
        case (Some(detailIndiv), Assistant) => success(userDetails, detailIndiv, groupAccount, affinityGroupOpt)
        case (Some(detailIndiv), _) =>
          enrolmentService.enrol(detailIndiv.individualId, addressId).flatMap {
            case Success => success(userDetails, detailIndiv, groupAccount, affinityGroupOpt)
            case Failure =>
              Logger.warn("Failed to enrol new VOA user")
              success(userDetails, detailIndiv, groupAccount, affinityGroupOpt)
          }
        case (None, _) => Future.successful(DetailsMissing)
      }
    }

  private def success(
        userDetails: UserDetails,
        detailedIndividualAccount: DetailedIndividualAccount,
        groupAccount: Option[GroupAccount],
        affinityGroupOpt: Option[AffinityGroup] = None)(
        implicit hc: HeaderCarrier,
        ec: ExecutionContext): Future[RegistrationResult] = {
    Logger.info(s"New ${userDetails.affinityGroup} ${userDetails.credentialRole} successfully registered for VOA")
    emailService
      .sendNewRegistrationSuccess(userDetails.email, detailedIndividualAccount, groupAccount, affinityGroupOpt)
      .map(_ => RegistrationSuccess(detailedIndividualAccount.individualId))
  }

}
