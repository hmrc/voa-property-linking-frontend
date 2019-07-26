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

package connectors.propertyLinking

import java.time.Instant

import javax.inject.{Inject, Singleton}
import actions.BasicAuthenticatedRequest
import binders.GetPropertyLinksParameters
import com.google.inject.ImplementedBy
import config.WSHttp
import connectors.fileUpload.FileMetadata
import controllers.{Pagination, PaginationParams, PaginationSearchSort}
import models.OwnerOrAgent.OwnerOrAgent
import models._
import models.searchApi.{AgentPropertiesParameters, OwnerAuthAgent, OwnerAuthResult, OwnerAuthorisation}
import play.api
import play.api.Logger
import play.api.libs.json.Json
import session.LinkingSessionRequest
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, NotFoundException}
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.ws.WSHttpResponse

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PropertyLinkConnector @Inject()(config: ServicesConfig, http: WSHttp)(implicit ec: ExecutionContext) extends PropertyLinksConnector {
  lazy val baseUrl: String = config.baseUrl("property-linking") + s"/property-linking"


  def getOwnerLink(submissionId: String)(implicit hc: HeaderCarrier): Future[Option[PropertyLink]] = {
    val url = s"$baseUrl/owner/property-links/$submissionId/"

    http.GET[Option[PropertyLink]](url)
  }

  def getClientLink(submissionId: String)(implicit hc: HeaderCarrier): Future[Option[PropertyLink]] = {
    val url = s"$baseUrl/agent/property-links/$submissionId/"

    http.GET[Option[PropertyLink]](url)
  }

  def linkToProperty(data: FileMetadata)(implicit request: LinkingSessionRequest[_]): Future[Unit] = {
    implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(request.request.headers, Some(request.request.session))
    val url = s"$baseUrl/property-links"
    val linkRequest = PropertyLinkRequest(
      request.ses.uarn,
      request.organisationId,
      request.ses.personId,
      Capacity.fromDeclaration(request.ses.declaration),
      Instant.now,
      data.linkBasis,
      data.fileInfo.toSeq,
      request.ses.submissionId
    )
    http.POST[PropertyLinkRequest, HttpResponse](url, linkRequest) map { _ => () }
  }

  def linkedPropertiesSearchAndSort(searchParams: GetPropertyLinksParameters,
                                    pagination: PaginationParams,
                                    representationStatusFilter: Seq[RepresentationStatus] =
                                        Seq(RepresentationApproved, RepresentationPending),
                                    ownerOrAgent: OwnerOrAgent)
                                   (implicit hc: HeaderCarrier): Future[OwnerAuthResult] = {


    val ownerAuthResult = http.GET[OwnerAuthResult](s"$baseUrl/$ownerOrAgent/property-links", List(searchParams.address.map("searchParams.address" -> _),
      searchParams.baref.map("searchParams.baref" -> _), searchParams.agent.map("searchParams.agent" -> _),searchParams.status.map("searchParams.status" -> _),
      searchParams.status.map("searchParams.status" -> _), searchParams.sortfield.map("searchParams.sortfield" -> _),searchParams.sortorder.map("searchParams.sortorder" -> _)).flatten ++
      Seq("params.startPoint" -> pagination.startPoint.toString,
          "params.pageSize" -> pagination.startPoint.toString,
          "params.requestTotalRowCount" -> pagination.requestTotalRowCount.toString))

    def validAgent(agent: OwnerAuthAgent): Boolean =
      agent.status.fold(false) { status =>
        representationStatusFilter.map(_.name.toUpperCase).contains(status.toUpperCase)
      }

    // filter agents on representationStatus
    ownerAuthResult.map(oar =>
      oar.copy(authorisations = oar.authorisations.map(auth =>
        auth.copy(agents = auth.agents.map(ags => ags.filter(ag => validAgent(ag)))))))
  }

  def appointableProperties(organisationId: Long,
                            pagination: AgentPropertiesParameters)
                           (implicit hc: HeaderCarrier): Future[OwnerAuthResult] = {

    http.GET[OwnerAuthResult](s"$baseUrl/property-links-appointable?" +
      s"ownerId=$organisationId" +
      s"&${pagination.queryString}")
  }

  def clientProperty(authorisationId: Long, clientOrgId: Long, agentOrgId: Long)(implicit hc: HeaderCarrier): Future[Option[ClientProperty]] = {
    val url =
      s"$baseUrl/property-links/client-property/$authorisationId" +
        s"?clientOrganisationId=$clientOrgId" +
        s"&agentOrganisationId=$agentOrgId"

    http.GET[Option[ClientProperty]](url) recover { case _: NotFoundException => None }
  }

  def getOwnerAssessmentsWithCapacity(submissionId: String, organisationId: Long)(implicit hc: HeaderCarrier): Future[Option[ApiAssessments]] = {
      http.GET[Option[ApiAssessments]](s"$baseUrl/dashboard/owner/assessments/$submissionId/$organisationId")
    }

  def getClientAssessmentsWithCapacity(submissionId: String, organisationId: Long)(implicit hc: HeaderCarrier): Future[Option[ApiAssessments]] = {
    http.GET[Option[ApiAssessments]](s"$baseUrl/dashboard/agent/assessments/$submissionId/$organisationId")
  }

  def getOwnerAssessments(submissionId: String)(implicit hc: HeaderCarrier): Future[Option[ApiAssessments]] = {
    http.GET[Option[ApiAssessments]](s"$baseUrl/owner/dashboard/assessments/$submissionId")
  }

  def getClientAssessments(submissionId: String)(implicit hc: HeaderCarrier): Future[Option[ApiAssessments]] = {
    http.GET[Option[ApiAssessments]](s"$baseUrl/agent/dashboard/assessments/$submissionId")
  }

  override def canChallenge(plSubmissionId: String, assessmentRef: Long, caseRef: String, isAgentOwnProperty: Boolean)(implicit request: BasicAuthenticatedRequest[_], hc: HeaderCarrier): Future[Option[CanChallengeResponse]]= {
    val interestedParty =  request.organisationAccount.isAgent && !isAgentOwnProperty match {
      case true => "agent"
      case false => "client"
    }
    http.GET[HttpResponse](s"$baseUrl/property-links/$plSubmissionId/check-cases/$caseRef/canChallenge?valuationId=$assessmentRef&party=$interestedParty").map{ resp =>
      resp.status match {
        case 200 => {
          Json.parse(resp.body).asOpt[CanChallengeResponse]
        }
        case _ => None
      }
    } recover{
      case x @ _ => {
        Logger.debug(s"unable to start a challenge: $x")
        None
      }
    }
  }
}


