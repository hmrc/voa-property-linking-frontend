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

package controllers

import actions.AuthenticatedAction
import config.ApplicationConfig
import connectors._
import models._
import models.searchApi.{OwnerAuthResult, OwnerAuthorisation}
import play.api.i18n.MessagesApi
import play.api.mvc.MessagesControllerComponents
import services.AgentRelationshipService
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class Dashboard @Inject()(
      val errorHandler: CustomErrorHandler,
      propertyLinks: AgentRelationshipService,
      groupAccounts: GroupAccounts,
      authenticated: AuthenticatedAction,
      override val controllerComponents: MessagesControllerComponents)(
      implicit executionContext: ExecutionContext,
      override val messagesApi: MessagesApi,
      val config: ApplicationConfig
) extends PropertyLinkingController {

  def home() = authenticated(Redirect(config.dashboardUrl("home")))

  def yourDetails() = authenticated(Redirect(config.dashboardUrl("your-details")))

  def manageProperties(clientDetails: Option[ClientDetails] = None) = authenticated {
    clientDetails match {
      case Some(client) =>
        Redirect(config.dashboardUrl(
          s"selected-client-properties?clientOrganisationId=${client.organisationId}&clientName=${client.organisationName}"))
      case _ => Redirect(config.dashboardUrl("your-properties"))
    }
  }

  def manageAgents() = authenticated(Redirect(config.dashboardUrl("your-agents")))

  def viewMessages() = authenticated(Redirect(config.dashboardUrl("inbox")))

  def viewMessage(messageId: String) = authenticated(Redirect(config.dashboardUrl("inbox")))
}

case class ManagePropertiesVM(organisationId: Long, result: OwnerAuthResult, pagination: PaginationSearchSort)

case class ManagedPropertiesVM(
      agentOrganisationId: Option[Long],
      agentName: String,
      agentCode: Long,
      properties: Seq[OwnerAuthorisation])

case class ManageAgentsVM(agents: Seq[AgentInfo])

case class DraftCasesVM(draftCases: Seq[DraftCase])

case class AgentInfo(organisationName: String, agentCode: Long)
