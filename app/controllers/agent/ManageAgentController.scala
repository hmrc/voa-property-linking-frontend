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

package controllers.agent

import actions.AuthenticatedAction
import binders.pagination.PaginationParameters
import binders.propertylinks.{ExternalPropertyLinkManagementSortField, ExternalPropertyLinkManagementSortOrder, GetPropertyLinksParameters}
import config.ApplicationConfig
import controllers.{PaginationParams, PropertyLinkingController}
import form.EnumMapping
import javax.inject.{Inject, Named}

import binders.pagination.PaginationParameters
import controllers.agent.forms.{AgentPropertiesForm, FilterAgentProperties}
import models.propertyrepresentation.AgentAppointmentChangesRequest.submitAgentAppointmentRequest
import models.{RepresentationApproved, RepresentationPending}
import models.propertyrepresentation._
import models.searchApi.AgentPropertiesFilter.Both
import models.searchApi.{AgentPropertiesParameters, OwnerAuthResult}
import play.api.Logger
import play.api.data.{Form, FormError, Forms}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import services.AgentRelationshipService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class ManageAgentController @Inject()(
      val errorHandler: CustomErrorHandler,
      authenticated: AuthenticatedAction,
      agentRelationshipService: AgentRelationshipService,
      manageAgentPage: views.html.propertyrepresentation.manage.manageAgent,
      myAgentsPage: views.html.propertyrepresentation.manage.myAgents,
      removeAgentFromOrganisation: views.html.propertyrepresentation.manage.removeAgentFromOrganisation,
      unassignAgentFromProperty: views.html.propertyrepresentation.manage.unassignAgentFromProperty,
      addAgentToAllProperties: views.html.propertyrepresentation.manage.addAgentToAllProperties,
      confirmAddAgentToAllProperties: views.html.propertyrepresentation.manage.confirmAddAgentToAllProperties,
      unassignAgentFromAllProperties: views.html.propertyrepresentation.manage.unassignAgentFromAllProperties,
      confirmUnassignAgentFromAllProperties: views.html.propertyrepresentation.manage.confirmUnassignAgentFromAllProperties,
      confirmRemoveAgentFromOrganisation: views.html.propertyrepresentation.manage.confirmRemoveAgentFromOrganisation,
      manageAgentSimplePage: views.html.propertyrepresentation.manage.manageAgentSimpleProperties)(
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
        myAgentsPage(organisationsAgents.agents, propertyLinksCount)
      )

  }

  def manageAgent(agentCode: Option[Long]): Action[AnyContent] = authenticated.async { implicit request =>
    getManageAgentPage(agentCode).map {
      case None       => NotFound(errorHandler.notFoundTemplate)
      case Some(page) => Ok(page)
    }
  }

  def manageAgentProperties(
        agentCode: Long,
        params: GetPropertyLinksParameters,
        pagination: PaginationParameters): Action[AnyContent] = authenticated.async { implicit request =>
    {
      for {
        ownerAuthResult <- agentRelationshipService
                            .getMyAgentPropertyLinks(agentCode, params, PaginationParams(1, 1000, true))
        agentDetails <- agentRelationshipService.getAgentNameAndAddress(agentCode)
      } yield {
        Ok(manageAgentSimplePage(ownerAuthResult, agentCode, agentDetails))
      }
    }
  }

  def manageAgentSearchSortProperties(
        agentCode: Long,
        params: GetPropertyLinksParameters,
        pagination: PaginationParameters): Action[AnyContent] = authenticated.async { implicit request =>
    {

      AgentPropertiesForm.filterPropertiesForm.bindFromRequest.fold(
        errors => {
          for {
            ownerAuthResult <- agentRelationshipService.getMyAgentPropertyLinks(
                                agentCode,
                                params,
                                PaginationParams(pagination.startPoint, pagination.pageSize, true))
            agentDetails <- agentRelationshipService.getAgentNameAndAddress(agentCode)
          } yield
            (BadRequest(
              views.html.propertyrepresentation.manage
                .manageAgentProperties(errors, ownerAuthResult, agentCode, agentDetails, params, pagination)))
        },
        success =>
          Future.successful(
            Redirect(
              routes.ManageAgentController.manageAgentProperties(
                agentCode,
                params.copy(address = success.address, baref = success.localAuthorityReference),
                pagination)))
      )

    }
  }
  private[agent] def getManageAgentPage(
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
                                           pageSize = 1000,
                                           requestTotalRowCount = false),
                                         representationStatusFilter =
                                           Seq(RepresentationApproved, RepresentationPending)
                                       )
      ipPropertyLinksCount = propertyLinks.total
    } yield {
      (ipPropertyLinksCount, agentToBeManagedOpt) match {
        case (0, Some(agent)) if agent.propertyCount == 0 =>
          //IP has no property links but still has an agent
          Some(removeAgentFromOrganisation(submitAgentAppointmentRequest, agent.representativeCode, agent.name))
        case (1, Some(agent)) if agent.propertyCount == 0 =>
          //IP has one property link but agent is not assigned
          Some(
            manageAgentPage(submitManageAgentForm, ManageAgentOptions.onePropertyLinkNoAssignedAgentsOptions, agent))
        case (1, Some(agent)) if agent.propertyCount == 1 =>
          //IP has one property link and agent is assigned to that property
          Some(unassignAgentFromProperty(submitManageAgentForm, agent))
        case (numberOfPropertyLinks, Some(agent)) if numberOfPropertyLinks > 1 && agent.propertyCount == 0 =>
          //IP has more than one property links but agent is not assigned to any
          Some(
            manageAgentPage(
              submitManageAgentForm,
              ManageAgentOptions.multiplePropertyLinksNoAssignedAgentsOptions,
              agent))
        case (numberOfPropertyLinks, Some(agent)) if numberOfPropertyLinks > agent.propertyCount =>
          //agent is assigned to some (but not all) of the IP's property links
          Some(
            manageAgentPage(
              submitManageAgentForm,
              ManageAgentOptions.multiplePropertyLinksAgentAssignedToSomeOptions,
              agent))
        case (numberOfPropertyLinks, Some(agent)) if numberOfPropertyLinks == agent.propertyCount =>
          //agent is assigned to all of the IP's property links
          Some(
            manageAgentPage(
              submitManageAgentForm,
              ManageAgentOptions.multiplePropertyLinksAgentAssignedToAllOptions,
              agent))
        case _ => None
      }
    }

  def submitManageAgent(agentCode: Long): Action[AnyContent] = authenticated.async { implicit request =>
    ManageAgentRequest.submitManageAgentRequest.bindFromRequest.fold(
      errors => {
        getManageAgentPage(Some(agentCode), errors).map {
          case None       => NotFound(errorHandler.notFoundTemplate)
          case Some(page) => BadRequest(page)
        }
      }, { success =>
        success.manageAgentOption match {
          case AssignToSomeProperties => Future.successful(joinOldAgentAppointJourney(agentCode))
          case AssignToAllProperties | AssignToYourProperty =>
            Future.successful(Ok(addAgentToAllProperties(submitAgentAppointmentRequest, success.agentName, agentCode)))
          case UnassignFromAllProperties =>
            Future.successful(
              Ok(unassignAgentFromAllProperties(submitAgentAppointmentRequest, success.agentName, agentCode)))
          case UnassignFromSomeProperties => Future.successful(joinOldAgentRevokeJourney(agentCode))
          case RemoveFromYourAccount =>
            Future.successful(
              Ok(removeAgentFromOrganisation(submitAgentAppointmentRequest, agentCode, success.agentName)))
        }
      }
    )
  }

  def assignAgentToAll(agentCode: Long, agentName: String): Action[AnyContent] = authenticated.async {
    implicit request =>
      submitAgentAppointmentRequest.bindFromRequest.fold(
        errors => {
          Future.successful(BadRequest(addAgentToAllProperties(errors, agentName, agentCode)))
        }, { success =>
          agentRelationshipService
            .assignAgent(success)
            .map { _ =>
              val page = confirmAddAgentToAllProperties(agentName)
              Ok(page)
            }
        }
      )
  }

  def unassignAgentFromAll(agentCode: Long, agentName: String): Action[AnyContent] = authenticated.async {
    implicit request =>
      submitAgentAppointmentRequest.bindFromRequest.fold(
        errors => {
          Future.successful(BadRequest(unassignAgentFromAllProperties(errors, agentName, agentCode)))
        }, { success =>
          agentRelationshipService
            .unassignAgent(success)
            .map(_ => Ok(confirmUnassignAgentFromAllProperties(agentName, agentCode)))
        }
      )
  }

  def showRemoveAgentFromIpOrganisation(agentCode: Long, agentName: String): Action[AnyContent] = authenticated.async {
    implicit request =>
      Future.successful(Ok(removeAgentFromOrganisation(submitAgentAppointmentRequest, agentCode, agentName)))
  }

  def removeAgentFromIpOrganisation(agentCode: Long, agentName: String): Action[AnyContent] = authenticated.async {
    implicit request =>
      submitAgentAppointmentRequest.bindFromRequest.fold(
        errors => {
          Future.successful(BadRequest(removeAgentFromOrganisation(errors, agentCode, agentName)))
        }, { success =>
          agentRelationshipService
            .removeAgentFromOrganisation(success)
            .map(_ => Ok(confirmRemoveAgentFromOrganisation(agentName)))
        }
      )
  }

  private def joinOldAgentAppointJourney(agentCode: Long) =
    Redirect(
      controllers.agentAppointment.routes.AppointAgentController.getMyOrganisationPropertyLinksWithAgentFiltering(
        pagination = PaginationParameters(),
        params = GetPropertyLinksParameters(
          sortfield = ExternalPropertyLinkManagementSortField.ADDRESS,
          sortorder = ExternalPropertyLinkManagementSortOrder.ASC),
        agentCode = agentCode,
        agentAppointed = Some(Both.name),
        backLink = controllers.agent.routes.ManageAgentController.manageAgent(Some(agentCode)).url
      ))

  private def joinOldAgentRevokeJourney(agentCode: Long) =
    Redirect(
      controllers.agentAppointment.routes.AppointAgentController.selectAgentPropertiesSearchSort(
        pagination = PaginationParameters(),
        params = GetPropertyLinksParameters(
          sortfield = ExternalPropertyLinkManagementSortField.ADDRESS,
          sortorder = ExternalPropertyLinkManagementSortOrder.ASC),
        agentCode = agentCode,
        backLink = controllers.agent.routes.ManageAgentController.manageAgent(Some(agentCode)).url
      ))
}
