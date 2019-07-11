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

import binders.GetPropertyLinksParameters
import connectors.fileUpload.FileMetadata
import connectors.propertyLinking.PropertyLinkConnector
import controllers.{PaginationParams, PaginationSearchSort, VoaPropertyLinkingSpec}
import models._
import models.searchApi.{AgentPropertiesParameters, OwnerAuthResult, OwnerAuthorisation}
import org.scalacheck.Arbitrary._
import play.api.http.Status.OK
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.test.FakeRequest
import resources._
import session.LinkingSessionRequest
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, NotFoundException}
import utils.StubServicesConfig

class PropertyLinkConnectorSpec extends VoaPropertyLinkingSpec {

  implicit val hc = HeaderCarrier()

  class Setup {
    val connector = new PropertyLinkConnector(StubServicesConfig, mockWSHttp) {
      override lazy val baseUrl: String = "tst-url"
    }
  }

  "get" must "return a property link" in new Setup {
    val propertyLink = arbitrary[PropertyLink].sample.get.copy()

    mockHttpGETOption[PropertyLink]("tst-url", propertyLink)
    whenReady(connector.getOwnerLink("11111"))(_ mustBe Some(propertyLink))
  }

//  "get" must "return None if no property link is found for the given organisation ID" in new Setup {
//    val propertyLink = arbitrary[PropertyLink].sample.get.copy()
//
//    mockHttpGETOption[PropertyLink]("tst-url", propertyLink)
//    whenReady(connector.getOwnerLink("111111"))(_ mustBe None)
//  }

  "linkToProperty" must "successfully post a property link request" in new Setup {
    val individualAccount = arbitrary[DetailedIndividualAccount]
    val groupAccount = arbitrary[GroupAccount]
    val capacityDeclaration = CapacityDeclaration(capacity = Occupier, interestedBefore2017 = true, fromDate = None, stillInterested = true)
    val linkingSession = LinkingSession(address = "123 Test Lane",
      uarn = 1, envelopeId = "abc", submissionId = "a001", personId = individualAccount.individualId, declaration = capacityDeclaration)

    val fileMetadata = FileMetadata(linkBasis = RatesBillFlag, fileInfo = Some(FileInfo("file.jpg", Lease)))

    mockHttpPOST[PropertyLinkRequest, HttpResponse]("tst-url", HttpResponse(OK))
    whenReady(connector.linkToProperty(fileMetadata)
    (LinkingSessionRequest(linkingSession,
      individualAccount.organisationId,
      individualAccount,
      groupAccount,
      FakeRequest())))(_ mustBe ())
  }

//  "linkedPropertiesSearchAndSort" must "return the property links for an owner" in new Setup {
//    val validPagination = PaginationSearchSort(pageNumber = 1, pageSize = 1)
//    val paginationParams = PaginationParams(1,1, false)
//    val ownerAuthResult = OwnerAuthResult(start = 1,
//      size = 1,
//      filterTotal = 1,
//      total = 1,
//      authorisations = Seq(arbitrary[OwnerAuthorisation].sample.get.copy(agents = None))
//    )
//
//    mockHttpGET[OwnerAuthResult]("tst-url", ownerAuthResult)
//    whenReady(connector.linkedPropertiesSearchAndSort(GetPropertyLinksParameters(), paginationParams, ownerOrAgent = "owner"))(_ mustBe ownerAuthResult)
//  }

  "appointableProperties" must "return the properties appointable to an agent" in new Setup {
    val agentPropertiesParameters = AgentPropertiesParameters(agentCode = 1)
    val ownerAuthResult = OwnerAuthResult(start = 1,
      size = 1,
      filterTotal = 1,
      total = 1,
      authorisations = Seq(arbitrary[OwnerAuthorisation].sample.get.copy(agents = None))
    )

    mockHttpGET[OwnerAuthResult]("tst-url", ownerAuthResult)
    whenReady(connector.appointableProperties(1, agentPropertiesParameters))(_ mustBe ownerAuthResult)
  }

  "clientProperty" must "return a client property if it exists" in new Setup {
    val clientProperty = arbitrary[ClientProperty].sample.get

    mockHttpGETOption[ClientProperty]("tst-url", clientProperty)
    whenReady(connector.clientProperty(1, 1, 1))(_ mustBe Some(clientProperty))
  }

  "clientProperty" must "return None if the client property is not found" in new Setup {
    val clientProperty = arbitrary[ClientProperty].sample.get

    mockHttpFailedGET[ClientProperty]("tst-url", new NotFoundException("Client property not found"))
    whenReady(connector.clientProperty(1, 1, 1))(_ mustBe None)
  }

  "getLink" must "return a property link if it exists" in new Setup {
    val propertyLink = arbitrary[PropertyLink].sample.get

    mockHttpGETOption[PropertyLink]("tst-url", propertyLink)
    whenReady(connector.getOwnerLink("1"))(_ mustBe Some(propertyLink))
  }

//  "getLink" must "return None if the property link is not found" in new Setup {
//    val propertyLink = arbitrary[PropertyLink].sample.get
//
//    mockHttpFailedGET[PropertyLink]("tst-url", new NotFoundException("Property link not found"))
//    whenReady(connector.getOwnerLink("1"))(_ mustBe None)
//  }

}
