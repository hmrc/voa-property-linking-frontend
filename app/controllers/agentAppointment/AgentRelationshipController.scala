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
import models.searchApi.AgentPropertiesFilter.No
import models.searchApi.AgentPropertiesSortField.Address
import play.api.data.{Form, Forms, Mapping}
import play.api.data.Forms.{longNumber, mapping, nonEmptyText, single}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepo
import services.AgentRelationshipService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.voa.propertylinking.errorhandler.CustomErrorHandler

import scala.concurrent.{ExecutionContext, Future}

class AgentRelationshipController @Inject()(val errorHandler: CustomErrorHandler,
                                            authenticated: AuthenticatedAction,
                                            withAppointAgentSession: WithAppointAgentSession,
                                            agentRelationshipService: AgentRelationshipService,
                                            agentsConnector: AgentsConnector,
                                            @Named("appointNewAgentSession") val sessionRepo: SessionRepo
                                           )(
                                             implicit override val messagesApi: MessagesApi,
                                             override val controllerComponents: MessagesControllerComponents,
                                             executionContext: ExecutionContext,
                                             val config: ApplicationConfig
                                           ) extends PropertyLinkingController {


  //fixme move forms elsewhere
  val agentCode: Form[Long] =
    Form(single("agentCode" -> {
      longNumber.verifying("error.agentCode.invalid", n => n > 0)
    }))

  val isThisYourAgent: Form[Boolean] = Form(Forms.single("isThisYourAgent" -> Mappings.mandatoryBoolean))

  val manageOnePropertyNoAgent: Form[Boolean] = Form(Forms.single("onePropertyNoAgent" -> Mappings.mandatoryBoolean))

  val manageOnePropertyExistingAgent: Form[ManageOnePropertyOptions] = Form(Forms.single("onePropertyExistingAgent" -> EnumMapping(ManageOnePropertyOptions)))

  val manageMultipleProperties: Form[ManageMultiplePropertiesOptions] = Form(Forms.single("multipleProperties" -> EnumMapping(ManageMultiplePropertiesOptions)))


  def start(): Action[AnyContent] = authenticated.async { implicit request =>
    Future.successful(Ok(views.html.propertyrepresentation.appoint.start(agentCode)))
  }

  def getAgentDetails(): Action[AnyContent] = authenticated.async { implicit request =>
    agentCode.bindFromRequest.fold(
      errors => {
        Future.successful(BadRequest(views.html.propertyrepresentation.appoint.start(errors)))
      },
      success => {
        agentRelationshipService.getAgent(success) map {
          case None => ???
          case Some(a) => {
            sessionRepo.saveOrUpdate(AppointNewAgentSession(agentCode = success, agentOrganisationName = Some(a.organisationLatestDetail.organisationName)))
            Redirect(controllers.agentAppointment.routes.AgentRelationshipController.isCorrectAgent())
          }
        }
      }
    )
  }

  def isCorrectAgent(): Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async { implicit request =>
    Future.successful(
      Ok(
        views.html.propertyrepresentation.appoint.isThisYourAgent(
          isThisYourAgent,
          request.sessionData.agentOrganisationName.getOrElse(throw new IllegalStateException("no agent name stored")))))
  }

  def agentSelected(): Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async { implicit request =>
    isThisYourAgent.bindFromRequest.fold(
      errors => {
        Future.successful(
          BadRequest(views.html.propertyrepresentation.appoint.isThisYourAgent(
            errors,
            request.sessionData.agentOrganisationName.getOrElse(throw new IllegalStateException("no agent name stored")))))
      },
      success => {
        if(success){
          for{
            propertyLinks <- agentRelationshipService.getMyOrganisationsPropertyLinks(
              searchParams = GetPropertyLinksParameters(),
              pagination = PaginationParams(startPoint = 1, pageSize = 1000, requestTotalRowCount = false),
              representationStatusFilter = Seq(RepresentationApproved, RepresentationPending)
            )
            ownerAgents <- agentsConnector.ownerAgents(request.organisationId)
            agentName = request.sessionData.agentOrganisationName.getOrElse(throw new IllegalStateException("no agent name stored"))
          } yield {
            propertyLinks.size match {
              case 0 => {
                //fixme send request to modernised
                Ok(views.html.propertyrepresentation.appoint.confirmation(agentName))
              }
              case 1 => {
                ownerAgents.agents.size match {
                  case 0 => Ok(views.html.propertyrepresentation.appoint.agentToManageOnePropertyNoExistingAgent(manageOnePropertyNoAgent, agentName))
                  case _ => Ok(views.html.propertyrepresentation.appoint.agentToManageOneProperty(manageOnePropertyExistingAgent, agentName))
                }
              }
              case _ => Ok(views.html.propertyrepresentation.appoint.agentToManageMultipleProperties(manageMultipleProperties, agentName))
            }
          }
        }
        else{
          Future.successful(Ok(views.html.propertyrepresentation.appoint.start(agentCode)))
        }
      }
    )
  }

  def onePropertyNoExistingAgent(): Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async { implicit request =>
    Future.successful(
      Ok(
        views.html.propertyrepresentation.appoint.agentToManageOnePropertyNoExistingAgent(
          manageOnePropertyNoAgent,
          request.sessionData.agentOrganisationName.getOrElse(throw new IllegalStateException("no agent name stored")))))
  }

  def submitOnePropertyNoExistingAgent(): Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async { implicit request =>
    manageOnePropertyNoAgent.bindFromRequest.fold(
      errors => {
        Future.successful(
          BadRequest(
            views.html.propertyrepresentation.appoint.agentToManageOnePropertyNoExistingAgent(
              errors,
              request.sessionData.agentOrganisationName.getOrElse(throw new IllegalStateException("no agent name stored")))))
      },
      success => {
        val choice = if(success) Yes.name else No.name
        sessionRepo.saveOrUpdate(request.sessionData.copy(managingProperty = Some(choice)))
        Future.successful(Redirect(controllers.agentAppointment.routes.AgentRelationshipController.checkYourAnswers()))
      }
    )
  }

  def onePropertyWithExistingAgent(): Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async { implicit request =>
    Future.successful(
      Ok(
        views.html.propertyrepresentation.appoint.agentToManageOneProperty(
          manageOnePropertyExistingAgent,
          request.sessionData.agentOrganisationName.getOrElse(throw new IllegalStateException("no agent name stored")))))
  }

  def submitOnePropertyWithExistingAgent(): Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async { implicit request =>
    manageOnePropertyExistingAgent.bindFromRequest.fold(
      errors => {
        Future.successful(BadRequest(views.html.propertyrepresentation.appoint.agentToManageOneProperty(errors, request.sessionData.agentOrganisationName.getOrElse(???))))
      },
      success => {
        sessionRepo.saveOrUpdate(request.sessionData.copy(managingProperty = Some(success.name)))
        Future.successful(Redirect(controllers.agentAppointment.routes.AgentRelationshipController.checkYourAnswers()))
      }
    )
  }

  def multipleProperties(): Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async { implicit request =>
    Future.successful(
      Ok(
        views.html.propertyrepresentation.appoint.agentToManageMultipleProperties(
          manageMultipleProperties,
          request.sessionData.agentOrganisationName.getOrElse(throw new IllegalStateException("no agent name stored")))))
  }

  def submitMultipleProperties(): Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async { implicit request =>
    manageMultipleProperties.bindFromRequest.fold(
      errors => {
        Future.successful(BadRequest(views.html.propertyrepresentation.appoint.agentToManageMultipleProperties(errors, request.sessionData.agentOrganisationName.getOrElse(???))))
      },
      success => {
        success match {
          case ChooseFromList => joinOldJourney(request.sessionData.agentCode)
          case propertyrepresentation.All | propertyrepresentation.None => {
            sessionRepo.get[AppointNewAgentSession] flatMap {
              case None => ???
              case Some(details) => {
                sessionRepo.saveOrUpdate(details.copy(managingProperty = Some(success.name)))
                Future.successful(Redirect(controllers.agentAppointment.routes.AgentRelationshipController.checkYourAnswers()))
              }
            }
          }
        }
      }
    )
  }

  def checkYourAnswers(): Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async { implicit request =>
    Future.successful(Ok(views.html.propertyrepresentation.appoint.checkYourAnswers(request.sessionData)))
  }

  private def joinOldJourney(agentCode: Long) = {
    Future.successful(Redirect(controllers.agentAppointment.routes.AppointAgentController.getMyOrganisationPropertyLinksWithAgentFiltering(
      pagination = PaginationParameters(page = 1, pageSize = 15),
      params = GetPropertyLinksParameters(sortfield = ExternalPropertyLinkManagementSortField.ADDRESS, sortorder = ExternalPropertyLinkManagementSortOrder.ASC),
      agentCode = agentCode,
      checkPermission = StartAndContinue.name,
      challengePermission = StartAndContinue.name,
      agentAppointed = Some(No.name)
    )))
  }
}
