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

package controllers

import connectors.propertyLinking.PropertyLinkConnector
import connectors.{DVRCaseManagementConnector, SubmissionIdConnector, _}
import controllers.detailedValuationRequest.DvrController
import models._
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
      mockPropertyLinkConnector,
      StubAuthentication,
      mockSubmissionIds,
      mockDvrCaseManagement,
      mockBusinessRatesAuthorisation
    )

    lazy val mockDvrCaseManagement = mock[DVRCaseManagementConnector]
    lazy val mockBusinessRatesAuthorisation = mock[BusinessRatesAuthorisation]


    lazy val mockSubmissionIds = {
      val m = mock[SubmissionIdConnector]
      when(m.get(matching("DVR"))(any[HeaderCarrier])).thenReturn(Future.successful("DVR123"))
      m
    }

    val organisation = arbitrary[GroupAccount].sample.get
    val person = arbitrary[DetailedIndividualAccount].sample.get
    val assessment = arbitrary[Assessment].sample.get
    val link: PropertyLink = arbitrary[PropertyLink].sample.get.copy().copy()

    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(organisation, person)))
    StubPropertyLinkConnector.stubLink(link)

  }


//  "detailed valuation check" must "return 200 OK when dvr case does not exist" in new Setup {
//    val now = LocalDateTime.now()
//
//    when(mockDvrCaseManagement.getDvrDocuments(any(), any(), any())(any[HeaderCarrier])).thenReturn(Future.successful(Some(DvrDocumentFiles(
//      checkForm = Document(DocumentSummary("1L", "Check Document", now)),
//      detailedValuation = Document(DocumentSummary("2L", "Detailed Valuation Document", now))
//    ))))
//
//    val result = controller.detailedValuationRequestCheck("1111", link.authorisationId, 1L, "billingAuthorityReference")(request)
//
//    status(result) mustBe OK
//  }
//
//  "detailed valuation check" must "return 303 SEE_OTHER when dvr case does exist" in new Setup {
//    when(mockDvrCaseManagement.getDvrDocuments(any(), any(), any())(any[HeaderCarrier])).thenReturn(Future.successful(None))
//
//    val result = controller.detailedValuationRequestCheck("1111", link.authorisationId, assessment.assessmentRef, "billingAuthorityReference")(request)
//
//    status(result) mustBe SEE_OTHER
//  }

//  "request detailed valuation" must "return 303 SEE_OTHER when request is valid" in new Setup {
//    when(mockDvrCaseManagement.requestDetailedValuationV2(any())(any[HeaderCarrier])).thenReturn(Future.successful(()))
//    val result = controller.requestDetailedValuation(1L, 1L, "billingAuthorityReference")(request)
//
//    status(result) mustBe SEE_OTHER
//  }

//  "request detailed valuation confirmation" must "return 200 OK when request is valid" in new Setup {
//    val result = controller.confirmation(link.authorisationId, "billingAuthorityReference")(request)
//
//    status(result) mustBe OK
//  }

  //Turning these off until after ways of working discussion
//
//  "already submitted detailed valuation request" must "return 200 OK when dvr does not exist" in new Setup {
//    when(mockDvrCaseManagement.dvrExists(any(), any())(any[HeaderCarrier])).thenReturn(Future.successful(false))
//
//    when(mockDvrCaseManagement.getDvrDocuments(any(), any(), any())(any[HeaderCarrier])).thenReturn(Future.successful(None))
//    val result = controller.alreadySubmittedDetailedValuationRequest("11111", 1L, 1L, "billingAuthorityReference", "some address", "01 April 2017", Some(123456L), true)(request)
//
//    status(result) mustBe OK
//  }
//
//  "already submitted detailed valuation request" must "return 200 OK when dvr already exists" in new Setup {
//    when(mockBusinessRatesAuthorisation.isAgentOwnProperty(any())(any[HeaderCarrier])).thenReturn(Future successful true)
//    when(mockDvrCaseManagement.dvrExists(any(), any())(any[HeaderCarrier])).thenReturn(Future.successful(true))
//
//    val mockApiAssessments = {
//      val apiAssessment = mock[ApiAssessments]
//      when(apiAssessment.assessments).thenReturn(List.fill(1)(mock[ApiAssessment]))
//      apiAssessment
//    }
//    when(mockPropertyLinkConnector.getOwnerAssessments(any())(any())).thenReturn(Future.successful(Some(mockApiAssessments)))
//    when(mockPropertyLinkConnector.getClientAssessments(any())(any())).thenReturn(Future.successful(Some(mockApiAssessments)))
//
//    val result = controller.alreadySubmittedDetailedValuationRequest(link.submissionId, link.authorisationId, 1L, "billingAuthorityReference", "some address", "01 April 2017", Some(123456L), false)(request)
//
//    status(result) mustBe OK
//  }
}
