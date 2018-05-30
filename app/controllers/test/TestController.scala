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

package controllers.test

import java.time.Instant
import java.util.UUID

import actions.AuthenticatedAction
import connectors._
import connectors.test.{EmacConnector, TestConnector}
import controllers.{Pagination, PaginationSearchSort, PropertyLinkingController}
import javax.inject.Inject
import models._
import models.test.TestUserDetails
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import services.{EnrolmentService, Failure, Success}

import scala.util.Random

class TestController @Inject()(authenticated: AuthenticatedAction,
                               enrolmentService: EnrolmentService,
                               individualAccounts: IndividualAccounts,
                               groups: GroupAccounts,
                               emacConnector: EmacConnector,
                               vPLAuthConnector: VPLAuthConnector,
                               testConnector: TestConnector,
                               reprConnector: PropertyRepresentationConnector
                              )(implicit val messagesApi: MessagesApi) extends PropertyLinkingController {

  def getUserDetails() = authenticated { implicit request =>
    Ok(Json.toJson(if (request.organisationAccount.isAgent) {
      TestUserDetails(
        personId = request.individualAccount.individualId,
        organisationId = request.organisationAccount.id,
        organisationName = request.organisationAccount.companyName,
        governmentGatewayGroupId = request.organisationAccount.groupId,
        governmentGatewayExternalId = request.individualAccount.externalId,
        agentCode = Some(request.organisationAccount.agentCode))
    } else {
      TestUserDetails(personId = request.individualAccount.individualId,
        organisationId = request.organisationAccount.id,
        organisationName = request.organisationAccount.companyName,
        governmentGatewayGroupId = request.organisationAccount.groupId,
        governmentGatewayExternalId = request.individualAccount.externalId,
        agentCode = None)
    }))
  }

  def deRegister() = authenticated { implicit request =>
    val orgId = request.individualAccount.organisationId
    testConnector.deRegister(orgId).map(res => Ok(s"Successfully de-registered organisation with ID: $orgId")).recover {
      case e => Ok(s"Failed to de-register organisation with ID: $orgId with error: ${e.getMessage}")
    }
  }

  def deEnrol() = authenticated { implicit request =>
    enrolmentService
      .deEnrolUser(request.individualAccount.individualId)
      .map {
        case Success => Ok("Successful")
        case Failure => Ok("Failure")
      }
  }

  def delete = authenticated { implicit request =>
    val externalId = UUID.randomUUID().toString
    for {
      user <- vPLAuthConnector.getUserDetails
      _ <- emacConnector.removeEnrolment(request.individualAccount.individualId, user.userInfo.gatewayId, user.userInfo.groupIdentifier)
      _ <- individualAccounts.update(request.individualAccount.copy(externalId = externalId))
      _ <- groups.update(request.organisationAccount.id, create(request.organisationAccount, externalId))
    } yield Ok("Successful")
  }

  def revokeAgentAppointments(agentOrgId: String) = authenticated { implicit request =>
    val agentAuthResult = reprConnector.forAgentSearchAndSort(agentOrgId.toLong, PaginationSearchSort(pageNumber = 1, pageSize = 100))
    agentAuthResult.map(representation => representation.authorisations.map(
      authorisation => reprConnector.revoke(authorisation.authorisedPartyId)
    )).map(_ =>
      Ok("Agent appointments revoked"))
  }

  def declinePendingAgentAppointments(agentOrgId: String, agentPersonId: String) = authenticated { implicit request =>
    val pendingAgentAppointments = reprConnector.forAgent(RepresentationPending, agentOrgId.toLong, Pagination(pageNumber = 1, pageSize = 100))
    pendingAgentAppointments.map(appointments =>
      appointments.propertyRepresentations.map(appointment =>
        reprConnector.response(RepresentationResponse(appointment.submissionId, agentPersonId.toLong, RepresentationResponseDeclined)))).map(_ =>
      Ok("Pending agent appointments declined"))
  }

  private def create(group: GroupAccount, externalId: String): UpdatedOrganisationAccount =
    UpdatedOrganisationAccount(
      Random.nextString(40),
      group.addressId,
      group.isAgent,
      group.companyName,
      group.email,
      group.phone,
      Instant.now(),
      externalId
    )
}
