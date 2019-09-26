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

package services

import java.time.Instant

import actions.BasicAuthenticatedRequest
import auditing.AuditingService
import binders.propertylinks.GetPropertyLinksParameters
import config.ApplicationConfig
import connectors.PropertyRepresentationConnector
import connectors.propertyLinking.PropertyLinkConnector
import controllers.{Pagination, PaginationParams}
import javax.inject.{Inject, Named}
import models._
import models.searchApi.{AgentPropertiesParameters, OwnerAuthResult}
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.Request
import repositories.SessionRepo
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

case class AppointRevokeException(message: String) extends Exception(s"Failed to appoint agent to multiple properties: $message")

class AppointRevokeAgentService @Inject()(representations: PropertyRepresentationConnector,
                                          propertyLinks: PropertyLinkConnector,
                                          @Named("appointLinkSession") val propertyLinksSessionRepo: SessionRepo,
                                          config: ApplicationConfig)
                                          (implicit val executionContext: ExecutionContext)
{

  val logger: Logger = Logger(this.getClass)

  def createAndSubmitAgentRepRequest( pLinkIds: List[String],
                                      agentOrgId: Long,
                                      organisationId: Long,
                                      individualId: Long,
                                      checkPermission: AgentPermission,
                                      challengePermission: AgentPermission,
                                      isAgent: Boolean)(implicit hc: HeaderCarrier): Future[Unit] = {


    Future.traverse(pLinkIds)(pLink =>
      appointAgent(
        pLink,
        agentOrgId,
        organisationId,
        individualId,
        checkPermission,
        challengePermission,
        isAgent)).map { x =>
      // Introduce a delay here to give modernised a chance to process its queue
      // This is a temporary measure to avoid duplicating agent requests when appointing multiple agents
      // TODO https://jira.tools.tax.service.gov.uk/browse/VTCCA-2972
      Thread.sleep(config.agentAppointDelay * 1000)
      x.reduce((a,b) => a)
    }
  }

  def getMyOrganisationPropertyLinksWithAgentFiltering(params: GetPropertyLinksParameters, pagination: AgentPropertiesParameters, organisationId: Long, agentOrganisationId: Long)
                                                      (implicit hc: HeaderCarrier): Future[OwnerAuthResult] = {

    propertyLinks.getMyOrganisationPropertyLinksWithAgentFiltering(
      params,
      PaginationParams(startPoint = pagination.startPoint, pageSize = pagination.pageSize, requestTotalRowCount = false),
      organisationId = organisationId,
      agentOrganisationId = agentOrganisationId,
      checkPermission = pagination.checkPermission.name,
      challengePermission = pagination.challengePermission.name)

  }

  private def appointAgent(
                                              pLink: String,
                                              agentOrgId: Long,
                                              organisationId: Long,
                                              individualId: Long,
                                              checkPermission: AgentPermission,
                                              challengePermission: AgentPermission,
                                              isAgent: Boolean
                                            )(implicit hc: HeaderCarrier):Future[Unit] = {
    hc.sessionId match {
      case Some(sessionId) =>
        propertyLinksSessionRepo.get[SessionPropertyLinks] flatMap {
          case Some(links) =>
            links.links.find(_.submissionId == pLink) match {
              case Some(link) =>
                Future.successful(updateAllAgentsPermission(
                  link,
                  AppointAgent(None, "", checkPermission, challengePermission),
                  agentOrgId,
                  individualId,
                  organisationId))
              case None =>
                logger.warn(s"Property link $pLink not found in property links cache.")
                Future.failed(new AppointRevokeException(s"Property link $pLink not found in property links cache."))
            }
          case None =>
            logger.warn(s"Session ID $sessionId no longer in property links cache - should be redirected to login by auth.")
            Future.failed(new AppointRevokeException(s"Session ID $sessionId no longer in property links cache - should be redirected to login by auth."))
        }
      case None =>
        logger.warn(s"Unable to obtain session ID from request to retrieve property links cache - should be redirected to login by auth.")
        Future.failed(new AppointRevokeException(s"Unable to obtain session ID from request to retrieve property links cache - should be redirected to login by auth."))
    }
  }

  private def updateAllAgentsPermission(
                                         link: SessionPropertyLink,
                                         newAgentPermission: AppointAgent,
                                         newAgentOrgId: Long,
                                         individualId: Long,
                                         organisationId: Long
                                       )(implicit hc: HeaderCarrier): Future[Unit] = {
    val updateExistingAgents = if (newAgentPermission.canCheck == StartAndContinue && newAgentPermission.canChallenge == StartAndContinue) {
      Future.sequence(link.agents.map(agent => representations.revoke(agent.authorisedPartyId)))
    } else if (newAgentPermission.canCheck == StartAndContinue) {
      val agentsToUpdate = link.agents.filter(_.checkPermission == StartAndContinue)
      for {
        revokedAgents <- Future.traverse(agentsToUpdate)(agent => representations.revoke(agent.authorisedPartyId))
        //existing agents that had a check permission have been revoked
        //we now need to re-add the agents that had a challenge permission
        updatedAgents <- Future.traverse(agentsToUpdate.filter(_.challengePermission != NotPermitted))(agent => {
          createAndSubmitAgentRepRequest(link.authorisationId, agent.organisationId, individualId, NotPermitted, agent.challengePermission, organisationId)
        })
      } yield {
        updatedAgents
      }
    } else {
      val agentsToUpdate = link.agents.filter(_.challengePermission == StartAndContinue)
      for {
        revokedAgents <- Future.traverse(agentsToUpdate)(agent => representations.revoke(agent.authorisedPartyId))
        updatedAgents <- Future.traverse(agentsToUpdate.filter(_.checkPermission != NotPermitted))(agent => {
          createAndSubmitAgentRepRequest(link.authorisationId, agent.organisationId, individualId, agent.checkPermission, NotPermitted, organisationId)
        })
      } yield {
        updatedAgents
      }
    }
    updateExistingAgents.flatMap(_ => {
      //existing agents have been updated. Time to add the new agent.
      createAndSubmitAgentRepRequest(link.authorisationId, newAgentOrgId, individualId, newAgentPermission, organisationId)
    })
  }

  private def createAndSubmitAgentRepRequest(authorisationId: Long, agentOrgId: Long, userIndividualId: Long,
                                             checkPermission: AgentPermission, challengePermission: AgentPermission, organisationId: Long)
                                            (implicit hc: HeaderCarrier): Future[Unit] = {
    val submissionId = java.util.UUID.randomUUID().toString
    val createDatetime = Instant.now
    val req = RepresentationRequest(authorisationId, agentOrgId, userIndividualId,
      submissionId, checkPermission.name, challengePermission.name, createDatetime)

    representations.create(req).map(x => {
      AuditingService.sendEvent("agent representation request approve", Json.obj(
        "organisationId" -> organisationId,
        "individualId" -> userIndividualId,
        "propertyLinkId" -> authorisationId,
        "agentOrganisationId" -> agentOrgId,
        "submissionId" -> submissionId,
        "checkPermission" -> checkPermission.name,
        "challengePermission" -> challengePermission.name,
        "createDatetime" -> createDatetime.toString
      ))
    })
  }

  private def createAndSubmitAgentRepRequest(authorisationId: Long, agentOrgId: Long, userIndividualId: Long, appointedAgent: AppointAgent, organisationId: Long)
                                            (implicit hc: HeaderCarrier): Future[Unit] = {
    createAndSubmitAgentRepRequest(authorisationId, agentOrgId, userIndividualId, appointedAgent.canCheck, appointedAgent.canChallenge, organisationId)
  }
}