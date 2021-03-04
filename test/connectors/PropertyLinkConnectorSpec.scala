/*
 * Copyright 2021 HM Revenue & Customs
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

import binders.propertylinks.GetPropertyLinksParameters
import connectors.propertyLinking.PropertyLinkConnector
import controllers.{DefaultPaginationParams, PaginationParams, VoaPropertyLinkingSpec}
import models._
import models.dvr.cases.check.myclients.CheckCasesWithClient
import models.dvr.cases.check.myorganisation.CheckCasesWithAgent
import models.propertylinking.payload.PropertyLinkPayload
import models.propertylinking.requests.PropertyLinkRequest
import models.propertyrepresentation.AgentList
import models.searchApi.OwnerAuthResult
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalacheck.Arbitrary._
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, NotFoundException}
import utils._

import scala.concurrent.Future

class PropertyLinkConnectorSpec extends VoaPropertyLinkingSpec {

  implicit val hc = HeaderCarrier()

  class Setup {
    val connector = new PropertyLinkConnector(config = servicesConfig, http = mockHttpClient) {
      override lazy val baseUrl: String = "tst-url"
    }
  }

  "get" must "return a property link" in new Setup {
    val propertyLink: PropertyLink = arbitrary[PropertyLink]

    mockHttpGETOption[PropertyLink]("tst-url", propertyLink)
    whenReady(connector.getMyOrganisationPropertyLink("11111"))(_ mustBe Some(propertyLink))
  }

  "linkToProperty" must "successfully post a property link request" in new Setup {
    val response = emptyJsonHttpResponse(OK)
    mockHttpPOST[PropertyLinkRequest, HttpResponse]("tst-url", response)
    whenReady(connector.createPropertyLink(mock[PropertyLinkPayload]))(_ mustBe response)
  }

  "linkToProperty" must "successfully post a property link request on client behalf" in new Setup {
    val response = emptyJsonHttpResponse(OK)
    val clientId = 100
    mockHttpPOST[PropertyLinkRequest, HttpResponse]("/clients/clientId/property-links", response)
    whenReady(connector.createPropertyLinkOnClientBehalf(mock[PropertyLinkPayload], clientId))(_ mustBe response)
  }

  "clientProperty" must "return a client property if it exists" in new Setup {
    val clientProperty = arbitrary[ClientProperty].sample.get

    mockHttpGETOption[ClientProperty]("tst-url", clientProperty)
    whenReady(connector.clientProperty(1, 1, 1))(_ mustBe Some(clientProperty))
  }

  "clientProperty" must "return None if the client property is not found" in new Setup {
    mockHttpFailedGET[ClientProperty]("tst-url", new NotFoundException("Client property not found"))
    whenReady(connector.clientProperty(1, 1, 1))(_ mustBe None)
  }

  "getLink" must "return a property link if it exists" in new Setup {
    val propertyLink = arbitrary[PropertyLink].sample.get

    mockHttpGETOption[PropertyLink]("tst-url", propertyLink)
    whenReady(connector.getMyOrganisationPropertyLink("1"))(_ mustBe Some(propertyLink))
  }

  "getMyOrganisationAgents" must "return organisation's agents" in new Setup {
    mockHttpGET[AgentList]("tst-url", organisationsAgentsList)
    whenReady(connector.getMyOrganisationAgents())(_ mustBe organisationsAgentsList)
  }

  "getMyAgentPropertyLinks" must "return agent property links" in new Setup {
    when(mockHttpClient.GET[OwnerAuthResult](any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(ownerAuthResultWithOneAuthorisation))

    connector
      .getMyAgentPropertyLinks(agentCode, GetPropertyLinksParameters(), DefaultPaginationParams)
      .futureValue mustBe ownerAuthResultWithOneAuthorisation

  }

  "getMyOrganisationPropertyLinksCount" must "return organisation's property links count" in new Setup {
    val propertyLinksCount = 1

    mockHttpGET[Int]("tst-url", propertyLinksCount)
    whenReady(connector.getMyOrganisationPropertyLinksCount())(_ mustBe propertyLinksCount)
  }

  "clientPropertyLink" must "return a client property link if it exists" in new Setup {
    val clientProperty = mock[ClientPropertyLink]

    mockHttpGETOption[ClientPropertyLink]("tst-url", clientProperty)
    whenReady(connector.clientPropertyLink("some-submission-id"))(_ mustBe Some(clientProperty))
  }

  "clientPropertyLink" must "return None if the client property link is not found" in new Setup {
    mockHttpFailedGET[ClientPropertyLink]("tst-url", new NotFoundException("Client property not found"))
    whenReady(connector.clientPropertyLink("some-submission-id"))(_ mustBe None)
  }

  "getMyOrganisationsCheckCases" must "return organisation's submitted check cases for the given property link" in new Setup {
    mockHttpGET[CheckCasesWithAgent]("tst-url", ownerCheckCasesResponse)
    whenReady(connector.getMyOrganisationsCheckCases("PL1343"))(_ mustBe List(ownerCheckCaseDetails))
  }

  "getMyClientsCheckCases" must "return organisation's submitted check cases for the given property link" in new Setup {
    mockHttpGET[CheckCasesWithClient]("tst-url", agentCheckCasesResponse)
    whenReady(connector.getMyClientsCheckCases("PL1343"))(_ mustBe List(agentCheckCaseDetails))
  }

  "getMyOrganisationsPropertyLinks" must "return OwnerAuthResult" in new Setup {
    mockHttpGETWithQueryParam[OwnerAuthResult]("tst-url", ownerAuthResultResponse)
    val res: Future[OwnerAuthResult] = connector.getMyOrganisationsPropertyLinks(
      GetPropertyLinksParameters(status = Some("APPROVED")),
      PaginationParams(1, 10, requestTotalRowCount = false))
    whenReady(res)(_ mustBe ownerAuthResultResponse)
  }

  "Owner request for getMyOrganisationPropertyLinksWithAgentFiltering" must "return OwnerAuthResult" in new Setup {
    mockHttpGETWithQueryParam[OwnerAuthResult]("tst-url", ownerAuthResultResponse)
    val res: Future[OwnerAuthResult] = connector.getMyOrganisationPropertyLinksWithAgentFiltering(
      searchParams = GetPropertyLinksParameters(status = Some("APPROVED")),
      pagination = PaginationParams(1, 10, requestTotalRowCount = false),
      organisationId = 1L,
      agentOrganisationId = 1L,
      agentAppointed = Some("NO"),
      agentCode = 1L
    )
    whenReady(res)(_ mustBe ownerAuthResultResponse)
  }

  "Agent request for getMyOrganisationPropertyLinksWithAgentFiltering" must "return OwnerAuthResult" in new Setup {
    mockHttpGETWithQueryParam[OwnerAuthResult]("tst-url", ownerAuthResultResponse)
    val res: Future[OwnerAuthResult] = connector.getMyOrganisationPropertyLinksWithAgentFiltering(
      searchParams = GetPropertyLinksParameters(status = Some("APPROVED")),
      pagination = PaginationParams(1, 10, requestTotalRowCount = false),
      organisationId = 1L,
      agentOrganisationId = 1L,
      agentAppointed = Some("BOTH"),
      agentCode = 1L
    )
  }

  "canChallenge" must "return CanChallengeResponse" in new Setup {
    mockHttpGET[HttpResponse](
      "tst-url",
      HttpResponse(status = 200, json = Json.obj("result" -> true, "reasonCode" -> "CODE"), headers = Map.empty)
    )
    val res: Future[Option[CanChallengeResponse]] = connector.canChallenge(
      plSubmissionId = "PL123",
      assessmentRef = 1234L,
      caseRef = "CHK-123",
      isOwner = true
    )
    whenReady(res)(_ mustBe Some(CanChallengeResponse(result = true, reasonCode = Some("CODE"), reason = None)))
  }

  "canChallenge" must "return None [CanChallengeResponse] when anything goes wrong" in new Setup {
    mockHttpGET[HttpResponse](
      "tst-url",
      HttpResponse(status = 502, body = "Bad Gateway")
    )
    val res: Future[Option[CanChallengeResponse]] = connector.canChallenge(
      plSubmissionId = "PL123",
      assessmentRef = 1234L,
      caseRef = "CHK-123",
      isOwner = true
    )
    whenReady(res)(_ mustBe None)
  }

}
