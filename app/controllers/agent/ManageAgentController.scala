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

package controllers.agent

import actions.AuthenticatedAction
import binders.pagination.PaginationParameters
import binders.propertylinks.GetPropertyLinksParameters
import config.ApplicationConfig
import controllers.{PaginationParams, PropertyLinkingController}
import models.propertyrepresentation.AgentAppointmentChangesRequest.submitAgentAppointmentRequest
import models.propertyrepresentation._
import models.searchApi.AgentPropertiesFilter.Both
import models.searchApi.OwnerAuthResult
import play.api.Logger
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import services.AgentRelationshipService
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ManageAgentController @Inject()(
      val errorHandler: CustomErrorHandler,
      authenticated: AuthenticatedAction,
      agentRelationshipService: AgentRelationshipService,
      manageAgentView: views.html.propertyrepresentation.manage.manageAgent,
      myAgentsView: views.html.propertyrepresentation.manage.myAgents,
      removeAgentFromOrganisationView: views.html.propertyrepresentation.manage.removeAgentFromOrganisation,
      unassignAgentFromPropertyView: views.html.propertyrepresentation.manage.unassignAgentFromProperty,
      addAgentToAllPropertiesView: views.html.propertyrepresentation.manage.addAgentToAllProperties,
      confirmAddAgentToAllPropertiesView: views.html.propertyrepresentation.manage.confirmAddAgentToAllProperties,
      unassignAgentFromAllPropertiesView: views.html.propertyrepresentation.manage.unassignAgentFromAllProperties,
      confirmUnassignAgentFromAllPropertiesView: views.html.propertyrepresentation.manage.confirmUnassignAgentFromAllProperties,
      confirmRemoveAgentFromOrganisationView: views.html.propertyrepresentation.manage.confirmRemoveAgentFromOrganisation,
      manageAgentPropertiesView: views.html.propertyrepresentation.manage.manageAgentProperties
)(
      implicit override val messagesApi: MessagesApi,
      override val controllerComponents: MessagesControllerComponents,
      executionContext: ExecutionContext,
      val config: ApplicationConfig
) extends PropertyLinkingController {

  val logger = Logger(this.getClass.getName)

  def showAgents(): Action[AnyContent] = authenticated.async { implicit request =>
    for {
      organisationsAgents <- agentRelationshipService.getMyOrganisationAgents()
      propertyLinksCount  <- agentRelationshipService.getMyOrganisationPropertyLinksCount()
    } yield
      Ok(
        myAgentsView(organisationsAgents.agents, propertyLinksCount)
      )

  }

  def manageAgent(agentCode: Option[Long]): Action[AnyContent] = authenticated.async { implicit request =>
    getManageAgentView(agentCode).map {
      case None       => NotFound(errorHandler.notFoundTemplate)
      case Some(page) => Ok(page)
    }
  }

  def manageAgentProperties(
        agentCode: Long,
        params: GetPropertyLinksParameters = GetPropertyLinksParameters(),
        propertyLinkId: Option[Long] = None,
        valuationId: Option[Long] = None,
        propertyLinkSubmissionId: Option[String] = None,
        uarn: Option[Long] = None): Action[AnyContent] = authenticated.async { implicit request =>
    {
      for {
        ownerAuthResult <- agentRelationshipService
                            .getMyAgentPropertyLinks(agentCode, params, PaginationParams(1, 100, true))
        agentDetails <- agentRelationshipService.getAgentNameAndAddress(agentCode)
        backLink = (propertyLinkId, valuationId, propertyLinkSubmissionId, uarn) match {
          case (Some(linkId), Some(valId), Some(submissionId), None) =>
            config.businessRatesValuationFrontendUrl(
              s"property-link/$linkId/valuations/$valId?submissionId=$submissionId#agents-tab")
          case (None, Some(valId), Some(submissionId), Some(u)) =>
            s"${controllers.detailedvaluationrequest.routes.DvrController.myOrganisationRequestDetailValuationCheck(propertyLinkSubmissionId = submissionId, valuationId = valId, uarn = u).url}#agents-tab"
          case _ => controllers.agent.routes.ManageAgentController.showAgents.url
        }
      } yield {
        Ok(manageAgentPropertiesView(ownerAuthResult, agentCode, agentDetails, backLink))
      }
    }
  }

  private[agent] def getManageAgentView(
        agentCode: Option[Long],
        submitManageAgentForm: Form[ManageAgentRequest] = ManageAgentRequest.submitManageAgentRequest)(
        implicit request: Request[_]) =
    for {
      organisationsAgents <- agentRelationshipService.getMyOrganisationAgents()
      agentToBeManagedOpt = agentCode match {
        case None if organisationsAgents.agents.size == 1 => Some(organisationsAgents.agents.head)
        case Some(code)                                   => organisationsAgents.agents.find(a => a.representativeCode == code).map(agent => agent)
        case _                                            => None
      }
      propertyLinks: OwnerAuthResult <- agentRelationshipService.getMyOrganisationsPropertyLinks(
                                         searchParams = GetPropertyLinksParameters(),
                                         pagination = PaginationParams(
                                           startPoint = 1,
                                           pageSize = 100,
                                           requestTotalRowCount = false)
                                       )
      ipPropertyLinksCount = propertyLinks.total
    } yield {
      (ipPropertyLinksCount, agentToBeManagedOpt) match {
        case (0, Some(agent)) if agent.propertyCount == 0 =>
          //IP has no property links but still has an agent
          Some(
            removeAgentFromOrganisationView(
              submitAgentAppointmentRequest,
              agent.representativeCode,
              agent.name,
              calculateBackLink(organisationsAgents, agent.representativeCode)))
        case (1, Some(agent)) if agent.propertyCount == 0 =>
          //IP has one property link but agent is not assigned
          Some(
            manageAgentView(
              submitManageAgentForm,
              ManageAgentOptions.onePropertyLinkNoAssignedAgentsOptions,
              agent,
              calculateBackLink(organisationsAgents, agent.representativeCode)))
        case (1, Some(agent)) if agent.propertyCount == 1 =>
          //IP has one property link and agent is assigned to that property
          Some(
            unassignAgentFromPropertyView(
              submitAgentAppointmentRequest,
              agent,
              calculateBackLink(organisationsAgents, agent.representativeCode)))

        case (numberOfPropertyLinks, Some(agent)) if numberOfPropertyLinks > 1 && agent.propertyCount == 0 =>
          //IP has more than one property links but agent is not assigned to any
          Some(
            manageAgentView(
              submitManageAgentForm,
              ManageAgentOptions.multiplePropertyLinksNoAssignedAgentsOptions,
              agent,
              calculateBackLink(organisationsAgents, agent.representativeCode)
            ))

        case (numberOfPropertyLinks, Some(agent)) if numberOfPropertyLinks > agent.propertyCount =>
          Some(
            manageAgentView(
              submitManageAgentForm,
              ManageAgentOptions.multiplePropertyLinksAgentAssignedToSomeOptions,
              agent,
              calculateBackLink(organisationsAgents, agent.representativeCode)
            ))
        case (numberOfPropertyLinks, Some(agent)) if numberOfPropertyLinks == agent.propertyCount =>
          //agent is assigned to all of the IP's property links
          Some(
            manageAgentView(
              submitManageAgentForm,
              ManageAgentOptions.multiplePropertyLinksAgentAssignedToAllOptions,
              agent,
              calculateBackLink(organisationsAgents, agent.representativeCode)
            ))
        case _ => None
      }
    }

  def submitManageAgent(agentCode: Long): Action[AnyContent] = authenticated.async { implicit request =>
    ManageAgentRequest.submitManageAgentRequest.bindFromRequest.fold(
      errors => {
        getManageAgentView(Some(agentCode), errors).map {
          case None       => NotFound(errorHandler.notFoundTemplate)
          case Some(page) => BadRequest(page)
        }
      }, { success =>
        success.manageAgentOption match {
          case AssignToSomeProperties => Future.successful(joinOldAgentAppointJourney(agentCode))
          case AssignToAllProperties | AssignToYourProperty =>
            agentRelationshipService
              .getMyOrganisationPropertyLinksCount()
              .map(
                linkCount =>
                  Ok(
                    addAgentToAllPropertiesView(
                      submitAgentAppointmentRequest,
                      success.agentName,
                      agentCode,
                      multiplePropertyLinks = linkCount > 1)))
          case UnassignFromAllProperties =>
            Future.successful(
              Ok(unassignAgentFromAllPropertiesView(submitAgentAppointmentRequest, success.agentName, agentCode)))
          case UnassignFromSomeProperties => Future.successful(joinOldAgentRevokeJourney(agentCode))
          case RemoveFromYourAccount =>
            Future.successful(
              Ok(
                removeAgentFromOrganisationView(
                  submitAgentAppointmentRequest,
                  agentCode,
                  success.agentName,
                  controllers.agent.routes.ManageAgentController.manageAgent(Some(agentCode)).url)))
        }
      }
    )
  }

  def assignAgentToAll(agentCode: Long, agentName: String): Action[AnyContent] = authenticated.async {
    implicit request =>
      submitAgentAppointmentRequest.bindFromRequest.fold(
        errors => {
          agentRelationshipService
            .getMyOrganisationPropertyLinksCount()
            .map(linkCount =>
              BadRequest(
                addAgentToAllPropertiesView(errors, agentName, agentCode, multiplePropertyLinks = linkCount > 1)))
        }, { success =>
          for {
            _         <- agentRelationshipService.assignAgent(success)
            linkCount <- agentRelationshipService.getMyOrganisationPropertyLinksCount()
          } yield Ok(confirmAddAgentToAllPropertiesView(agentName, multiplePropertyLinks = linkCount > 1))
        }
      )
  }

  def unassignAgentFromAll(agentCode: Long, agentName: String): Action[AnyContent] = authenticated.async {
    implicit request =>
      submitAgentAppointmentRequest.bindFromRequest.fold(
        errors => {
          Future.successful(BadRequest(unassignAgentFromAllPropertiesView(errors, agentName, agentCode)))
        }, { success =>
          for {
            _         <- agentRelationshipService.unassignAgent(success)
            linkCount <- agentRelationshipService.getMyOrganisationPropertyLinksCount()
          } yield
            Ok(confirmUnassignAgentFromAllPropertiesView(agentName, agentCode, multiplePropertyLinks = linkCount > 1))
        }
      )
  }

  def showRemoveAgentFromIpOrganisation(agentCode: Long, agentName: String): Action[AnyContent] = authenticated.async {
    implicit request =>
      Future.successful(
        Ok(
          removeAgentFromOrganisationView(
            submitAgentAppointmentRequest,
            agentCode,
            agentName,
            config.dashboardUrl("home"))))
  }

  def removeAgentFromIpOrganisation(agentCode: Long, agentName: String, backLink: String): Action[AnyContent] =
    authenticated.async { implicit request =>
      submitAgentAppointmentRequest.bindFromRequest.fold(
        errors => {
          Future.successful(BadRequest(removeAgentFromOrganisationView(errors, agentCode, agentName, backLink)))
        }, { success =>
          agentRelationshipService
            .removeAgentFromOrganisation(success)
            .map(_ => Ok(confirmRemoveAgentFromOrganisationView(agentName)))
        }
      )
    }

  private def calculateBackLink(organisationsAgents: AgentList, agentCode: Long) =
    if (organisationsAgents.resultCount > 1)
      controllers.agent.routes.ManageAgentController
        .manageAgentProperties(agentCode, GetPropertyLinksParameters())
        .url
    else config.dashboardUrl("home")

  private def joinOldAgentAppointJourney(agentCode: Long) =
    Redirect(
      controllers.agentAppointment.routes.AppointAgentController.getMyOrganisationPropertyLinksWithAgentFiltering(
        pagination = PaginationParameters(),
        agentCode = agentCode,
        agentAppointed = Some(Both.name),
        backLink = controllers.agent.routes.ManageAgentController.manageAgent(Some(agentCode)).url
      ))

  private def joinOldAgentRevokeJourney(agentCode: Long) =
    Redirect(
      controllers.agentAppointment.routes.AppointAgentController.selectAgentPropertiesSearchSort(
        pagination = PaginationParameters(),
        agentCode = agentCode
      )
    )
}