@ImplementedBy(classOf[PropertyLinkConnector])
trait PropertyLinksConnector {

  def getOwnerLink(submissionId: String)(implicit hc: HeaderCarrier): Future[Option[PropertyLink]]
  def getClientLink(submissionId: String)(implicit hc: HeaderCarrier): Future[Option[PropertyLink]]
  def linkToProperty(data: FileMetadata)(implicit request: LinkingSessionRequest[_]): Future[Unit]

    def linkedPropertiesSearchAndSort(searchParams: GetPropertyLinksParameters,
                                    pagination: PaginationParams,
                                    representationStatusFilter: Seq[RepresentationStatus] =
                                    Seq(RepresentationApproved, RepresentationPending),
                                    ownerOrAgent: OwnerOrAgent)
                                   (implicit hc: HeaderCarrier): Future[OwnerAuthResult]
  def appointableProperties(organisationId: Long,
                            pagination: AgentPropertiesParameters)
                           (implicit hc: HeaderCarrier): Future[OwnerAuthResult]
  def clientProperty(authorisationId: Long, clientOrgId: Long, agentOrgId: Long)(implicit hc: HeaderCarrier): Future[Option[ClientProperty]]
  def getOwnerAssessmentsWithCapacity(submissionId: String, organisationId: Long)(implicit hc: HeaderCarrier): Future[Option[ApiAssessments]]
  def getClientAssessmentsWithCapacity(submissionId: String, organisationId: Long)(implicit hc: HeaderCarrier): Future[Option[ApiAssessments]]
  def getOwnerAssessments(submissionId: String)(implicit hc: HeaderCarrier): Future[Option[ApiAssessments]]
  def getClientAssessments(submissionId: String)(implicit hc: HeaderCarrier): Future[Option[ApiAssessments]]
  def canChallenge(plSubmissionId: String, assessmentRef: Long, caseRef: String, isAgentOwnProperty: Boolean)(implicit request: BasicAuthenticatedRequest[_], hc: HeaderCarrier): Future[Option[CanChallengeResponse]]
}
