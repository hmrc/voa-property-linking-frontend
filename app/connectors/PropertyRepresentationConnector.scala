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

package connectors

import controllers.Pagination
import javax.inject.Inject
import models._
import models.propertyrepresentation.AgentOrganisation
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, NotFoundException}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

class PropertyRepresentationConnector @Inject()(serverConfig: ServicesConfig, http: HttpClient)(implicit ec: ExecutionContext) {
  lazy val baseUrl: String = s"${serverConfig.baseUrl("property-linking")}/property-linking"

  def validateAgentCode(agentCode:Long, authorisationId: Long)(implicit hc: HeaderCarrier): Future[AgentCodeValidationResult] = {
    http.GET[AgentCodeValidationResult](s"$baseUrl/property-representations/validate-agent-code/$agentCode/$authorisationId")
  }

  def forAgent(status: RepresentationStatus, agentOrganisationId: Long, pagination: Pagination)(implicit hc: HeaderCarrier): Future[PropertyRepresentations] = {
    http.GET[PropertyRepresentations](s"$baseUrl/property-representations/agent/${status.name}/$agentOrganisationId?$pagination")
  }

  def create(reprRequest: RepresentationRequest)(implicit hc: HeaderCarrier): Future[Unit] = {
    http.POST[RepresentationRequest, HttpResponse](s"$baseUrl/property-representations/create", reprRequest) map { _ => () }
  }

  def response(representationResponse: RepresentationResponse)(implicit hc: HeaderCarrier): Future[Unit] = {
    http.PUT[RepresentationResponse, HttpResponse](s"$baseUrl/property-representations/response", representationResponse) map { _ => () }
  }

  def revoke(authorisedPartyId: Long)(implicit hc: HeaderCarrier): Future[Unit] = {
    http.PATCH[String, HttpResponse](s"$baseUrl/property-representations/revoke/$authorisedPartyId", "") map { _ => () }
  }

  def getAgent(agentCode: Long)(implicit hc: HeaderCarrier): Future[Option[AgentOrganisation]] = {
    http.GET[Option[AgentOrganisation]](s"$baseUrl/property-representations/agent/$agentCode") recover {
      case _: NotFoundException => None
    }
  }
}
