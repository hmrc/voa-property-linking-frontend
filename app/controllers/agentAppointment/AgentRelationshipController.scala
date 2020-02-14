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

package controllers.agentAppointment

import javax.inject.{Inject, Named}
import actions.AuthenticatedAction
import actions.agentrelationship.WithAppointAgentSession
import actions.agentrelationship.request.AppointAgentSessionRequest
import actions.requests.BasicAuthenticatedRequest
import binders.pagination.PaginationParameters
import binders.propertylinks.{ExternalPropertyLinkManagementSortField, ExternalPropertyLinkManagementSortOrder, GetPropertyLinksParameters}
import config.ApplicationConfig
import connectors.AgentsConnector
import controllers.agent.routes
import controllers.{PaginationParams, PropertyLinkingController}
import form.{EnumMapping, Mappings}
import models.{RepresentationApproved, RepresentationPending, StartAndContinue, propertyrepresentation}
import models.propertyrepresentation.{AppointNewAgentSession, ChooseFromList, ManageMultiplePropertiesOptions, ManageOnePropertyOptions, Yes}
import models.searchApi.AgentPropertiesFilter.{Both, No}
import models.searchApi.AgentPropertiesSortField.Address
import play.api.data.{Form, FormError, Forms, Mapping}
import play.api.data.Forms.{boolean, longNumber, mapping, nonEmptyText, optional, single, text}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepo
import services.AgentRelationshipService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.voa.propertylinking.errorhandler.CustomErrorHandler
import controllers.agent.routes
import models.searchApi.AgentPropertiesParameters
import views.html.propertyrepresentation.appoint._
import models.propertyrepresentation.OnePropertyOptions
import controllers.agentAppointment.AgentRelationshipForms._

import scala.concurrent.{ExecutionContext, Future}

