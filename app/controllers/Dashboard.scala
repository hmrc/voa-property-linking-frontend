/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers

import java.time._

import actions.AuthenticatedAction
import binders.propertylinks.GetPropertyLinksParameters
import config.ApplicationConfig
import connectors._
import javax.inject.Inject
import models._
import models.searchApi.{OwnerAuthResult, OwnerAuthorisation}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.AgentRelationshipService
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import scala.concurrent.ExecutionContext

class Dashboard @Inject()(
      val errorHandler: CustomErrorHandler,
      propertyLinks: AgentRelationshipService,
      groupAccounts: GroupAccounts,
      authenticated: AuthenticatedAction,
      override val controllerComponents: MessagesControllerComponents
)(
      implicit executionContext: ExecutionContext,
      override val messagesApi: MessagesApi,
      val config: ApplicationConfig
) extends PropertyLinkingController {

  def home() = authenticated(Redirect(config.newDashboardUrl("home")))

  def yourDetails() = authenticated(Redirect(config.newDashboardUrl("your-details")))

  def manageProperties(clientDetails: Option[ClientDetails] = None) = authenticated {
    clientDetails match {
      case Some(client) =>
        Redirect(config.newDashboardUrl(
          s"selected-client-properties?clientOrganisationId=${client.organisationId}&clientName=${client.organisationName}"))
      case _ => Redirect(config.newDashboardUrl("your-properties"))
    }
  }

  def manageAgents() = authenticated(Redirect(config.newDashboardUrl("your-agents")))

  def viewManagedProperties(agentCode: Long, owner: Boolean): Action[AnyContent] = authenticated.async {
    implicit request =>
      for {
        group <- groupAccounts.withAgentCode(agentCode.toString)
        companyName = group.fold("No Name")(_.companyName) // impossible
        agentOrganisationId = group.map(_.id)
        authResult <- propertyLinks.getMyOrganisationsPropertyLinks(
                       GetPropertyLinksParameters(agent = group.map(_.companyName)),
                       PaginationParams(1, 1000, false))
      } yield
        Ok(
          views.html.dashboard.managedByAgentsProperties(
            ManagedPropertiesVM(agentOrganisationId, companyName, agentCode, authResult.authorisations),
            owner))
  }

  def viewMessages() = authenticated(Redirect(config.newDashboardUrl("inbox")))

  def viewMessage(messageId: String) = authenticated(Redirect(config.newDashboardUrl("inbox")))
}

case class ManagePropertiesVM(organisationId: Long, result: OwnerAuthResult, pagination: PaginationSearchSort)

case class ManagedPropertiesVM(
      agentOrganisationId: Option[Long],
      agentName: String,
      agentCode: Long,
      properties: Seq[OwnerAuthorisation])

case class ManageAgentsVM(agents: Seq[AgentInfo])

case class DraftCasesVM(draftCases: Seq[DraftCase])

case class PropertyLinkRepresentations(
      name: String,
      linkId: String,
      capacity: CapacityType,
      linkedDate: LocalDate,
      representations: Seq[PropertyRepresentation])

case class PendingPropertyLinkRepresentations(
      name: String,
      linkId: String,
      capacity: CapacityType,
      linkedDate: LocalDate,
      representations: Seq[PropertyRepresentation])

case class LinkedPropertiesRepresentations(
      added: Seq[PropertyLinkRepresentations],
      pending: Seq[PendingPropertyLinkRepresentations])

case class AgentInfo(organisationName: String, agentCode: Long)
