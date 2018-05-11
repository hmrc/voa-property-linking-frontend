/*
 * Copyright 2018 HM Revenue & Customs
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

import controllers.{VoaPropertyLinkingSpec, Pagination, PaginationSearchSort}
import models._
import models.searchApi.AgentAuthResult
import org.scalacheck.Arbitrary._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsValue, Json}
import resources._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.StubServicesConfig
import play.api.http.Status._

class PropertyRepresentationConnectorSpec extends VoaPropertyLinkingSpec {

  implicit val hc = HeaderCarrier()

  class Setup {
    val connector = new PropertyRepresentationConnector(StubServicesConfig, mockWSHttp) {
      override lazy val baseUrl: String = "tst-url"
    }
  }

  "validateAgentCode" must "return an agent code validation result" in new Setup {
    val agentCodeValidationResult = mock[AgentCodeValidationResult]

    mockHttpGET[AgentCodeValidationResult]("tst-url", agentCodeValidationResult)
    whenReady(connector.validateAgentCode(1, 1))(_ mustBe agentCodeValidationResult)
  }

  "forAgent" must "return the property representations for an agent" in new Setup {
    val propertyRepresentation = mock[PropertyRepresentations]
    val representationStatus = arbitrary[RepresentationStatus].sample.get
    val pagination = mock[Pagination]

    mockHttpGET[PropertyRepresentations]("tst-url", propertyRepresentation)
    whenReady(connector.forAgent(representationStatus, 1, pagination))(_ mustBe propertyRepresentation)
  }

  "forAgentSearchAndSort" must "return the authorisations for an agent" in new Setup {
    val agentAuthResult = mock[AgentAuthResult]
    val pagination = mock[PaginationSearchSort]

    mockHttpGET[AgentAuthResult]("tst-url", agentAuthResult)
    whenReady(connector.forAgentSearchAndSort(1, pagination))(_ mustBe agentAuthResult)
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

}