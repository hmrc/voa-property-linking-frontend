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

package services

import binders.propertylinks.GetPropertyLinksParameters
import connectors.PropertyRepresentationConnector
import connectors.propertyLinking.PropertyLinkConnector
import controllers.PaginationParams

import javax.inject.{Inject, Named}
import models.propertyrepresentation._
import models.searchApi.{AgentPropertiesParameters, OwnerAuthResult}
import play.api.Logging
import repositories.SessionRepo
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

case class AppointRevokeException(message: String)
    extends Exception(s"Failed to appoint agent to multiple properties: $message")

class AgentRelationshipService @Inject()(
      representations: PropertyRepresentationConnector,
      propertyLinks: PropertyLinkConnector,
      @Named("appointLinkSession") val propertyLinksSessionRepo: SessionRepo)(
      implicit val executionContext: ExecutionContext)
    extends Logging {

  def createAndSubmitAgentRepRequest(pLinkIds: List[String], agentCode: Long)(
        implicit hc: HeaderCarrier): Future[Unit] =
    assignAgentToSomeProperties(AppointAgentToSomePropertiesRequest(agentCode = agentCode, propertyLinkIds = pLinkIds))
      .map(_ => ())

  def createAndSubmitAgentRevokeRequest(pLinkIds: List[String], agentCode: Long)(
        implicit hc: HeaderCarrier): Future[Unit] =
    unassignAgentFromSomeProperties(
      AppointAgentToSomePropertiesRequest(
        agentCode = agentCode,
        propertyLinkIds = pLinkIds
      )
    ).map(_ => ())

  def getMyOrganisationPropertyLinksWithAgentFiltering(
        params: GetPropertyLinksParameters,
        pagination: AgentPropertiesParameters,
        organisationId: Long,
        agentOrganisationId: Long)(implicit hc: HeaderCarrier): Future[OwnerAuthResult] =
    propertyLinks.getMyOrganisationPropertyLinksWithAgentFiltering(
      params,
      PaginationParams(
        startPoint = pagination.startPoint,
        pageSize = pagination.pageSize,
        requestTotalRowCount = false),
      organisationId = organisationId,
      agentOrganisationId = agentOrganisationId,
      agentAppointed = Some(pagination.agentAppointed),
      agentCode = pagination.agentCode
    )

  def getMyAgentPropertyLinks(agentCode: Long, searchParams: GetPropertyLinksParameters, pagination: PaginationParams)(
        implicit hc: HeaderCarrier): Future[OwnerAuthResult] =
    propertyLinks.getMyAgentPropertyLinks(agentCode, searchParams, pagination)

  def getMyOrganisationsPropertyLinks(searchParams: GetPropertyLinksParameters, pagination: PaginationParams)(
        implicit hc: HeaderCarrier): Future[OwnerAuthResult] = {
    val ownerAuthResult = propertyLinks.getMyOrganisationsPropertyLinks(searchParams, pagination)

    ownerAuthResult.map(
      oar =>
        oar.copy(authorisations = oar.authorisations
          .filter(auth => auth.agents.nonEmpty)))
  }

  def getAgentNameAndAddress(agentCode: Long)(implicit hc: HeaderCarrier): Future[Option[AgentDetails]] =
    representations.getAgentDetails(agentCode)

  def removeAgentFromOrganisation(appointAgentRequest: AgentAppointmentChangesRequest)(
        implicit hc: HeaderCarrier): Future[AgentAppointmentChangesResponse] =
    propertyLinks.removeAgentFromOrganisation(appointAgentRequest)

  def assignAgent(appointAgentRequest: AgentAppointmentChangesRequest)(
        implicit hc: HeaderCarrier): Future[AgentAppointmentChangesResponse] =
    propertyLinks.assignAgent(appointAgentRequest)

  def assignAgentToSomeProperties(request: AppointAgentToSomePropertiesRequest)(
        implicit hc: HeaderCarrier): Future[AgentAppointmentChangesResponse] =
    propertyLinks.assignAgentToSomeProperties(request)

  def unassignAgent(request: AgentAppointmentChangesRequest)(
        implicit hc: HeaderCarrier): Future[AgentAppointmentChangesResponse] =
    propertyLinks.unassignAgent(request)

  def unassignAgentFromSomeProperties(request: AppointAgentToSomePropertiesRequest)(
        implicit hc: HeaderCarrier): Future[AgentAppointmentChangesResponse] =
    propertyLinks.unassignAgentFromSomeProperties(request)

  def getMyOrganisationAgents()(implicit hc: HeaderCarrier): Future[AgentList] =
    propertyLinks.getMyOrganisationAgents()

  def getMyOrganisationPropertyLinksCount()(implicit hc: HeaderCarrier): Future[Int] =
    propertyLinks.getMyOrganisationPropertyLinksCount()

}
