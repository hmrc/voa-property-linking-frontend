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

package connectors

import controllers.{Pagination, VoaPropertyLinkingSpec}
import models._
import org.scalacheck.Arbitrary._
import play.api.http.Status._
import resources._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

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

  "forAgent" must "return the property representations for an agent" in new Setup {
    val propertyRepresentation = mock[PropertyRepresentations]
    val representationStatus = arbitrary[RepresentationStatus].sample.get
    val pagination = mock[Pagination]

    mockHttpGET[PropertyRepresentations]("tst-url", propertyRepresentation)
    whenReady(connector.forAgent(representationStatus, 1, pagination))(_ mustBe propertyRepresentation)
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
