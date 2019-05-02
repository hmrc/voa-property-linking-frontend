/*
 * Copyright 2019 HM Revenue & Customs
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
import binders.pagination.PaginationParameters
import binders.searchandsort.SearchAndSort
import com.builtamont.play.pdf.PdfGenerator
import config.ApplicationConfig
import connectors._
import connectors.propertyLinking.PropertyLinkConnector
import javax.inject.Inject
import models._
import models.messages.MessagePagination
import models.searchApi.{OwnerAuthResult, OwnerAuthorisation}
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc.{Request, Result}

import scala.concurrent.Future

class Dashboard @Inject()(draftCases: DraftCases,
                          propertyLinks: PropertyLinkConnector,
                          messagesConnector: MessagesConnector,
                          agentsConnector: AgentsConnector,
                          groupAccounts: GroupAccounts,
                          authenticated: AuthenticatedAction,
                          pdfGen: PdfGenerator)(implicit val messagesApi: MessagesApi, val config: ApplicationConfig) extends PropertyLinkingController {

  def home() = authenticated { implicit request =>
    Future.successful(Redirect(config.newDashboardUrl("home")))
  }

  def yourDetails() = authenticated { implicit request =>
    Future.successful(Redirect(config.newDashboardUrl("your-details")))
  }

  def manageProperties() = authenticated { implicit request =>
   Future.successful(Redirect(config.newDashboardUrl("your-properties")))
  }

  def getProperties(
                   pagination: PaginationParameters,
                   searchAndSort: SearchAndSort
                   ) = authenticated { implicit request =>
      propertyLinks.linkedPropertiesSearchAndSort(request.organisationId, pagination, searchAndSort) map { res =>
        Ok(Json.toJson(res))
      }
  }

  def manageAgents() = authenticated { implicit request =>
    Future.successful(Redirect(config.newDashboardUrl("your-agents")))
  }

  def viewManagedProperties(agentCode: Long) = authenticated { implicit request =>
    for {
      group <- groupAccounts.withAgentCode(agentCode.toString)
      companyName = group.fold("No Name")(_.companyName) // impossible
      agentOrganisationId = group.map(_.id)
      authResult <- propertyLinks.linkedPropertiesSearchAndSort(
        request.organisationId,
        PaginationParameters(1, 1000),
        SearchAndSort(agent = group.map(_.companyName))
       )

      // keep only authorisations that have status Approved/Pending and are managed by this agent
      filteredAuths = authResult.authorisations.filter(auth =>
        Seq(PropertyLinkingApproved.name, PropertyLinkingPending.name).contains(auth.status)).filter(
        _.agents.fold(false)(_.map(_.organisationId).exists(id => agentOrganisationId.fold(false)(_ == id))))

    } yield Ok(views.html.dashboard.managedByAgentsProperties(
      ManagedPropertiesVM(agentOrganisationId, companyName, agentCode, filteredAuths)))
  }

  def viewMessages() = authenticated { implicit request =>
    Future.successful(Redirect(config.newDashboardUrl("inbox")))
  }

  def viewMessage(messageId: String) = authenticated { implicit request =>
    Future.successful(Redirect(config.newDashboardUrl("inbox")))
  }

  def viewMessageAsPdf(messageId: String) = authenticated { implicit request =>
    messagesConnector
      .getMessage(request.organisationId, messageId)
      .map(m => pdfGen.ok(views.html.dashboard.messages.viewMessagePdf(m), config.serviceUrl))
  }

  def messageCountJson() = authenticated { implicit request =>
    messagesConnector.countUnread(request.organisationId).map(messageCount => Ok(Json.toJson(messageCount)))
  }
}

case class ManagePropertiesVM(organisationId: Long,
                              result: OwnerAuthResult,
                              pagination: PaginationParameters,
                              searchAndSort: SearchAndSort)


case class ManagedPropertiesVM(agentOrganisationId: Option[Long],
                               agentName: String,
                               agentCode: Long,
                               properties: Seq[OwnerAuthorisation])

case class ManageAgentsVM(agents: Seq[AgentInfo])

case class DraftCasesVM(draftCases: Seq[DraftCase])

case class PropertyLinkRepresentations(name: String, linkId: String, capacity: CapacityType, linkedDate: LocalDate,
                                       representations: Seq[PropertyRepresentation])

case class PendingPropertyLinkRepresentations(name: String, linkId: String, capacity: CapacityType,
                                              linkedDate: LocalDate, representations: Seq[PropertyRepresentation])

case class LinkedPropertiesRepresentations(added: Seq[PropertyLinkRepresentations], pending: Seq[PendingPropertyLinkRepresentations])

case class AgentInfo(organisationName: String, agentCode: Long)

case class ClientPropertiesVM(properties: Seq[ClientProperty])
