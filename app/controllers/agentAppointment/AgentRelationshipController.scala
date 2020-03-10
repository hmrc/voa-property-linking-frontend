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
import actions.agentrelationship.WithAppointAgentSessionRefiner
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
import models.propertyrepresentation.{AppointAgentRequest, AppointNewAgentSession, AppointmentScope, ChooseFromList, ManagePropertiesOptions, ManagingProperty, SearchedAgent, SelectedAgent, Yes}
import models.propertyrepresentation.AppointAgentRequest.submitAppointAgentRequest
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
import controllers.agent.routes
import models.searchApi.AgentPropertiesParameters
import views.html.propertyrepresentation.appoint._
import controllers.agentAppointment.AppointNewAgentForms._
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class AgentRelationshipController @Inject()(
      val errorHandler: CustomErrorHandler,
      authenticated: AuthenticatedAction,
      withAppointAgentSession: WithAppointAgentSessionRefiner,
      agentRelationshipService: AgentRelationshipService,
      agentsConnector: AgentsConnector,
      @Named("appointNewAgentSession") val sessionRepo: SessionRepo,
      startPage: views.html.propertyrepresentation.appoint.start,
      isTheCorrectAgent: views.html.propertyrepresentation.appoint.isThisYourAgent,
      agentToManageOneProperty: views.html.propertyrepresentation.appoint.agentToManageOneProperty,
      agentToManageMultipleProperties: views.html.propertyrepresentation.appoint.agentToManageMultipleProperties,
      checkYourAnswers: views.html.propertyrepresentation.appoint.checkYourAnswers,
      confirmation: views.html.propertyrepresentation.appoint.confirmation)(
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
        Future.successful(BadRequest(startPage(errors)))
      },
      success => {
        Try(success.toLong).toOption match {
          case None =>
            Future.successful(
              BadRequest(
                startPage(
                  agentCode.copy(
                    data = Map("agentCode" -> success),
                    errors = Seq[FormError](FormError(key = "agentCode", message = "error.agentCode.required")))
                )))
          case Some(representativeCode) => {
            for {
              organisationsAgents    <- agentRelationshipService.getMyOrganisationAgents()
              agentNameAndAddressOpt <- agentRelationshipService.getAgentNameAndAddress(success.toLong)
            } yield {
              agentNameAndAddressOpt match {
                case None => {
                  BadRequest(
                    startPage(
                      agentCode.copy(
                        data = Map("agentCode" -> s"$representativeCode"),
                        errors = Seq[FormError](
                          FormError(key = "agentCode", message = "error.propertyRepresentation.unknownAgent")))
                    ))
                }
                case Some(agent)
                    if organisationsAgents.agents
                      .filter(a => a.representativeCode == representativeCode)
                      .nonEmpty =>
                  BadRequest(
                    startPage(
                      agentCode.copy(
                        data = Map("agentCode" -> s"$representativeCode"),
                        errors = Seq[FormError](
                          FormError(key = "agentCode", message = "error.propertyRepresentation.agentAlreadyAppointed"))
                      )
                    ))
                case Some(agent) => {
                  sessionRepo.saveOrUpdate(
                    SearchedAgent(
                      agentCode = representativeCode,
                      agentOrganisationName = agent.name,
                      agentAddress = agent.address
                    ))
                  Redirect(controllers.agentAppointment.routes.AgentRelationshipController.isCorrectAgent())
                }
              }
            }
          }
        }
      }
    )
  }

  def isCorrectAgent(): Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async { implicit request =>
    Future.successful(Ok(isTheCorrectAgent(isThisTheCorrectAgent, request.agentDetails)))
  }

  def agentSelected(): Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async { implicit request =>
    isThisTheCorrectAgent.bindFromRequest.fold(
      errors => {
        Future.successful(BadRequest(isTheCorrectAgent(errors, request.agentDetails)))
      },
      success => {
        if (success) {
          for {
            searchedAgentOpt <- sessionRepo.get[SearchedAgent]
            searchedAgent = searchedAgentOpt.getOrElse(throw NoAgentSavedException("no agent saved"))
            _ <- sessionRepo.saveOrUpdate(SelectedAgent(searchedAgent, success))
            propertyLinks <- agentRelationshipService.getMyOrganisationPropertyLinksWithAgentFiltering(
                              params = GetPropertyLinksParameters(),
                              pagination = AgentPropertiesParameters(agentCode = searchedAgent.agentCode),
                              agentOrganisationId = searchedAgent.agentCode,
                              organisationId = request.organisationId
                            )
          } yield {
            propertyLinks.authorisations.size match {
              case 0 => {
                agentRelationshipService.sendAppointAgentRequest(
                  AppointAgentRequest(
                    scope = AppointmentScope.RELATIONSHIP.toString,
                    agentRepresentativeCode = searchedAgent.agentCode
                  )
                )
                Ok(confirmation(request.agentDetails.name))
              }
              case 1 => Redirect(controllers.agentAppointment.routes.AgentRelationshipController.oneProperty())
              case _ => Redirect(controllers.agentAppointment.routes.AgentRelationshipController.multipleProperties())
            }
          }
        } else {
          Future.successful(
            Redirect(controllers.agentAppointment.routes.AgentRelationshipController.startAppointJourney()))
        }
      }
    )
  }
  def oneProperty(): Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async { implicit request =>
    Future.successful(Ok(agentToManageOneProperty(manageOneProperty, request.agentDetails.name)))
  }

  def submitOneProperty(): Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async {
    implicit request =>
      manageOneProperty.bindFromRequest.fold(
        errors => {
          Future.successful(
            BadRequest(
              agentToManageOneProperty(
                errors,
                request.agentDetails.name
              )))
        },
        success => {
          for {
            selectedAgentOpt <- sessionRepo.get[SelectedAgent]
            selectedAgent = selectedAgentOpt.getOrElse(throw NoAgentSavedException("no agent saved"))
            _ <- sessionRepo.saveOrUpdate(ManagingProperty(selectedAgent, success.name))
          } yield {
            Redirect(controllers.agentAppointment.routes.AgentRelationshipController.checkAnswers())
          }
        }
      )
  }

  def multipleProperties(): Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async {
    implicit request =>
      Future.successful(Ok(agentToManageMultipleProperties(manageMultipleProperties, request.agentDetails.name)))
  }

  def submitMultipleProperties(): Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async {
    implicit request =>
      manageMultipleProperties.bindFromRequest.fold(
        errors => {
          Future.successful(
            BadRequest(
              agentToManageMultipleProperties(
                errors,
                request.agentDetails.name
              )))
        },
        success => {
          for {
            selectedAgentOpt <- sessionRepo.get[SelectedAgent]
            selectedAgent = selectedAgentOpt.getOrElse(throw NoAgentSavedException("no agent saved"))
            _ <- sessionRepo.saveOrUpdate(ManagingProperty(selectedAgent, success.name))
          } yield {
            success match {
              case ChooseFromList => joinOldJourney(selectedAgent.agentCode)
              case propertyrepresentation.All | propertyrepresentation.None =>
                Redirect(controllers.agentAppointment.routes.AgentRelationshipController.checkAnswers())
            }
          }
        }
      )
  }

  def checkAnswers(): Action[AnyContent] = authenticated.andThen(withAppointAgentSession) { implicit request =>
    PartialFunction
      .condOpt(request.sessionData) {
        case data: ManagingProperty =>
          Ok(checkYourAnswers(submitAppointAgentRequest, data))
      }
      .getOrElse(NotFound(errorHandler.notFoundTemplate))
  }

  def appointAgent(): Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async { implicit request =>
    submitAppointAgentRequest.bindFromRequest.fold(
      errors => {
        PartialFunction
          .condOpt(request.sessionData) {
            case data: ManagingProperty =>
              Future.successful(
                BadRequest(
                  checkYourAnswers(
                    errors,
                    data
                  )))
          }
          .getOrElse(Future.successful(NotFound(errorHandler.notFoundTemplate)))
      }, { success =>
        {
          agentRelationshipService
            .sendAppointAgentRequest(success)
            .map(_ => Ok(confirmation(request.agentDetails.name)))
        }
      }
    )
  }

  private def joinOldJourney(agentCode: Long) =
    Redirect(
      controllers.agentAppointment.routes.AppointAgentController.getMyOrganisationPropertyLinksWithAgentFiltering(
        pagination = PaginationParameters(page = 1, pageSize = 15),
        params = GetPropertyLinksParameters(
          sortfield = ExternalPropertyLinkManagementSortField.ADDRESS,
          sortorder = ExternalPropertyLinkManagementSortOrder.ASC),
        agentCode = agentCode,
        checkPermission = StartAndContinue.name,
        challengePermission = StartAndContinue.name,
        agentAppointed = Some(Both.name),
        backLink = controllers.agentAppointment.routes.AgentRelationshipController.multipleProperties().url
      ))

}

case class NoAgentSavedException(message: String) extends Exception(message)
