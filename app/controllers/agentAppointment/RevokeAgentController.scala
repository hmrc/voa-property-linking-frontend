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

package controllers.agentAppointment

import actions.{AuthenticatedAction, BasicAuthenticatedRequest}
import javax.inject.Inject
import auditing.AuditingService
import config.ApplicationConfig
import connectors.{AgentsConnector, PropertyRepresentationConnector}
import connectors.propertyLinking.PropertyLinkConnector
import controllers.PropertyLinkingController
import exceptionhandler.ErrorHandler
import models.{AppointAgent, Party}
import models.searchApi.{AgentId, AgentPropertiesParameters}
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.data.{Form, FormError}
import play.api.data.Forms.{number, _}
import play.api.data.{Form, FormError}
import play.api.mvc.{Action, AnyContent}

import scala.concurrent.Future

class RevokeAgentController @Inject()(
                                       authenticated: AuthenticatedAction,
                                       propertyLinks: PropertyLinkConnector,
                                       representations: PropertyRepresentationConnector,
                                       auditingService: AuditingService,
                                       errorHandler: ErrorHandler
                                     )(implicit val messagesApi: MessagesApi, val config: ApplicationConfig) extends PropertyLinkingController {

  def revokeAgent(authorisationId: Long, authorisedPartyId: Long, agentCode: Long): Action[AnyContent] = authenticated { implicit request =>
    propertyLinks.get(request.organisationId, authorisationId) map { link =>
        link.flatMap (_.agents.find(a => agentIsAuthorised(a, authorisedPartyId, agentCode))) match {
          case Some(agent)  =>
            Ok(views.html.propertyRepresentation.revokeAgent(agentCode, authorisationId, authorisedPartyId, agent.organisationName))
          case None         => errorHandler.notFound
        }
    }
  }

  def revokeAgentConfirmed(
                            authorisationId: Long,
                            authorisedPartyId: Long,
                            agentCode: Long
                          ): Action[AnyContent] = authenticated { implicit request =>
    propertyLinks.get(request.organisationId, authorisationId) flatMap {
      case Some(link) =>
        link.agents.find(a => agentIsAuthorised(a, authorisedPartyId, agentCode)) match {
          case Some(agent)  => representations.revoke(authorisedPartyId).map { _ => {
            auditingService.sendEvent("agent representation revoke", Json.obj(
              "organisationId" -> request.organisationId,
              "individualId" -> request.individualAccount.individualId,
              "propertyLinkId" -> authorisationId,
              "agentOrganisationId" -> request.organisationAccount.id).toString
            )
            val nextLink = if (link.agents.size > 1) {
              controllers.routes.Dashboard.viewManagedProperties(agentCode).url
            } else {
              controllers.routes.Dashboard.manageAgents().url
            }
            Ok(views.html.propertyRepresentation.revokedAgent(nextLink, agent.organisationName, link.address))
          }
          }
          case None         => Future.successful(errorHandler.notFound)
        }
      case None       => Future.successful(errorHandler.notFound)
    }
  }

  private def agentIsAuthorised(agent: Party, authorisedPartyId: Long, agentCode: Long) = {
    agent.authorisedPartyId == authorisedPartyId && agent.agentCode == agentCode
  }
}
