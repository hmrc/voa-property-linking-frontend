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

package controllers

import java.time.LocalDateTime

import connectors.propertyLinking.PropertyLinkConnector
import connectors.{DVRCaseManagementConnector, SubmissionIdConnector, _}
import models._
import models.dvr.documents.{Document, DocumentSummary, DvrDocumentFiles}
import org.mockito.ArgumentMatchers.{any, eq => matching}
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary.arbitrary
import play.api.test.FakeRequest
import play.api.test.Helpers._
import resources._
import uk.gov.hmrc.http.HeaderCarrier
import utils.{StubAuthentication, StubPropertyLinkConnector}

import scala.concurrent.Future

class DvrControllerSpec extends VoaPropertyLinkingSpec {

  trait Setup {
    implicit val request = FakeRequest()
    val mockPropertyLinkConnector = mock[PropertyLinkConnector]
    val controller = new DvrController(
      StubPropertyLinkConnector,
      StubAuthentication,
      mockSubmissionIds,
      mockDvrCaseManagement
    )

    lazy val mockDvrCaseManagement = mock[DVRCaseManagementConnector]


    lazy val mockSubmissionIds = {
      val m = mock[SubmissionIdConnector]
      when(m.get(matching("DVR"))(any[HeaderCarrier])).thenReturn(Future.successful("DVR123"))
      m
    }

    val organisation = arbitrary[GroupAccount].sample.get
    val person = arbitrary[DetailedIndividualAccount].sample.get
    val link: PropertyLink = arbitrary[PropertyLink].sample.get.copy().copy(pending = false)

    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(organisation, person)))
    StubPropertyLinkConnector.stubLink(link)

  }

  "detailed valuation check" must "return 200 OK when dvr case does not exist" in new Setup {
    when(mockDvrCaseManagement.dvrExists(any(), any())(any[HeaderCarrier])).thenReturn(Future.successful(false))
    val result = controller.detailedValuationRequestCheck(1L, 1L, "billingAuthorityReference")(request)

    status(result) mustBe OK
  }

  "detailed valuation check" must "return 303 SEE_OTHER when dvr case does exist" in new Setup {
    when(mockDvrCaseManagement.dvrExists(any(), any())(any[HeaderCarrier])).thenReturn(Future.successful(true))
    val result = controller.detailedValuationRequestCheck(1L, 1L, "billingAuthorityReference")(request)

    status(result) mustBe SEE_OTHER
  }

  "request detailed valuation" must "return 303 SEE_OTHER when request is valid" in new Setup {
    when(mockDvrCaseManagement.requestDetailedValuation(any())(any[HeaderCarrier])).thenReturn(Future.successful(()))
    val result = controller.requestDetailedValuation(1L, 1L, "billingAuthorityReference")(request)

    status(result) mustBe SEE_OTHER
  }

  "request detailed valuation confirmation" must "return 200 OK when request is valid" in new Setup {
    val result = controller.confirmation(link.authorisationId, "billingAuthorityReference")(request)

    status(result) mustBe OK
  }

  "already submitted detailed valuation request" must "return 200 OK when dvr not ready" in new Setup {
    when(mockDvrCaseManagement.getDvrDocuments(any(), any(), any())(any[HeaderCarrier])).thenReturn(Future.successful(None))
    val result = controller.alreadySubmittedDetailedValuationRequest(1L, link.authorisationId, "billingAuthorityReference")(request)

    status(result) mustBe OK
  }

  "already submitted detailed valuation request" must "return 200 OK when dvr ready" in new Setup {
    val now = LocalDateTime.now()

    when(mockDvrCaseManagement.getDvrDocuments(any(), any(), any())(any[HeaderCarrier])).thenReturn(Future.successful(Some(DvrDocumentFiles(
      checkForm = Document(DocumentSummary(1L, "Check Document", now)),
      detailedValuation = Document(DocumentSummary(2L, "Detailed Valuation Document", now))
    ))))

    val result = controller.alreadySubmittedDetailedValuationRequest(1L, link.authorisationId, "billingAuthorityReference")(request)

    status(result) mustBe OK
  }
}
