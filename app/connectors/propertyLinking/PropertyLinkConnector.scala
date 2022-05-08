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

package connectors.propertyLinking

import binders.propertylinks.GetPropertyLinksParameters
import connectors.errorhandler.exceptions.ExceptionThrowingReadsInstances
import controllers.PaginationParams
import models._
import models.dvr.cases.check.myclients.CheckCasesWithClient
import models.dvr.cases.check.myorganisation.CheckCasesWithAgent
import models.dvr.cases.check.projection.CaseDetails
import models.properties.PropertyHistory
import models.propertylinking.payload.PropertyLinkPayload
import models.propertyrepresentation.{AgentAppointmentChangesRequest, AgentAppointmentChangesResponse, AgentList, AppointAgentToSomePropertiesRequest}
import models.searchApi.OwnerAuthResult
import play.api.Logging
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PropertyLinkConnector @Inject()(config: ServicesConfig, http: HttpClient)(implicit ec: ExecutionContext)
    extends Logging {
  lazy val baseUrl: String = config.baseUrl("property-linking") + s"/property-linking"
  lazy val vmBaseUrl: String = config.baseUrl("vmv") + s"/vmv"

  def createPropertyLink(propertyLinkPayload: PropertyLinkPayload)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    import ExceptionThrowingReadsInstances._
    http.POST[PropertyLinkPayload, HttpResponse](s"$baseUrl/property-links", propertyLinkPayload)
  }

  def createPropertyLinkOnClientBehalf(propertyLinkPayload: PropertyLinkPayload, clientId: Long)(
        implicit hc: HeaderCarrier): Future[HttpResponse] = {
    import ExceptionThrowingReadsInstances._
    http.POST[PropertyLinkPayload, HttpResponse](s"$baseUrl/clients/$clientId/property-links", propertyLinkPayload)
  }
  def getMyOrganisationPropertyLink(submissionId: String)(implicit hc: HeaderCarrier): Future[Option[PropertyLink]] =
    http.GET[Option[PropertyLink]](s"$baseUrl/owner/property-links/$submissionId")

  def getMyAgentPropertyLinks(agentCode: Long, searchParams: GetPropertyLinksParameters, pagination: PaginationParams)(
        implicit hc: HeaderCarrier): Future[OwnerAuthResult] =
    http.GET[OwnerAuthResult](
      s"$baseUrl/my-organisation/agents/$agentCode/property-links",
      List(
        searchParams.address.map("address" -> _),
        searchParams.baref.map("baref"     -> _),
        searchParams.agent.map("agent"     -> _),
        searchParams.status.map("status"   -> _),
        Some("sortField"                   -> searchParams.sortfield.toString),
        Some("sortOrder"                   -> searchParams.sortorder.toString)
      ).flatten ++
        List(
          "startPoint"           -> pagination.startPoint.toString,
          "pageSize"             -> pagination.pageSize.toString,
          "requestTotalRowCount" -> pagination.requestTotalRowCount.toString
        )
    )

  def getMyOrganisationsPropertyLinks(
        searchParams: GetPropertyLinksParameters,
        pagination: PaginationParams
  )(implicit hc: HeaderCarrier): Future[OwnerAuthResult] =
    http.GET[OwnerAuthResult](
      s"$baseUrl/owner/property-links",
      List(
        searchParams.address.map("address" -> _),
        searchParams.baref.map("baref"     -> _),
        searchParams.agent.map("agent"     -> _),
        searchParams.status.map("status"   -> _),
        Some("sortField"                   -> searchParams.sortfield.toString),
        Some("sortOrder"                   -> searchParams.sortorder.toString)
      ).flatten ++
        List(
          "startPoint"           -> pagination.startPoint.toString,
          "pageSize"             -> pagination.pageSize.toString,
          "requestTotalRowCount" -> pagination.requestTotalRowCount.toString
        )
    )

  def getMyClientsPropertyLink(submissionId: String)(implicit hc: HeaderCarrier): Future[Option[PropertyLink]] =
    http.GET[Option[PropertyLink]](s"$baseUrl/agent/property-links/$submissionId")

  def getMyOrganisationPropertyLinksWithAgentFiltering(
        searchParams: GetPropertyLinksParameters,
        pagination: PaginationParams,
        organisationId: Long,
        agentOrganisationId: Long,
        agentAppointed: Option[String] = None,
        agentCode: Long
  )(implicit hc: HeaderCarrier): Future[OwnerAuthResult] = {

    val ownerAuthResult = if (agentAppointed.contains("NO")) {
      getMyOrganisationsPropertyLinks(searchParams, pagination)
    } else {
      http.GET[OwnerAuthResult](
        s"$baseUrl/my-organisation/agents/$agentCode/available-property-links",
        List(
          searchParams.address.map("address" -> _),
          searchParams.agent.map("agent"     -> _)
        ).flatten ++ List(
          "sortField"            -> searchParams.sortfield.toString,
          "sortOrder"            -> searchParams.sortorder.toString,
          "startPoint"           -> pagination.startPoint.toString,
          "pageSize"             -> pagination.pageSize.toString,
          "requestTotalRowCount" -> pagination.requestTotalRowCount.toString
        )
      )
    }
    // filter agents on representationStatus
    ownerAuthResult.map(oar =>
      oar.copy(authorisations = oar.authorisations.map(auth => auth.copy(agents = auth.agents))))
  }

  def clientPropertyLink(plSubmissionId: String)(implicit hc: HeaderCarrier): Future[Option[ClientPropertyLink]] = {
    val url =
      s"$baseUrl/agent/property-links/$plSubmissionId?projection=clientsPropertyLink"

    http.GET[Option[ClientPropertyLink]](url) recover { case _: NotFoundException => None }
  }

  def getOwnerAssessments(submissionId: String)(implicit hc: HeaderCarrier): Future[Option[ApiAssessments]] =
    http.GET[Option[ApiAssessments]](s"$baseUrl/dashboard/owner/assessments/$submissionId")

  def getClientAssessments(submissionId: String)(implicit hc: HeaderCarrier): Future[Option[ApiAssessments]] =
    http.GET[Option[ApiAssessments]](s"$baseUrl/dashboard/agent/assessments/$submissionId")

  def canChallenge(plSubmissionId: String, assessmentRef: Long, caseRef: String, isOwner: Boolean)(
        implicit hc: HeaderCarrier): Future[Option[CanChallengeResponse]] = {
    val interestedParty = if (isOwner) "client" else "agent"
    http
      .GET[HttpResponse](
        s"$baseUrl/property-links/$plSubmissionId/check-cases/$caseRef/canChallenge?valuationId=$assessmentRef&party=$interestedParty")
      .map { resp =>
        resp.status match {
          case 200 =>
            Json.parse(resp.body).asOpt[CanChallengeResponse]
          case _ => None
        }
      } recover {
      case x @ _ =>
        logger.debug(s"unable to start a challenge: $x")
        None
    }
  }

  def getMyOrganisationAgents()(implicit hc: HeaderCarrier): Future[AgentList] =
    http.GET[AgentList](s"$baseUrl/owner/agents")

  def getMyOrganisationPropertyLinksCount()(implicit hc: HeaderCarrier): Future[Int] =
    http.GET[Int](s"$baseUrl/owner/property-links/count")

  def assignAgent(agentRelationshipRequest: AgentAppointmentChangesRequest)(
        implicit hc: HeaderCarrier): Future[AgentAppointmentChangesResponse] =
    http
      .POST[AgentAppointmentChangesRequest, AgentAppointmentChangesResponse](
        s"$baseUrl/my-organisation/agent/assign",
        agentRelationshipRequest)

  def assignAgentToSomeProperties(request: AppointAgentToSomePropertiesRequest)(
        implicit hc: HeaderCarrier): Future[AgentAppointmentChangesResponse] =
    http
      .POST[AppointAgentToSomePropertiesRequest, AgentAppointmentChangesResponse](
        s"$baseUrl/my-organisation/agent/assign-to-some-properties",
        request)

  def removeAgentFromOrganisation(agentRelationshipRequest: AgentAppointmentChangesRequest)(
        implicit hc: HeaderCarrier): Future[AgentAppointmentChangesResponse] =
    http
      .POST[AgentAppointmentChangesRequest, AgentAppointmentChangesResponse](
        s"$baseUrl/my-organisation/agent/remove-from-organisation ",
        agentRelationshipRequest)

  def unassignAgent(agentRelationshipRequest: AgentAppointmentChangesRequest)(
        implicit hc: HeaderCarrier): Future[AgentAppointmentChangesResponse] =
    http
      .POST[AgentAppointmentChangesRequest, AgentAppointmentChangesResponse](
        s"$baseUrl/my-organisation/agent/unassign",
        agentRelationshipRequest)

  def unassignAgentFromSomeProperties(request: AppointAgentToSomePropertiesRequest)(
        implicit hc: HeaderCarrier): Future[AgentAppointmentChangesResponse] =
    http
      .POST[AppointAgentToSomePropertiesRequest, AgentAppointmentChangesResponse](
        s"$baseUrl/my-organisation/agent/unassign-from-some-properties",
        request)

  def getMyOrganisationsCheckCases(propertyLinkSubmissionId: String)(
        implicit hc: HeaderCarrier): Future[List[CaseDetails]] =
    http
      .GET[CheckCasesWithAgent](s"$baseUrl/check-cases/$propertyLinkSubmissionId/client")
      .map(_.checkCases.map(CaseDetails.apply))

  def getMyClientsCheckCases(propertyLinkSubmissionId: String)(implicit hc: HeaderCarrier): Future[List[CaseDetails]] =
    http
      .GET[CheckCasesWithClient](s"$baseUrl/check-cases/$propertyLinkSubmissionId/agent")
      .map(_.checkCases.map(CaseDetails.apply))

  def getPropertyHistory(uarn: Long)(implicit hc: HeaderCarrier): Future[PropertyHistory] =
    http.GET[PropertyHistory](s"$vmBaseUrl/rating-listing/api/properties/$uarn")

}
