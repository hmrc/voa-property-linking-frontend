/*
 * Copyright 2022 HM Revenue & Customs
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

import connectors.challenge.ChallengeConnector
import controllers.VoaPropertyLinkingSpec
import models.challenge.myclients.ChallengeCasesWithClient
import models.challenge.myorganisations.ChallengeCasesWithAgent
import uk.gov.hmrc.http.HeaderCarrier

class ChallengeConnectorSpec extends VoaPropertyLinkingSpec {

  implicit val hc = HeaderCarrier()

  class Setup {
    val connector = new ChallengeConnector(servicesConfig, mockHttpClient) {
      override lazy val baseUrl: String = "tst-url"
    }
  }

  "getMyOrganisationsChallengeCases" should "return organisation's submitted challenge cases for the given property link" in new Setup {
    mockHttpGETWithQueryParam[ChallengeCasesWithAgent]("tst-url", ownerChallengeCasesResponse)
    whenReady(connector.getMyOrganisationsChallengeCases("PL1343"))(_ shouldBe List(ownerChallengeCaseDetails))
  }

  "getMyClientsChallengeCases" should "return organisation's submitted challenge cases for the given property link" in new Setup {
    mockHttpGETWithQueryParam[ChallengeCasesWithClient]("tst-url", agentChallengeCasesResponse)
    whenReady(connector.getMyClientsChallengeCases("PL1343"))(_ shouldBe List(agentChallengeCaseDetails))
  }

}
