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

import java.time.LocalDateTime

import controllers.VoaPropertyLinkingSpec
import models.dvr.DetailedValuationRequest
import models.dvr.documents.{Document, DocumentSummary, DvrDocumentFiles}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mockito.{ArgumentMatchers => Matchers}
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future

class DVRCaseManagementConnectorSpec extends VoaPropertyLinkingSpec {

  class Setup {
    val mockWsClient = mock[WSClient]
    val connector = new DVRCaseManagementConnector(servicesConfig, mockWsClient, mockHttpClient) {
      override val url: String = "tst-url"
    }
  }

  "requestDetailedValuation" should "successfully post a detailed valuation request" in new Setup {
    val detailedValuationRequest = mock[DetailedValuationRequest]

    mockHttpPOST[DetailedValuationRequest, HttpResponse]("tst-url", emptyJsonHttpResponse(OK))
    whenReady(connector.requestDetailedValuation(detailedValuationRequest))(_ shouldBe ((): Unit))
  }

  "dvrExists" should "successfully return a boolean" in new Setup {
    mockHttpGET[Boolean]("tst-url", true)
    whenReady(connector.dvrExists(1, 2))(_ shouldBe true)
  }

  "request detailed valuation" should "update the valuation with the detailed valuation request" in new Setup {

    val dvrUrl = s"/dvr-case-management-api/dvr_case/create_dvr_case"

    mockHttpPOST(dvrUrl, emptyJsonHttpResponse(200))

    val dvr = DetailedValuationRequest(
      authorisationId = 123456,
      organisationId = 9876543,
      personId = 1111111,
      submissionId = "submission1",
      assessmentRef = 24680,
      agents = Nil,
      billingAuthorityReferenceNumber = ""
    )

    val result: Unit = await(connector.requestDetailedValuation(dvr))
    result shouldBe ((): Unit)
  }

  "get dvr documents" should "return the documents and transfer them into an optional" in new Setup {
    val valuationId = 1L
    val uarn = 2L
    val propertyLinkId = "PL-123456789"

    val now = LocalDateTime.now()
    private val someDvrDocumentFiles: Some[DvrDocumentFiles] = Some(
      DvrDocumentFiles(
        checkForm = Document(DocumentSummary("1L", "Check Document", now)),
        detailedValuation = Document(DocumentSummary("2L", "Detailed Valuation Document", now))
      ))

    when(
      mockHttpClient.GET[Option[DvrDocumentFiles]](Matchers.anyString(), Matchers.any(), Matchers.any())(
        Matchers.any(),
        Matchers.any[HeaderCarrier](),
        Matchers.any()))
      .thenReturn(Future.successful(someDvrDocumentFiles))

    val result = await(connector.getDvrDocuments(valuationId, uarn, propertyLinkId))

    result shouldBe someDvrDocumentFiles
  }

  "get dvr documents" should "return None when the documents don't exist" in new Setup {
    val valuationId = 1L
    val uarn = 2L
    val propertyLinkId = "PL-123456789"

    when(
      mockHttpClient.GET[Option[DvrDocumentFiles]](Matchers.anyString(), Matchers.any(), Matchers.any())(
        Matchers.any(),
        Matchers.any[HeaderCarrier](),
        Matchers.any()))
      .thenReturn(Future.successful(None))

    val result = await(connector.getDvrDocuments(valuationId, uarn, propertyLinkId))
    result shouldBe None
  }

  "get dvr document" should "return streamed Documents" in new Setup {
    val mockWsRequest = mock[WSRequest]
    when(mockWsClient.url(any())).thenReturn(mockWsRequest)
    when(mockWsRequest.withMethod(any())).thenReturn(mockWsRequest)
    when(mockWsRequest.withHttpHeaders(any())).thenReturn(mockWsRequest)
    when(mockWsRequest.stream()).thenReturn(Future.successful(mock[mockWsRequest.Response]))

    connector.getDvrDocument(1L, 1L, "PL-1234", "1").futureValue
  }
}
