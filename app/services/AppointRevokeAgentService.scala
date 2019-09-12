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

class AppointRevokeAgentService @Inject()(representations: PropertyRepresentationConnector,
                                          propertyLinks: PropertyLinkConnector,
                                          @Named("propertyLinksSession") val propertyLinksSessionRepo: SessionRepo) {

  val logger: Logger = Logger(this.getClass)



  def createAndSubmitAgentRepRequest( pLinkIds: List[String],
                                      agentOrgId: Long,
                                      organisationId: Long,
                                      individualId: Long,
                                      checkPermission: AgentPermission,
                                      challengePermission: AgentPermission,
                                      isAgent: Boolean)(implicit hc: HeaderCarrier, executionContext: ExecutionContext): Future[Option[Unit]] = {


    Future.traverse(pLinkIds)(pLink =>
      appointAgent(
        pLink,
        agentOrgId,
        organisationId,
        individualId,
        checkPermission,
        challengePermission,
        isAgent)).map {
      x => if(x.forall(_.isDefined))
            Some()
           else
            None
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
                                            )(implicit hc: HeaderCarrier, executionContext: ExecutionContext):Future[Option[Unit]] = {

    hc.sessionId match {
      case Some(sessionId) =>
        propertyLinksSessionRepo.get[SessionPropertyLinks] map {
          case Some(links) =>
            links.links.find(_.submissionId == pLink) match {
              case Some(link) =>
                Some(updateAllAgentsPermission(
                  link,
                  AppointAgent(None, "", checkPermission, challengePermission),
                  agentOrgId,
                  individualId,
                  organisationId))
              case None =>
                logger.error(s"Property link $pLink not found in property links cache.")
                None
            }
          case None =>
            logger.warn(s"Session ID $hc.sessionId no longer in property links cache - should be redirected to login by auth.")
            Some()
        }
      case None =>
        logger.warn(s"Unable to obtain session ID from request to retrieve property links cache - should be redirected to login by auth.")
        Future.successful(Some())
    }
  }

  private def updateAllAgentsPermission(
                                         link: SessionPropertyLink,
                                         newAgentPermission: AppointAgent,
                                         newAgentOrgId: Long,
                                         individualId: Long,
                                         organisationId: Long
                                       )(implicit hc: HeaderCarrier, executionContext: ExecutionContext): Future[Unit] = {
    val updateExistingAgents = if (newAgentPermission.canCheck == StartAndContinue && newAgentPermission.canChallenge == StartAndContinue) {
      Future.sequence(link.agents.map(agent => representations.revoke(agent.authorisedPartyId)))
    } else if (newAgentPermission.canCheck == StartAndContinue) {
      val agentsToUpdate = link.agents.filter(_.permissions.checkPermission == StartAndContinue)
      for {
        revokedAgents <- Future.traverse(agentsToUpdate)(agent => representations.revoke(agent.authorisedPartyId))
        //existing agents that had a check permission have been revoked
        //we now need to re-add the agents that had a challenge permission
        updatedAgents <- Future.traverse(agentsToUpdate.filter(_.permissions.challengePermission != NotPermitted))(agent => {
          createAndSubmitAgentRepRequest(link.authorisationId, agent.organisationId, individualId, NotPermitted, agent.permissions.challengePermission, organisationId)
        })
      } yield {
        updatedAgents
      }
    } else {
      val agentsToUpdate = link.agents.filter(_.permissions.challengePermission == StartAndContinue)
      for {
        revokedAgents <- Future.traverse(agentsToUpdate)(agent => representations.revoke(agent.authorisedPartyId))
        updatedAgents <- Future.traverse(agentsToUpdate.filter(_.permissions.checkPermission != NotPermitted))(agent => {
          createAndSubmitAgentRepRequest(link.authorisationId, agent.organisationId, individualId, agent.permissions.checkPermission, NotPermitted, organisationId)
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
                                            (implicit hc: HeaderCarrier, executionContext: ExecutionContext): Future[Unit] = {
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
                                            (implicit hc: HeaderCarrier, executionContext: ExecutionContext): Future[Unit] = {
    createAndSubmitAgentRepRequest(authorisationId, agentOrgId, userIndividualId, appointedAgent.canCheck, appointedAgent.canChallenge, organisationId)
  }
}


