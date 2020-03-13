/*
 * Copyright 2020 HM Revenue & Customs
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

import actions.requests.BasicAuthenticatedRequest
import binders.propertylinks.GetPropertyLinksParameters
import controllers.PaginationParams
import javax.inject.{Inject, Singleton}
import models._
import models.propertylinking.payload.PropertyLinkPayload
import models.propertyrepresentation.{AgentList, AppointAgentRequest, AppointAgentResponse}
import models.searchApi.{AgentPropertiesParameters, OwnerAuthResult}
import play.api.Logger
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, NotFoundException}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PropertyLinkConnector @Inject()(config: ServicesConfig, http: HttpClient)(implicit ec: ExecutionContext) {
  lazy val baseUrl: String = config.baseUrl("property-linking") + s"/property-linking"

  def createPropertyLink(propertyLinkPayload: PropertyLinkPayload)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    http.POST[PropertyLinkPayload, HttpResponse](s"$baseUrl/property-links", propertyLinkPayload)

  def getMyOrganisationPropertyLink(submissionId: String)(implicit hc: HeaderCarrier): Future[Option[PropertyLink]] =
    http.GET[Option[PropertyLink]](s"$baseUrl/owner/property-links/$submissionId")

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
        representationStatusFilter: Seq[RepresentationStatus] = Seq(RepresentationApproved, RepresentationPending),
        organisationId: Long,
        agentOrganisationId: Long,
        agentAppointed: Option[String] = None
  )(implicit hc: HeaderCarrier): Future[OwnerAuthResult] = {

    val ownerAuthResult = http.GET[OwnerAuthResult](
      s"$baseUrl/owner/property-links/appointable",
      List(
        searchParams.address.map("address"  -> _),
        searchParams.baref.map("baref"      -> _),
        searchParams.agent.map("agent"      -> _),
        searchParams.status.map("status"    -> _),
        Some("sortfield"                    -> searchParams.sortfield.toString),
        Some("sortorder"                    -> searchParams.sortorder.toString),
        agentAppointed.map("agentAppointed" -> _.toString),
        Some("organisationId"               -> organisationId.toString),
        Some("agentOrganisationId"          -> agentOrganisationId.toString)
      ).flatten ++
        List(
          "startPoint"           -> pagination.startPoint.toString,
          "pageSize"             -> pagination.pageSize.toString,
          "requestTotalRowCount" -> pagination.requestTotalRowCount.toString
        )
    )

    // filter agents on representationStatus
    ownerAuthResult.map(oar =>
      oar.copy(authorisations = oar.authorisations.map(auth => auth.copy(agents = auth.agents))))
  }

  def filterAgents(
        ownerAuthResult: Future[OwnerAuthResult],
        representationStatusFilter: Seq[RepresentationStatus]): Future[OwnerAuthResult] =
    ownerAuthResult.map(
      oar =>
        oar.copy(
          authorisations = oar.authorisations
            .map(auth => auth.copy(agents = auth.agents))
            .filter(auth => auth.agents.nonEmpty)))

  def appointableProperties(organisationId: Long, pagination: AgentPropertiesParameters)(
        implicit hc: HeaderCarrier): Future[OwnerAuthResult] =
    http.GET[OwnerAuthResult](
      s"$baseUrl/property-links-appointable?" +
        s"ownerId=$organisationId" +
        s"&${pagination.queryString}")

  def clientProperty(authorisationId: Long, clientOrgId: Long, agentOrgId: Long)(
        implicit hc: HeaderCarrier): Future[Option[ClientProperty]] = {
    val url =
      s"$baseUrl/property-links/client-property/$authorisationId" +
        s"?clientOrganisationId=$clientOrgId" +
        s"&agentOrganisationId=$agentOrgId"

    http.GET[Option[ClientProperty]](url) recover { case _: NotFoundException => None }
  }

  def getOwnerAssessmentsWithCapacity(submissionId: String)(
        implicit hc: HeaderCarrier): Future[Option[ApiAssessments]] =
    http.GET[Option[ApiAssessments]](s"$baseUrl/dashboard/owner/assessments/$submissionId")

  def getClientAssessmentsWithCapacity(submissionId: String)(
        implicit hc: HeaderCarrier): Future[Option[ApiAssessments]] =
    http.GET[Option[ApiAssessments]](s"$baseUrl/dashboard/agent/assessments/$submissionId")

  def getOwnerAssessments(submissionId: String)(implicit hc: HeaderCarrier): Future[Option[ApiAssessments]] =
    http.GET[Option[ApiAssessments]](s"$baseUrl/dashboard/owner/assessments/$submissionId")

  def getClientAssessments(submissionId: String)(implicit hc: HeaderCarrier): Future[Option[ApiAssessments]] =
    http.GET[Option[ApiAssessments]](s"$baseUrl/dashboard/agent/assessments/$submissionId")

  def canChallenge(plSubmissionId: String, assessmentRef: Long, caseRef: String, isAgentOwnProperty: Boolean)(
        implicit request: BasicAuthenticatedRequest[_],
        hc: HeaderCarrier): Future[Option[CanChallengeResponse]] = {
    val interestedParty = if (request.organisationAccount.isAgent && !isAgentOwnProperty) "agent" else "client"
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
        Logger.debug(s"unable to start a challenge: $x")
        None
    }
  }

  def getMyOrganisationAgents()(implicit hc: HeaderCarrier): Future[AgentList] =
    http.GET[AgentList](s"$baseUrl/owner/agents")

  def sendAppointAgentRequest(agentRelationshipRequest: AppointAgentRequest)(
        implicit hc: HeaderCarrier): Future[AppointAgentResponse] =
    http
      .POST[AppointAgentRequest, AppointAgentResponse](
        s"$baseUrl/my-organisation/agent/appoint",
        agentRelationshipRequest)

}
