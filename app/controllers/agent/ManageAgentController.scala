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
import binders.propertylinks.GetPropertyLinksParameters
import config.ApplicationConfig
import controllers.{PaginationParams, PropertyLinkingController}
import form.EnumMapping
import javax.inject.{Inject, Named}
import models.{RepresentationApproved, RepresentationPending}
import models.propertyrepresentation.{AgentList, AgentOrganisation, AgentSummary, ManageAgentOptions, ManageAgentRequest}
import models.searchApi.AgentPropertiesFilter.Both
import models.searchApi.{AgentPropertiesParameters, OwnerAuthResult}
import play.api.Logger
import play.api.data.{Form, Forms}
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
      removeAgentFromOrganisation: views.html.propertyrepresentation.manage.removeAgentFromOrganisation,
      unassignAgentFromProperty: views.html.propertyrepresentation.manage.unassignAgentFromProperty)(
      implicit override val messagesApi: MessagesApi,
      override val controllerComponents: MessagesControllerComponents,
      executionContext: ExecutionContext,
      val config: ApplicationConfig
) extends PropertyLinkingController {

  val logger = Logger(this.getClass.getName)

  def manageAgent(agentCode: Option[Long]): Action[AnyContent] = authenticated.async { implicit request =>
    getManageAgentPage(agentCode).map {
      case None       => NotFound(errorHandler.notFoundTemplate)
      case Some(page) => Ok(page)
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
    } yield {
      (propertyLinks.total, agentToBeManagedOpt) match {
        case (0, Some(agent)) if agent.propertyCount == 0 =>
          //IP has no property links but still has an agent
          Some(removeAgentFromOrganisation(submitManageAgentForm, agent))
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
          //agent is not assigned to all of the IP's property links
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
        ??? //fixme this will be implemented in subsequent stories (VTCCA-3208, VTCCA-3207, VTCCA-3205)
      }
    )
  }

}
