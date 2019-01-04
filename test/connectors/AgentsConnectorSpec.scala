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

import controllers.VoaPropertyLinkingSpec
import models.searchApi.{OwnerAgent, OwnerAgents}
import uk.gov.hmrc.http.HeaderCarrier
import utils.StubServicesConfig
import scala.concurrent.ExecutionContext.global

class AgentsConnectorSpec extends VoaPropertyLinkingSpec {

  implicit val hc = HeaderCarrier()
  implicit val ec = global

  class Setup {
    val connector = new AgentsConnector(mockWSHttp, StubServicesConfig)(ec) {
      val url: String = "tst-url"
    }
  }

  "ownerAgents" must "return a valid sequence of agents for a client using their organisation ID" in new Setup {
    val validOwnerAgent = OwnerAgent(name = "Agent 47", ref = 123)
    val validOwnerAgents = OwnerAgents(Seq(validOwnerAgent))

    mockHttpGET[OwnerAgents]("tst-url", validOwnerAgents)
    whenReady(connector.ownerAgents(1))(_ mustBe validOwnerAgents)
  }

}
