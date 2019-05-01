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

import javax.inject.Inject
import actions.AuthenticatedAction
import com.builtamont.play.pdf.PdfGenerator
import config.{ApplicationConfig, Global}
import connectors._
import connectors.propertyLinking.PropertyLinkConnector
import models._
import models.messages.MessagePagination
import models.searchApi.{OwnerAuthResult, OwnerAuthorisation}
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Request, Result}

import scala.concurrent.Future

class Dashboard @Inject()(draftCases: DraftCases,
                          propertyLinks: PropertyLinkConnector,
                          messagesConnector: MessagesConnector,
                          agentsConnector: AgentsConnector,
                          groupAccounts: GroupAccounts,
                          authenticated: AuthenticatedAction,
                          pdfGen: PdfGenerator)(implicit val messagesApi: MessagesApi, val config: ApplicationConfig) extends PropertyLinkingController with ValidPagination {

  def home() = authenticated { implicit request =>
    Redirect(config.newDashboardUrl("home"))
  }

  def yourDetails() = authenticated { implicit request =>
    Redirect(config.newDashboardUrl("your-details"))
  }

  def manageProperties() = authenticated { implicit request =>
   Redirect(config.newDashboardUrl("your-properties"))
  }

  def getProperties(page: Int,
                    pageSize: Int,
                    requestTotalRowCount: Boolean,
                    sortfield: Option[String],
                    sortorder: Option[String],
                    status: Option[String],
                    address: Option[String],
                    baref: Option[String],
                    agent: Option[String]) = authenticated { implicit request =>
    withValidPaginationSearchSort(page, pageSize, requestTotalRowCount, sortfield, sortorder, status, address, baref, agent) { pagination =>
      propertyLinks.linkedPropertiesSearchAndSort(request.organisationId, pagination) map { res =>
        Ok(Json.toJson(res))
      }
    }
  }

  def manageAgents() = authenticated { implicit request =>
    Redirect(config.newDashboardUrl("your-agents"))
  }

  def viewManagedProperties(agentCode: Long) = authenticated { implicit request =>
    for {
      group <- groupAccounts.withAgentCode(agentCode.toString)
      companyName = group.fold("No Name")(_.companyName) // impossible
      agentOrganisationId = group.map(_.id)
      authResult <- propertyLinks.linkedPropertiesSearchAndSort(
        request.organisationId,
        PaginationSearchSort(
          pageNumber = 1,
          pageSize = 1000,
          agent = group.map(_.companyName)))

      // keep only authorisations that have status Approved/Pending and are managed by this agent
      filteredAuths = authResult.authorisations.filter(auth =>
        Seq(PropertyLinkingApproved.name, PropertyLinkingPending.name).contains(auth.status)).filter(
        _.agents.fold(false)(_.map(_.organisationId).exists(id => agentOrganisationId.fold(false)(_ == id))))

    } yield Ok(views.html.dashboard.managedByAgentsProperties(
      ManagedPropertiesVM(agentOrganisationId, companyName, agentCode, filteredAuths)))
  }

  def viewMessages() = authenticated { implicit request =>
    Redirect(config.newDashboardUrl("inbox"))
  }

  def viewMessage(messageId: String) = authenticated { implicit request =>
    Redirect(config.newDashboardUrl("inbox"))
  }

  def viewMessageAsPdf(messageId: String) = authenticated { implicit request =>
    for {
      message <- messagesConnector.getMessage(request.organisationId, messageId)
    } yield {
      message match {
        case Some(m) => pdfGen.ok(views.html.dashboard.messages.viewMessagePdf(m), config.serviceUrl)
        case None => NotFound(Global.notFoundTemplate)
      }
    }
  }

  def messageCountJson() = authenticated { implicit request =>
    messagesConnector.countUnread(request.organisationId).map(messageCount => Ok(Json.toJson(messageCount)))
  }

  private def withValidMessagePagination(pagination: MessagePagination)
                                        (f: => Future[Result])
                                        (implicit request: Request[_]): Future[Result] = {
    if (pagination.pageNumber >= 1 && pagination.pageSize >= 1 && pagination.pageSize <= 100) {
      f
    } else {
      BadRequest(Global.badRequestTemplate)
    }
  }
}

case class ManagePropertiesVM(organisationId: Long,
                              result: OwnerAuthResult,
                              pagination: PaginationSearchSort)


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
