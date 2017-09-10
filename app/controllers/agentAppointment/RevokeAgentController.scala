/*
 * Copyright 2017 HM Revenue & Customs
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

import actions.AuthenticatedAction
import com.google.inject.Inject
import connectors.PropertyRepresentationConnector
import connectors.propertyLinking.PropertyLinkConnector
import controllers.PropertyLinkingController
import models.Party

class RevokeAgentController @Inject()(authenticated: AuthenticatedAction,
                                      propertyLinks: PropertyLinkConnector,
                                      representations: PropertyRepresentationConnector) extends PropertyLinkingController {

  def revokeAgent(authorisationId: Long, authorisedPartyId: Long, agentCode: Long) = authenticated { implicit request =>
    propertyLinks.get(request.organisationId, authorisationId) map {
      case Some(link) =>
        link.agents.find(a => agentIsAuthorised(a, authorisedPartyId, agentCode)) match {
          case Some(agent) =>
            Ok(views.html.propertyRepresentation.revokeAgent(agentCode, authorisationId, authorisedPartyId, agent.organisationName))
          case None => notFound
        }
      case None => notFound
    }
  }

  def revokeAgentConfirmed(authorisationId: Long, authorisedPartyId: Long, agentCode: Long) = authenticated { implicit request =>
    propertyLinks.get(request.organisationId, authorisationId) flatMap {
      case Some(link) =>
        val nextLink = if (link.agents.size > 1) {
          controllers.routes.Dashboard.viewManagedProperties(agentCode).url
        } else {
          controllers.routes.Dashboard.manageAgents().url
        }

        link.agents.find(a => agentIsAuthorised(a, authorisedPartyId, agentCode)) match {
          case Some(agent) => representations.revoke(authorisedPartyId).map { _ =>
            Ok(views.html.propertyRepresentation.revokedAgent(nextLink, agent.organisationName, link.address))
          }
          case None => notFound
        }
      case None => notFound
    }
  }

  private def agentIsAuthorised(agent: Party, authorisedPartyId: Long, agentCode: Long) = {
    agent.authorisedPartyId == authorisedPartyId && agent.agentCode == agentCode
  }
}
