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

import controllers.VoaPropertyLinkingSpec
import models.propertyrepresentation.AgentDetails
import play.api.http.Status._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, Upstream4xxResponse}

import scala.concurrent.Future

class PropertyRepresentationConnectorSpec extends VoaPropertyLinkingSpec {

  implicit val hc = HeaderCarrier()

  class Setup {
    val connector = new PropertyRepresentationConnector(servicesConfig, mockWSHttp) {
      override lazy val baseUrl: String = "tst-url"
    }
  }

  "getAgentDetails" must "get some agent's details" in new Setup {
    val mockAgentDetails = mock[Option[AgentDetails]]

    mockHttpGET[Option[AgentDetails]]("tst-url", mockAgentDetails)
    whenReady(connector.getAgentDetails(1))(_ mustBe mockAgentDetails)
  }

  "getAgentDetails" must "return None for Forbidden response" in new Setup {

    mockHttpGET[Option[AgentDetails]]("tst-url", Future.failed(Upstream4xxResponse("org is not an agent", 403, 403)))
    whenReady(connector.getAgentDetails(1))(_ mustBe None)
  }

  "revokeClientProperty" must "send a DELETE request" in new Setup {
    mockHttpDELETE[HttpResponse]("tst-url", HttpResponse(NO_CONTENT))
    whenReady(connector.revokeClientProperty("some-submission-id"))(_ mustBe ((): Unit))
  }
}
