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

package services

import config.ApplicationConfig
import connectors.{Addresses, GroupAccounts, IndividualAccounts, VPLAuthConnector}
import javax.inject.Inject

import models.registration._
import models.{DetailedIndividualAccount, GroupAccount, IndividualAccountSubmission}
import play.api.Logger
import services.email.EmailService
import uk.gov.hmrc.auth.core.{AffinityGroup, Assistant}
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}

import scala.concurrent.{ExecutionContext, Future}


class RegistrationService @Inject()(groupAccounts: GroupAccounts,
                                    individualAccounts: IndividualAccounts,
                                    enrolmentService: EnrolmentService,
                                    auth: VPLAuthConnector,
                                    addresses: Addresses,
                                    emailService: EmailService,
                                    config: ApplicationConfig
                                   ) {

  private val logger: Logger = Logger(this.getClass)

  def create[A](
                 groupDetails: GroupAccountDetails,
                 ctx: A,
                 affinityGroupOpt: AffinityGroup
               )
               (individual: UserDetails => Long => Option[Long] => IndividualAccountSubmission)
               (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RegistrationResult] = {
    for {
      user          <- auth.userDetails(ctx)
      groupId       <- auth.getGroupId(ctx)
      id            <- addresses.registerAddress(groupDetails)
      _             <- register(groupId, acc => individualAccounts.create(individual(user)(id)(Some(acc.id))), groupAccounts.create(groupId, id, groupDetails, individual(user)(id)(None)))
      personId      <- individualAccounts.withExternalId(user.externalId)
      groupAccount  <- groupAccounts.withGroupId(groupId)
      res           <- enrol(personId, id, groupAccount, affinityGroupOpt)(user)
    } yield res
  }

  private def register(
                        groupId: String,
                        groupExists: GroupAccount => Future[Int],
                        noGroup: => Future[Long])
                      (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Long] = {
    groupAccounts.withGroupId(groupId).flatMap(groupExists(_).map(_.toLong)).recoverWith {
      case _: NotFoundException => noGroup
      case e                    => throw e
    }
  }

  private def enrol(
                     detailIndiv: DetailedIndividualAccount,
                     addressId: Long,
                     groupAccount: GroupAccount,
                     affinityGroupOpt: AffinityGroup)
                   (userDetails: UserDetails)
                   (implicit hc: HeaderCarrier, ex: ExecutionContext): Future[RegistrationResult] =
    if (config.stubEnrolment) {
      success(userDetails, detailIndiv, groupAccount, affinityGroupOpt)
    } else {
      userDetails.userInfo.credentialRole match {
        case Assistant  => success(userDetails, detailIndiv, groupAccount, affinityGroupOpt)
        case _          =>
          enrolmentService.enrol(detailIndiv.individualId, addressId).flatMap {
            case Success => success(userDetails, detailIndiv, groupAccount, affinityGroupOpt)
            case Failure =>
              logger.warn("Failed to enrol new VOA user")
              success(userDetails, detailIndiv, groupAccount, affinityGroupOpt)
          }
      }
    }

  private def success(
                       userDetails: UserDetails,
                       detailedIndividualAccount: DetailedIndividualAccount,
                       groupAccount: GroupAccount,
                       affinityGroupOpt: AffinityGroup)
                     (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RegistrationResult] = {
    logger.info(s"New ${userDetails.userInfo.affinityGroup} ${userDetails.userInfo.credentialRole} successfully registered for VOA")
    emailService
      .sendNewRegistrationSuccess(userDetails.userInfo.email, detailedIndividualAccount, groupAccount, affinityGroupOpt)
      .map(_ => RegistrationSuccess(detailedIndividualAccount.individualId))
  }

}

