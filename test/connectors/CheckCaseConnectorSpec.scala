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

import actions.BasicAuthenticatedRequest
import config.WSHttp
import controllers.VoaPropertyLinkingSpec
import models._
import models.searchApi.{OwnerAgent, OwnerAgents}
import org.scalacheck.Arbitrary._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.config.inject.ServicesConfig
import utils.{StubServicesConfig, TestCheckCasesData}
import resources._
import session.LinkingSessionRequest
import models.searchApi.{AgentPropertiesParameters, OwnerAuthResult, OwnerAuthorisation}
import play.api.mvc.Request
import play.api.test.FakeRequest

import scala.concurrent.ExecutionContext._

class CheckCaseConnectorSpec extends VoaPropertyLinkingSpec with TestCheckCasesData{

  implicit val hc = HeaderCarrier()
  val propertyLink = arbitrary[PropertyLink].sample.get.copy(organisationId = 1)
  implicit val ec = global

  implicit val basicAuthenticatedRequest = BasicAuthenticatedRequest(
    GroupAccount(
      id = 123,
      groupId = "321",
      companyName = "company",
      addressId = 456,
      email = "aa@bb.cc",
      phone = "0",
      isAgent = false,
      agentCode = 0
    ),
    DetailedIndividualAccount(
      externalId = "external-id",
      trustId = "trust-id",
      organisationId = 123,
      individualId = 456,
      details = IndividualDetails(
        firstName = "Mr",
        lastName = "Man",
        email = "aa@bb.cc",
        phone1 = "123",
        phone2 = None,
        addressId = 789
      )
    ),
    FakeRequest()
  )


  class Setup {
    val connector = new CheckCaseConnector(StubServicesConfig, mockWSHttp) {
      override lazy val baseUrl: String = "tst-url"
    }
  }

  "call to agents check cases" must "return a valid AgentCheckCasesResponse" in new Setup {

    mockHttpGET[AgentCheckCasesResponse]("tst-url", agentCheckCasesResponse)
    whenReady(connector.getCheckCases(Some(propertyLink), false))(_.get mustBe agentCheckCasesResponse)
  }

  "call to client check cases" must "return a valid OwnerCheckCasesResponse" in new Setup {

    mockHttpGET[OwnerCheckCasesResponse]("tst-url", ownerCheckCasesResponse)
    whenReady(connector.getCheckCases(Some(propertyLink), true))(_.get mustBe ownerCheckCasesResponse)
  }

}
