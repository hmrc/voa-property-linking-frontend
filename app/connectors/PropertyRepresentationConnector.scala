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

import controllers.Pagination
import models._
import models.searchApi.{AgentAuthResult, OwnerAuthResult}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._

import scala.concurrent.{ExecutionContext, Future}

class PropertyRepresentationConnector(http: HttpGet with HttpPut with HttpPost with HttpPatch)(implicit ec: ExecutionContext)
  extends ServicesConfig {
  lazy val baseUrl: String = s"${baseUrl("property-linking")}/property-linking"

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
                            pagination: Pagination,
                            sortfield: Option[String] = None,
                            sortorder: Option[String] = None,
                            status: Option[String] = None,
                            address: Option[String] = None,
                            baref: Option[String] = None,
                            client: Option[String] = None)
                           (implicit hc: HeaderCarrier): Future[AgentAuthResult] = {
    http.GET[AgentAuthResult](s"$baseUrl/property-representations-search-sort?" +
      s"organisationId=$agentOrganisationId&" +
      s"$pagination&" +
      buildUppercaseQueryParams("sortfield", sortfield) +
      buildUppercaseQueryParams("sortorder", sortorder) +
      buildUppercaseQueryParams("status", status) +
      buildQueryParams("address", address) +
      buildQueryParams("baref", baref) +
      buildQueryParams("client", client)
    )

  }

  private def buildQueryParams(name : String, value : Option[String]) : String = {
    value match { case Some(paramValue) if paramValue != "" => s"&$name=$paramValue" ; case _ => ""}
  }

  private def buildUppercaseQueryParams(name : String, value : Option[String]) : String = {
    value match { case Some(paramValue) if paramValue != "" => s"&$name=${paramValue.toUpperCase}" ; case _ => ""}
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
