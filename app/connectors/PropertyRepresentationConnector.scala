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

package connectors

import javax.inject.Inject

import controllers.{Pagination, PaginationSearchSort}
import models._
import models.searchApi.AgentAuthResult
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.{ExecutionContext, Future}

class PropertyRepresentationConnector @Inject()(serverConfig: ServicesConfig, http: WSHttp)(implicit ec: ExecutionContext) {
  lazy val baseUrl: String = s"${serverConfig.baseUrl("property-linking")}/property-linking"

  def validateAgentCode(agentCode:Long, authorisationId: Long)(implicit hc: HeaderCarrier): Future[AgentCodeValidationResult] = {
    http.GET[AgentCodeValidationResult](s"$baseUrl/property-representations/validate-agent-code/$agentCode/$authorisationId")
  }

  def get(representationId: Long)(implicit hc: HeaderCarrier): Future[Option[PropertyRepresentation]] = {
    http.GET[Option[PropertyRepresentation]](s"$baseUrl/property-representations/$representationId")
  }

  def forAgent(status: RepresentationStatus, agentOrganisationId: Int, pagination: Pagination)(implicit hc: HeaderCarrier): Future[PropertyRepresentations] = {
    http.GET[PropertyRepresentations](s"$baseUrl/property-representations/agent/${status.name}/$agentOrganisationId?$pagination")
  }

  def forAgentSearchAndSort(agentOrganisationId: Int,
                            pagination: PaginationSearchSort)
                           (implicit hc: HeaderCarrier): Future[AgentAuthResult] = {
    val url = s"$baseUrl/property-representations-search-sort?" +
      s"organisationId=$agentOrganisationId&" +
      s"$pagination"
    http.GET[AgentAuthResult](url)
  }

  def find(linkId: Long)(implicit hc: HeaderCarrier): Future[Seq[PropertyRepresentation]] = {
    http.GET[Seq[PropertyRepresentation]](s"$baseUrl/property-representations/linkId/$linkId")
  }

  def create(reprRequest: RepresentationRequest)(implicit hc: HeaderCarrier): Future[Unit] = {
    http.POST[RepresentationRequest, HttpResponse](s"$baseUrl/property-representations/create", reprRequest) map { _ => () }
  }

  def response(representationResponse: RepresentationResponse)(implicit hc: HeaderCarrier): Future[Unit] = {
    http.PUT[RepresentationResponse, HttpResponse](s"$baseUrl/property-representations/response", representationResponse) map { _ => () }
  }

  def update(updated: UpdatedRepresentation)(implicit hc: HeaderCarrier): Future[Unit] = {
    http.PUT[UpdatedRepresentation, HttpResponse](s"$baseUrl/property-representations/update", updated) map { _ => () }
  }

  def revoke(authorisedPartyId: Long)(implicit hc: HeaderCarrier): Future[Unit] = {
    http.PATCH[String, HttpResponse](s"$baseUrl/property-representations/revoke/$authorisedPartyId", "") map { _ => () }
  }
}
