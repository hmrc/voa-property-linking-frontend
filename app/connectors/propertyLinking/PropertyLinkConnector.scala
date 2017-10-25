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

package connectors.propertyLinking

import java.time.Instant
import javax.inject.Inject

import connectors.fileUpload.FileMetadata
import controllers.{Pagination, PaginationSearchSort}
import models._
import models.searchApi.{OwnerAuthAgent, OwnerAuthResult}
import session.LinkingSessionRequest
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.{ExecutionContext, Future}

class PropertyLinkConnector @Inject()(config: ServicesConfig, http: WSHttp)(implicit ec: ExecutionContext) {
  lazy val baseUrl: String = config.baseUrl("property-linking") + s"/property-linking"

  def get(organisationId: Long, authorisationId: Long)(implicit hc: HeaderCarrier): Future[Option[PropertyLink]] = {
    val url = s"$baseUrl/property-links/$authorisationId"

    http.GET[Option[PropertyLink]](url) map { link =>
      link.find(_.organisationId == organisationId)
    } recover {
      case _: NotFoundException => None
    }
  }

  def linkToProperty(data: FileMetadata)(implicit request: LinkingSessionRequest[_]): Future[Unit] = {
    implicit val hc = HeaderCarrier.fromHeadersAndSession(request.request.headers, Some(request.request.session))
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

  def linkedProperties(organisationId: Long, pagination: Pagination)
                      (implicit hc: HeaderCarrier): Future[PropertyLinkResponse] = {
    http.GET[PropertyLinkResponse](s"$baseUrl/property-links?organisationId=$organisationId&$pagination")
  }

  def linkedPropertiesSearchAndSort(organisationId: Long,
                                    pagination: PaginationSearchSort)
                                   (implicit hc: HeaderCarrier): Future[OwnerAuthResult] = {

    val ownerAuthResult = http.GET[OwnerAuthResult](s"$baseUrl/property-links-search-sort?" +
      s"organisationId=$organisationId&" +
      s"$pagination")

    def validAgent(agent: OwnerAuthAgent) = {
      Seq(RepresentationApproved.name, RepresentationPending.name)
        .contains(agent.status.getOrElse(RepresentationApproved.name).toUpperCase)
    }

    // filter out agents with status other than Approved, Pending
    // if status is not present, then keep the agent
    ownerAuthResult.map(oar =>
      oar.copy(authorisations = oar.authorisations.map(auth =>
        auth.copy(agents = auth.agents.map(ags => ags.filter(ag => validAgent(ag)))))))

  }

  def clientProperty(authorisationId: Long, clientOrgId: Long, agentOrgId: Long)(implicit hc: HeaderCarrier): Future[Option[ClientProperty]] = {
    val url =
      s"$baseUrl/property-links/client-property/$authorisationId" +
        s"?clientOrganisationId=$clientOrgId" +
        s"&agentOrganisationId=$agentOrgId"

    http.GET[Option[ClientProperty]](url) recover { case _: NotFoundException => None }
  }

  def getLink(authorisationId: Long)(implicit hc: HeaderCarrier): Future[Option[PropertyLink]] = {
    http.GET[Option[PropertyLink]](s"$baseUrl/dashboard/assessments/$authorisationId") recover { case _: NotFoundException => None }
  }
}
