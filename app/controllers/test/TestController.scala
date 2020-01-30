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

package controllers.test

import actions.AuthenticatedAction
import actions.propertylinking.WithLinkingSession
import connectors._
import connectors.attachments.BusinessRatesAttachmentsConnector
import connectors.test.{TestCheckConnector, TestPropertyLinkingConnector}
import controllers.{Pagination, PropertyLinkingController}
import javax.inject.Inject
import models._
import models.test.TestUserDetails
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.test.TestService
import services.{Failure, Success}
import uk.gov.voa.propertylinking.errorhandler.CustomErrorHandler
import utils.Cats

import scala.concurrent.{ExecutionContext, Future}

class TestController @Inject()(
      override val errorHandler: CustomErrorHandler,
      authenticated: AuthenticatedAction,
      withLinkingSession: WithLinkingSession,
      testService: TestService,
      individualAccounts: IndividualAccounts,
      testPropertyLinkingConnector: TestPropertyLinkingConnector,
      businessRatesAttachmentsConnector: BusinessRatesAttachmentsConnector,
      testCheckConnector: TestCheckConnector,
      reprConnector: PropertyRepresentationConnector
)(implicit executionContext: ExecutionContext, override val controllerComponents: MessagesControllerComponents)
    extends PropertyLinkingController with Cats {

  val getUserDetails: Action[AnyContent] = authenticated { implicit request =>
    Ok(Json.toJson(if (request.organisationAccount.isAgent) {
      TestUserDetails(
        personId = request.individualAccount.individualId,
        organisationId = request.organisationAccount.id,
        organisationName = request.organisationAccount.companyName,
        governmentGatewayGroupId = request.organisationAccount.groupId,
        governmentGatewayExternalId = request.individualAccount.externalId,
        agentCode = request.organisationAccount.agentCode
      )
    } else {
      TestUserDetails(
        personId = request.individualAccount.individualId,
        organisationId = request.organisationAccount.id,
        organisationName = request.organisationAccount.companyName,
        governmentGatewayGroupId = request.organisationAccount.groupId,
        governmentGatewayExternalId = request.individualAccount.externalId,
        agentCode = None
      )
    }))
  }

  def deRegister(): Action[AnyContent] = authenticated.async { implicit request =>
    val orgId = request.individualAccount.organisationId
    testPropertyLinkingConnector
      .deRegister(orgId)
      .map(_ => Ok(s"Successfully de-registered organisation with ID: $orgId"))
      .recover {
        case e => Ok(s"Failed to de-register organisation with ID: $orgId with error: ${e.getMessage}")
      }
  }

  def deEnrol() = authenticated.async { implicit request =>
    testService
      .deEnrolUser(request.individualAccount.individualId)
      .map {
        case Success => Ok("Successful")
        case Failure => Ok("Failure")
      }
  }

  def revokeAgentAppointments(agentOrgId: String) = authenticated.async { implicit request =>
    //TODO need more context around what this is used for.
    Future.successful(Ok("Agent appointments revoked"))
  }

  def declinePendingAgentAppointments(agentOrgId: String, agentPersonId: String) = authenticated.async {
    implicit request =>
      val pendingAgentAppointments =
        reprConnector.forAgent(RepresentationPending, agentOrgId.toLong, Pagination(pageNumber = 1, pageSize = 100))
      pendingAgentAppointments
        .map(appointments =>
          appointments.propertyRepresentations.map(appointment =>
            reprConnector.response(
              RepresentationResponse(appointment.submissionId, agentPersonId.toLong, RepresentationResponseDeclined))))
        .map(_ => Ok("Pending agent appointments declined"))
  }

  def clearDvrRecords = authenticated.async { implicit request =>
    testPropertyLinkingConnector
      .clearDvrRecords(request.organisationAccount.id)
      .map(res => Ok(s"Successfully cleared DVR records for organisation with ID: ${request.organisationAccount.id}"))
      .recover {
        case e =>
          Ok(
            s"Failed to clear DVR records for organisation with ID: ${request.organisationAccount.id} with error: ${e.getMessage}")
      }
  }

  def clearDraftCases = authenticated.async { implicit request =>
    testCheckConnector
      .clearDraftCases(request.organisationAccount.id)
      .map(res =>
        Ok(s"Successfully cleared draft check cases for organisation with ID: ${request.organisationAccount.id}"))
      .recover {
        case e =>
          Ok(
            s"Failed to clear draft check cases for organisation with ID: ${request.organisationAccount.id} with error: ${e.getMessage}")
      }
  }

  def clearCheckCases(propertyLinksSubmissionId: String) = authenticated.async { implicit request =>
    testPropertyLinkingConnector
      .deleteCheckCases(propertyLinksSubmissionId)
      .map(res =>
        Ok(s"Successfully cleared the check cases for propertyLinksSubmissionId: $propertyLinksSubmissionId"))
      .recover {
        case e =>
          Ok(
            s"Failed to delete the check cases for propertyLinksSubmissionId: $propertyLinksSubmissionId with error: ${e.getMessage}")
      }
  }

  def getSubmittedCheck(submissionId: String) = authenticated.async { implicit request =>
    testCheckConnector.getSubmittedCheck(submissionId).map(response => Ok(response.body))
  }

  val getAttachments = authenticated.andThen(withLinkingSession).async { implicit request =>
    request.ses.uploadEvidenceData.attachments
      .fold(Set.empty[String])(_.keySet)
      .toList
      .traverse(businessRatesAttachmentsConnector.getAttachment)
      .map(attachments => Ok(Json.toJson(AttachmentsInLinkingSession(attachments))))
  }

}
