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

package controllers.test

import actions.AuthenticatedAction
import actions.propertylinking.WithLinkingSession
import connectors.attachments.BusinessRatesAttachmentsConnector
import connectors.test.TestCheckConnector
import controllers.PropertyLinkingController
import models.test.TestUserDetails
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.test.TestService
import services.{Failure, Success}
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler
import utils.Cats

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class TestController @Inject()(
      override val errorHandler: CustomErrorHandler,
      authenticated: AuthenticatedAction,
      withLinkingSession: WithLinkingSession,
      testService: TestService,
      businessRatesAttachmentsConnector: BusinessRatesAttachmentsConnector,
      testCheckConnector: TestCheckConnector
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

  def deEnrol() = authenticated.async { implicit request =>
    testService
      .deEnrolUser(request.individualAccount.individualId)
      .map {
        case Success => Ok("Successful")
        case Failure => Ok("Failure")
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
