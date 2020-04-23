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

import controllers.{Pagination, VoaPropertyLinkingSpec}
import models._
import models.propertyrepresentation.AgentDetails
import org.scalacheck.Arbitrary._
import play.api.http.Status._
import resources._
import uk.gov.hmrc.http.{ForbiddenException, HeaderCarrier, HttpResponse, Upstream4xxResponse}

import scala.concurrent.Future

class PropertyRepresentationConnectorSpec extends VoaPropertyLinkingSpec {

  implicit val hc = HeaderCarrier()

  class Setup {
    val connector = new PropertyRepresentationConnector(servicesConfig, mockWSHttp) {
      override lazy val baseUrl: String = "tst-url"
    }
  }

  "validateAgentCode" must "return an agent code validation result" in new Setup {
    val agentCodeValidationResult = mock[AgentCodeValidationResult]

    mockHttpGET[AgentCodeValidationResult]("tst-url", agentCodeValidationResult)
    whenReady(connector.validateAgentCode(1, 1))(_ mustBe agentCodeValidationResult)
  }

  "create" must "post a representation request" in new Setup {
    val representationRequest = mock[RepresentationRequest]

    mockHttpPOST[RepresentationRequest, HttpResponse]("tst-url", HttpResponse(OK))
    whenReady(connector.create(representationRequest))(_ mustBe ())
  }

  "response" must "put a response to a representation request" in new Setup {
    val representationResponse = mock[RepresentationResponse]

    mockHttpPUT[RepresentationResponse, HttpResponse]("tst-url", HttpResponse(OK))
    whenReady(connector.response(representationResponse))(_ mustBe ())
  }

  "update" must "put a response to a representation request" in new Setup {
    val representationResponse = mock[RepresentationResponse]

    mockHttpPUT[RepresentationResponse, HttpResponse]("tst-url", HttpResponse(OK))
    whenReady(connector.response(representationResponse))(_ mustBe ())
  }

  "revoke" must "patch a revoke request" in new Setup {
    mockHttpPATCH[String, HttpResponse]("tst-url", HttpResponse(OK))
    whenReady(connector.revoke(1))(_ mustBe ())
  }

  "getAgentDetails" must "get some agent's details" in new Setup {
    val mockAgentDetails = mock[Option[AgentDetails]]

    mockHttpGET[Option[AgentDetails]]("tst-url", mockAgentDetails)
    whenReady(connector.getAgentDetails(1))(_ mustBe mockAgentDetails)
  }

  "getAgentDetails" must "return None for Forbidden response" in new Setup {

    mockHttpGET[Option[AgentDetails]](
      "tst-url",
      Future.failed(new Upstream4xxResponse("org is not an agent", 403, 403)))
    whenReady(connector.getAgentDetails(1))(_ mustBe None)
  }
}
