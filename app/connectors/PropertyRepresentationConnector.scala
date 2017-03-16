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

import models._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._

import scala.concurrent.{ExecutionContext, Future}

class PropertyRepresentationConnector(http: HttpGet with HttpPut with HttpPost with HttpPatch)(implicit ec: ExecutionContext)
  extends ServicesConfig {
  lazy val baseUrl: String = baseUrl("property-representations") + s"/property-linking"

  def validateAgentCode(agentCode:Long, authorisationId: Long)(implicit hc: HeaderCarrier) = {
    val url = baseUrl + s"/property-representations/validate-agent-code/$agentCode/$authorisationId"
    http.GET[AgentCodeValidationResult](url)
  }

  def get(representationId: Long)(implicit hc: HeaderCarrier): Future[Option[PropertyRepresentation]] = {
    val url = baseUrl + s"/property-representations/$representationId"
    http.GET[Option[PropertyRepresentation]](url)
  }

  def forAgent(status: String, agentOrganisationId: Int)(implicit hc: HeaderCarrier): Future[PropertyRepresentations] = {
    val url = baseUrl + s"/property-representations/agent/$status/$agentOrganisationId"
    http.GET[PropertyRepresentations](url)
  }

  def find(linkId: Long)(implicit hc: HeaderCarrier): Future[Seq[PropertyRepresentation]] = {
    val url = baseUrl + s"/property-representations/linkId/$linkId"
    http.GET[Seq[PropertyRepresentation]](url)
  }

  def create(reprRequest: RepresentationRequest)(implicit hc: HeaderCarrier): Future[Unit] = {
    val url = baseUrl + s"/property-representations/create"
    http.POST[RepresentationRequest, HttpResponse](url, reprRequest) map { _ => () }
  }

  def response(representationResponse: RepresentationResponse)(implicit hc: HeaderCarrier): Future[Unit] = {
    val url = baseUrl + s"/property-representations/response"
    http.PUT[RepresentationResponse, HttpResponse](url, representationResponse) map { _ => () }
  }

  def update(updated: UpdatedRepresentation)(implicit hc: HeaderCarrier): Future[Unit] = {
    val url = baseUrl + s"/property-representations/update"
    http.PUT[UpdatedRepresentation, HttpResponse](url, updated) map { _ => () }
  }

  def revoke(authorisedPartyId: Long)(implicit hc: HeaderCarrier): Future[Unit] = {
    val url = baseUrl + s"/property-representations/revoke/${authorisedPartyId}"
    http.PATCH[String, HttpResponse](url, "") map { _ => () }
  }
}