class AgentRelationshipController @Inject()(val errorHandler: CustomErrorHandler,
                                            authenticated: AuthenticatedAction,
                                            withAppointAgentSession: WithAppointAgentSession,
                                            agentRelationshipService: AgentRelationshipService,
                                            agentsConnector: AgentsConnector,
                                            @Named("appointNewAgentSession") val sessionRepo: SessionRepo,
                                            startPage: views.html.propertyrepresentation.appoint.start,
                                            isTheCorrectAgent: views.html.propertyrepresentation.appoint.isThisYourAgent,
                                            agentToManageOnePropertyNoExistingAgent: views.html.propertyrepresentation.appoint.agentToManageOnePropertyNoExistingAgent,
                                            agentToManageOneProperty: views.html.propertyrepresentation.appoint.agentToManageOneProperty,
                                            agentToManageMultipleProperties: views.html.propertyrepresentation.appoint.agentToManageMultipleProperties,
                                            checkYourAnswers: views.html.propertyrepresentation.appoint.checkYourAnswers,
                                            confirmation: views.html.propertyrepresentation.appoint.confirmation
                                           )(
                                             implicit override val messagesApi: MessagesApi,
                                             override val controllerComponents: MessagesControllerComponents,
                                             executionContext: ExecutionContext,
                                             val config: ApplicationConfig
                                           ) extends PropertyLinkingController {

  def startAppointJourney(): Action[AnyContent] = authenticated.async { implicit request =>
    Future.successful(Ok(startPage(agentCode)))
  }

  def getAgentDetails(): Action[AnyContent] = authenticated.async { implicit request =>
    agentCode.bindFromRequest.fold(
      errors => {
        Future.successful(
          BadRequest(
            startPage(
              swapErrorKeyIfRequired[Long](errors, "agentCode", "error.number", "error.agentCode.required"))))
      },
      success => {
        agentRelationshipService.getAgent(success) map {
          case None => {
            val formWithErrors = agentCode.copy(data = Map("agentCode" -> success.toString),errors = Seq[FormError](FormError(key = "agentCode", message = "error.propertyRepresentation.unknownAgent")))
            BadRequest(startPage(formWithErrors))
          }
          case Some(a) => {
            sessionRepo.saveOrUpdate(AppointNewAgentSession(agentCode = success, agentOrganisationName = Some(a.organisationLatestDetail.organisationName), agentOrganisationId = a.id))
            Redirect(controllers.agentAppointment.routes.AgentRelationshipController.isCorrectAgent())
          }
        }
      }
    )
  }

  def isCorrectAgent(): Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async { implicit request =>
    Future.successful(
      Ok(
        isTheCorrectAgent(
          isThisYourAgent,
          getAgentName(request))))
  }

  def agentSelected(): Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async { implicit request =>
    isThisYourAgent.bindFromRequest.fold(
      errors => {
        Future.successful(
          BadRequest(isTheCorrectAgent(
            errors,
            getAgentName(request))))
      },
      success => {
        if(success){
          sessionRepo.saveOrUpdate(request.sessionData.copy(isCorrectAgent = Some(success)))
          for{
            propertyLinks <- agentRelationshipService.getMyOrganisationPropertyLinksWithAgentFiltering(
              params = GetPropertyLinksParameters(),
              pagination = AgentPropertiesParameters(agentCode = request.sessionData.agentCode),
              agentOrganisationId = request.sessionData.agentOrganisationId,
              organisationId = request.organisationId
            )
            ownerAgents <- agentsConnector.ownerAgents(request.organisationId)
            agentName = getAgentName(request)
          } yield {
            propertyLinks.authorisations.size match {
              case 0 => {
                //fixme send request to modernised once we have an endpoint
                Ok(confirmation(agentName))
              }
              case 1 => {
                ownerAgents.agents.size match {
                  case 0 => Redirect(controllers.agentAppointment.routes.AgentRelationshipController.onePropertyNoExistingAgent())
                  case _ => Redirect(controllers.agentAppointment.routes.AgentRelationshipController.onePropertyWithExistingAgent())
                }
              }
              case _ => Redirect(controllers.agentAppointment.routes.AgentRelationshipController.multipleProperties())
            }
          }
        }
        else{
          Future.successful(Redirect(controllers.agentAppointment.routes.AgentRelationshipController.startAppointJourney()))
        }
      }
    )
  }

  def onePropertyNoExistingAgent(): Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async { implicit request =>
    Future.successful(
      Ok(
        agentToManageOnePropertyNoExistingAgent(
          manageOnePropertyNoAgent,
          getAgentName(request))))
  }

  def submitOnePropertyNoExistingAgent(): Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async { implicit request =>
    manageOnePropertyNoAgent.bindFromRequest.fold(
      errors => {
        Future.successful(
          BadRequest(
            agentToManageOnePropertyNoExistingAgent(
              swapErrorKeyIfRequired[Boolean](errors, "onePropertyNoAgent", "error.boolean", "error.oneProperty.required"),
              getAgentName(request))))
      },
      success => {
        val choice = if(success) Yes.name else No.name
        sessionRepo.saveOrUpdate(request.sessionData.copy(managingProperty = Some(choice)))
        Future.successful(Redirect(controllers.agentAppointment.routes.AgentRelationshipController.checkAnswers()))
      }
    )
  }

  def onePropertyWithExistingAgent(): Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async { implicit request =>
    Future.successful(
      Ok(
        agentToManageOneProperty(
          manageOnePropertyExistingAgent,
          getAgentName(request))))
  }

  def submitOnePropertyWithExistingAgent(): Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async { implicit request =>
    manageOnePropertyExistingAgent.bindFromRequest.fold(
      errors => {
        Future.successful(
          BadRequest(
            agentToManageOneProperty(
              swapErrorKeyIfRequired[ManageOnePropertyOptions](errors, "onePropertyWithAgent", "error.common.noValueSelected", "error.oneProperty.required"),
              getAgentName(request))))
      },
      success => {
        sessionRepo.saveOrUpdate(request.sessionData.copy(managingProperty = Some(success.name)))
        Future.successful(Redirect(controllers.agentAppointment.routes.AgentRelationshipController.checkAnswers()))
      }
    )
  }

  def multipleProperties(): Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async { implicit request =>
    Future.successful(
      Ok(
        agentToManageMultipleProperties(
          manageMultipleProperties,
          getAgentName(request))))
  }

  def submitMultipleProperties(): Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async { implicit request =>
    manageMultipleProperties.bindFromRequest.fold(
      errors => {
        Future.successful(
          BadRequest(
            agentToManageMultipleProperties(
              swapErrorKeyIfRequired[ManageMultiplePropertiesOptions](errors, "multipleProperties", "error.common.noValueSelected", "error.multipleProperties.required"),
              getAgentName(request))))
      },
      {
        case ChooseFromList => joinOldJourney(request.sessionData.agentCode)
        case success@(propertyrepresentation.All | propertyrepresentation.None) => {
          sessionRepo.saveOrUpdate(data = request.sessionData.copy(managingProperty = Some(success.name)))
          Future.successful(Redirect(controllers.agentAppointment.routes.AgentRelationshipController.checkAnswers()))
        }
      }
    )
  }

  def checkAnswers(): Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async { implicit request =>
    Future.successful(Ok(checkYourAnswers(request.sessionData)))
  }

  private def getAgentName(request: AppointAgentSessionRequest[AnyContent]) = {
    request.sessionData.agentOrganisationName.getOrElse(throw new IllegalStateException("no agent name stored"))
  }

  private def joinOldJourney(agentCode: Long) = {
    Future.successful(Redirect(controllers.agentAppointment.routes.AppointAgentController.getMyOrganisationPropertyLinksWithAgentFiltering(
      pagination = PaginationParameters(page = 1, pageSize = 15),
      params = GetPropertyLinksParameters(sortfield = ExternalPropertyLinkManagementSortField.ADDRESS, sortorder = ExternalPropertyLinkManagementSortOrder.ASC),
      agentCode = agentCode,
      checkPermission = StartAndContinue.name,
      challengePermission = StartAndContinue.name,
      agentAppointed = Some(Both.name)
    )))
  }

  private def swapErrorKeyIfRequired[A](errors: Form[A], fieldName: String, errorMessageKeyToBeSwapped: String, newErrorMessageKey: String): Form[A] = {
    if(errors.errors.size == 1
      && errors.errors.head.messages.size == 1
      && errors.errors.head.messages.head == errorMessageKeyToBeSwapped){
      val updatedErrors = errors.copy(
        errors = Seq(FormError(fieldName, Seq(newErrorMessageKey), Seq.empty))
      )
      updatedErrors
    } else errors
  }
}
