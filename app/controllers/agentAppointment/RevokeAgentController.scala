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

import actions.AuthenticatedAction
import auditing.AuditingService
import config.ApplicationConfig
import connectors.PropertyRepresentationConnector
import connectors.propertyLinking.PropertyLinkConnector
import controllers.PropertyLinkingController
import javax.inject.Inject
import models.Party
import play.api.Logger
import play.api.data.{Form, Forms}
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import uk.gov.voa.propertylinking.errorhandler.CustomErrorHandler

import scala.concurrent.{ExecutionContext, Future}

class RevokeAgentController @Inject()(
                                       val errorHandler: CustomErrorHandler,
                                       auditingService: AuditingService,
                                       authenticated: AuthenticatedAction,
                                      propertyLinks: PropertyLinkConnector,
                                      representations: PropertyRepresentationConnector
                                     )(implicit executionContenxt: ExecutionContext, val messagesApi: MessagesApi, val config: ApplicationConfig) extends PropertyLinkingController {

  val logger = Logger(this.getClass.getName)

  def revokeAgent(submissionId: String, authorisedPartyId: Long, agentCode: Long) = authenticated.async { implicit request =>
    propertyLinks.getMyOrganisationPropertyLink(submissionId).map {
      case Some(link) =>
        link.agents.find(a => agentIsAuthorised(a, authorisedPartyId, agentCode)) match {
          case Some(agent) =>
            Ok(views.html.propertyrepresentation.revoke.revokeAgent(agentCode, submissionId, authorisedPartyId, agent.organisationName, !request.organisationAccount.isAgent))
          case None => notFound
        }
      case None => notFound
    }
  }

  def revokeAgentSubmit(
                   submissionId: String,
                   agentCode: Long): Action[AnyContent] = authenticated.async { implicit request =>
    Form(Forms.single("authorisedPartyId" -> longNumber)).bindFromRequest().fold(
      errors =>
        propertyLinks.getMyOrganisationPropertyLink(submissionId).map {
          case Some(link) =>
            link.agents.find(a => a.agentCode == agentCode) match {
              case Some(agent)  =>
                logger.warn(s"authorised party id not passed with this request. $errors")
                BadRequest(views.html.propertyrepresentation.revoke.revokeAgent(agentCode, submissionId, agent.authorisedPartyId, agent.organisationName, !request.organisationAccount.isAgent))
              case None         => notFound
            }
          case None       => notFound
        },
      authorisedPartyId =>
    propertyLinks
      .getMyOrganisationPropertyLink(submissionId)
      .flatMap {
        case Some(link) =>
          link.agents.find(a => agentIsAuthorised(a, authorisedPartyId, agentCode)) match {
            case Some(_) =>
              representations.revoke(authorisedPartyId).map { _ => {
                auditingService.sendEvent("agent representation revoke", Json.obj(
                  "organisationId" -> request.organisationId,
                  "individualId" -> request.individualAccount.individualId,
                  "propertyLinkId" -> submissionId,
                  "agentOrganisationId" -> request.organisationAccount.id).toString
                )
                Redirect(routes.RevokeAgentController.revokeAgentConfirmed(submissionId, authorisedPartyId, agentCode))
            }
          }
          case None      => Future.successful(notFound)
        }
      case None         => Future.successful(notFound)
    }
    )
  }

  def revokeAgentConfirmed(submissionId: String, authorisedPartyId: Long, agentCode: Long): Action[AnyContent] = authenticated.async { implicit request =>
    propertyLinks.getMyOrganisationPropertyLink(submissionId).map {
      case Some(link) =>
        val nextLink = if (link.agents.size > 1) {
          controllers.routes.Dashboard.viewManagedProperties(agentCode, !request.organisationAccount.isAgent).url
        } else {
          controllers.routes.Dashboard.manageAgents().url
        }

        link.agents.find(a => agentIsAuthorised(a, authorisedPartyId, agentCode)) match {
          case Some(agent) =>
            auditingService.sendEvent("agent representation revoke", Json.obj(
              "organisationId" -> request.organisationId,
              "individualId" -> request.individualAccount.individualId,
              "propertyLinkId" -> submissionId,
              "agentOrganisationId" -> request.organisationAccount.id).toString
            )
            Ok(views.html.propertyrepresentation.revoke.revokedAgent(nextLink, agent.organisationName, link.address))
          case None => notFound
        }
      case None => notFound
    }
  }

  private def agentIsAuthorised(agent: Party, authorisedPartyId: Long, agentCode: Long) = {
    agent.authorisedPartyId == authorisedPartyId && agent.agentCode == agentCode
  }
}
