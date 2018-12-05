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

import java.time.LocalDateTime

import akka.stream.scaladsl.Source
import controllers.VoaPropertyLinkingSpec
import models.dvr.documents.{Document, DocumentSummary, DvrDocumentFiles}
import models.dvr.{DetailedValuationRequest, StreamedDocument}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.libs.json.Json
import play.api.libs.ws.{DefaultWSResponseHeaders, StreamedResponse, WSClient, WSRequest}
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse, NotFoundException}
import utils.StubServicesConfig
import org.mockito.{ArgumentMatchers => Matchers}

import scala.concurrent.Future

class DVRCaseManagementConnectorSpec extends VoaPropertyLinkingSpec {

  implicit val hc = HeaderCarrier()

  class Setup {
    val mockWsClient = mock[WSClient]
    val connector = new DVRCaseManagementConnector(StubServicesConfig, mockWsClient, mockWSHttp) {
      override val url: String = "tst-url"
    }
  }

  "requestDetailedValuation" must "successfully post a detailed valuation request" in new Setup {
    val detailedValuationRequest = mock[DetailedValuationRequest]

    mockHttpPOST[DetailedValuationRequest, HttpResponse]("tst-url", HttpResponse(OK))
    whenReady(connector.requestDetailedValuation(detailedValuationRequest))(_ mustBe ())
  }

  "dvrExists" must "successfully return a boolean" in new Setup {
    mockHttpGET[Boolean]("tst-url", true)
    whenReady(connector.dvrExists(1,2))(_ mustBe true)
  }

  "request detailed valuation" must "update the valuation with the detailed valuation request" in new Setup {

      val dvrUrl = s"/dvr-case-management-api/dvr_case/create_dvr_case"

    mockHttpPOST(dvrUrl, HttpResponse(200))

      val dvr = DetailedValuationRequest(
        authorisationId = 123456,
        organisationId = 9876543,
        personId = 1111111,
        submissionId = "submission1",
        assessmentRef = 24680,
        billingAuthorityReferenceNumber = "barn1"
      )

      val result: Unit = await(connector.requestDetailedValuation(dvr))
      result mustBe ()
    }

  "get dvr documents" must "return the documents and transfer them into an optional" in new Setup {
      val valuationId = 1L
      val uarn = 2L
      val propertyLinkId = "PL-123456789"

      val dvrUrl = s"/dvr-case-management-api/dvr_case/$uarn/valuation/$valuationId/files?propertyLinkId=PL-123456789"

      val now = LocalDateTime.now()

    when(mockWSHttp.GET[DvrDocumentFiles](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any[HeaderCarrier](), Matchers.any()))
      .thenReturn(Future.successful(DvrDocumentFiles(
        checkForm = Document(DocumentSummary(1L, "Check Document", now)),
        detailedValuation = Document(DocumentSummary(2L, "Detailed Valuation Document", now))
      )))

      val result = await(connector.getDvrDocuments(valuationId, uarn, propertyLinkId))
      result mustBe Some(DvrDocumentFiles(
        checkForm = Document(DocumentSummary(1L, "Check Document", now)),
        detailedValuation = Document(DocumentSummary(2L, "Detailed Valuation Document", now))
      ))
    }

  "get dvr documents" must "return None when the documents don't exist" in new Setup {
    val valuationId = 1L
    val uarn = 2L
    val propertyLinkId = "PL-123456789"

    val dvrUrl = s"/dvr-case-management-api/dvr_case/$uarn/valuation/$valuationId/files?propertyLinkId=PL-123456789"

    val now = LocalDateTime.now()

    when(mockWSHttp.GET[DvrDocumentFiles](Matchers.anyString(), Matchers.any())(Matchers.any(), Matchers.any[HeaderCarrier](), Matchers.any()))
      .thenReturn(Future.failed(new NotFoundException("Documents dont exist")))

    val result = await(connector.getDvrDocuments(valuationId, uarn, propertyLinkId))
    result mustBe None
  }

  "get dvr document" must "return streamed Documents" in new Setup {
    val mockWsRequest = mock[WSRequest]
    when(mockWsClient.url(any())).thenReturn(mockWsRequest)
    when(mockWsRequest.withMethod(any())).thenReturn(mockWsRequest)
    when(mockWsRequest.withHeaders(any())).thenReturn(mockWsRequest)
    when(mockWsRequest.stream())
      .thenReturn(Future.successful(StreamedResponse(DefaultWSResponseHeaders(200, Map.empty), Source.empty)))

    val result = connector.getDvrDocument(1L, 1l, "PL-1234", 1)

    await(result) mustBe an[StreamedDocument]
  }
}
