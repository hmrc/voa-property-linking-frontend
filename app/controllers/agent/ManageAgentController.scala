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
import models.propertyrepresentation.{AgentList, AgentOrganisation, AgentSummary, ManageAgentOptions}
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

class ManageAgentController @Inject()(
        val errorHandler: CustomErrorHandler,
        authenticated: AuthenticatedAction,
        agentRelationshipService: AgentRelationshipService,
        manageAgentPage: views.html.propertyrepresentation.manage.manageAgent,
        manageAgentZeroPropertyLinks: views.html.propertyrepresentation.manage.manageAgentZeroPropertyLinks,
        manageAgentOnePropertyLinkWithAgentAssigned: views.html.propertyrepresentation.manage.manageAgentOnePropertyLinkWithAgentAssigned)(
        implicit override val messagesApi: MessagesApi,
        override val controllerComponents: MessagesControllerComponents,
        executionContext: ExecutionContext,
        val config: ApplicationConfig
      ) extends PropertyLinkingController {

  val logger = Logger(this.getClass.getName)

  //fixme change method name
  def manageAgents(): Action[AnyContent] = authenticated.async { implicit request =>
    for{
      organisationsAgents <- agentRelationshipService.getMyOrganisationAgents()
      propertyLinks <- agentRelationshipService.getMyOrganisationsPropertyLinks(
          searchParams = GetPropertyLinksParameters(),
          pagination = PaginationParams(startPoint = 1, pageSize = 1000, requestTotalRowCount = false),
          representationStatusFilter = Seq(RepresentationApproved, RepresentationPending)
        )
    }
    yield{
      (organisationsAgents, propertyLinks) match {
        case (agentsList: AgentList, links: OwnerAuthResult) if agentsList.resultCount == 0 && links.total == 0 => {
          logger.error("Organisation does not have any any property links or any agents to manage")
          NotFound(errorHandler.notFoundTemplate)
        }
        case (agentsList: AgentList, links: OwnerAuthResult) if agentsList.resultCount == 1 && links.total == 0 => {
          Ok(manageAgentZeroPropertyLinks(
            ManageAgentForms.manageAgent,
            agentsList.agents.head.name))
        }
        case (agentsList: AgentList, links: OwnerAuthResult) if agentsList.resultCount == 1 && links.total == 1 => {
          if(agentsList.agents.head.propertyCount == 1){
            showManageAgentOnePropertyLinkWithAgentAssigned(agentsList.agents.head.name)
          } else {
            Ok(manageAgentPage(
              ManageAgentForms.manageAgent,
              ManageAgentOptions.onePropertyLinkNoAssignedAgentsOptions,
              agentsList.agents.head.name))  //What do you want to do to the agent <Agent name> ? - AR
          }
        }
        case _ => Redirect(config.newDashboardUrl("your-agents")) //fixme this needs to be replaced by the new manage agents page
      }
    }
  }

  private def showManageAgentOnePropertyLinkWithAgentAssigned(agentName: String)(implicit request: Request[_]) = {
    Ok(manageAgentOnePropertyLinkWithAgentAssigned(
      ManageAgentForms.manageAgent,
      agentName)) //Would you like to unassign <Agent name> from your property?
  }

  def manageAgent(agentCode: Long): Action[AnyContent] = authenticated.async { implicit request =>
    for{
      organisationsAgents <- agentRelationshipService.getMyOrganisationAgents()
      agentToBeManaged = organisationsAgents.agents.find(a => a.representativeCode == agentCode)
      propertyLinks <- agentRelationshipService.getMyOrganisationsPropertyLinks(
        searchParams = GetPropertyLinksParameters(),
        pagination = PaginationParams(startPoint = 1, pageSize = 1000, requestTotalRowCount = false),
        representationStatusFilter = Seq(RepresentationApproved, RepresentationPending)
      )
    }
    yield {
      agentToBeManaged.fold(NotFound(errorHandler.notFoundTemplate)){agentSummary =>
        agentSummary.propertyCount match {
          case 0 =>
            Ok(manageAgentPage(
              ManageAgentForms.manageAgent,
              ManageAgentOptions.multiplePropertyLinksNoAssignedAgentsOptions,
              agentSummary.name)) //What do you want to do to the agent <Agent name > ? - AAR
          case num if num == propertyLinks.authorisations.size && propertyLinks.authorisations.size == 1 =>
            showManageAgentOnePropertyLinkWithAgentAssigned(agentSummary.name)
          case num if num == propertyLinks.authorisations.size =>
            Ok(manageAgentPage(
              ManageAgentForms.manageAgent,
              ManageAgentOptions.multiplePropertyLinksAgentAssignedToAllOptions,
              agentSummary.name)) //What do you want to do to the agent <Agent name> ? - UU
          case _ =>
            Ok(manageAgentPage(
              ManageAgentForms.manageAgent,
              ManageAgentOptions.multiplePropertyLinksAgentAssignedToSomeOptions,
              agentSummary.name))//What do you want to do to the agent <Agent name> ? - AAUU
        }
      }
    }
  }

  def submitManageAgent(): Action[AnyContent] = authenticated.async { implicit request =>
    ???
  }

//  private def numberOfPropertyLinksAgentIsAssignedTo(propertyLinks: OwnerAuthResult, agentCode: Long): Int = {
//    propertyLinks.authorisations.flatMap(pl => pl.agents.map(_.agentCode)).filter(x => x == agentCode).size
//  }
//
//  private def getAgentOrganisationName(agentOrganisationOpt: Option[AgentOrganisation]) = agentOrganisationOpt match {
//    case None => throw new IllegalStateException("Agent does not exist")
//    case Some(agentOrg) => agentOrg.organisationLatestDetail.organisationName
//  }
//
//  private def getAgentOrganisationName2(agentCode: Long)(implicit headerCarrier: HeaderCarrier): Future[String] = {
//    agentRelationshipService.getAgent(agentCode) map {
//      case None => throw new IllegalStateException("Agent does not exist")
//      case Some(agentOrg) => agentOrg.organisationLatestDetail.organisationName
//    }
//  }
//
//  private def getPropertyCountForAgent(agentList: AgentList, agentCode: Long): Int = {
//    agentList.agents.find(a => a.representativeCode == agentCode) match {
//      case None => throw new IllegalStateException(s"Agent $agentCode does not belong to organisation")
//      case Some(agentSummary) => agentSummary.propertyCount
//    }sb
//  }
//
//
//  private def numberOfPropertyLinksAgentsAreAssignedTo(propertyLinks: OwnerAuthResult, agentsList: AgentList): Int = {
//    val propertyLinksAgentCodes = propertyLinks.authorisations.flatMap(pl => pl.agents.map(_.agentCode))
//    val agentListAgentCodes = agentsList.agents.map(_.representativeCode)
//
//    propertyLinksAgentCodes.intersect(agentListAgentCodes).size
//  }

}
