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

package controllers.detailedvaluationrequest

import connectors.SubmissionIdConnector
import controllers.VoaPropertyLinkingSpec
import models._
import models.dvr.documents.{Document, DocumentSummary, DvrDocumentFiles}
import org.mockito.ArgumentMatchers.{any, eq => matching}
import org.mockito.Mockito.{never, verify, when}
import org.scalacheck.Arbitrary.arbitrary
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.govukfrontend.views.html.components._
import utils.{StubPropertyLinkConnector, _}
import views.html.dvr._
import views.html.errors.propertyMissing

import java.time.LocalDateTime
import scala.concurrent.Future

class DvrControllerSpec extends VoaPropertyLinkingSpec {

  trait Setup {
    implicit val request = FakeRequest()

    val controller = new DvrController(
      errorHandler = mockCustomErrorHandler,
      propertyLinks = mockPropertyLinkConnector,
      challengeConnector = mockChallengeConnector,
      authenticated = preAuthenticatedActionBuilders(),
      submissionIds = mockSubmissionIds,
      dvrCaseManagement = mockDvrCaseManagement,
      alreadyRequestedDetailedValuationView = new alreadyRequestedDetailedValuation(mainLayout),
      requestDetailedValuationView = new requestDetailedValuation(mainLayout, GovukButton, FormWithCSRF),
      requestedDetailedValuationView = new requestedDetailedValuation(mainLayout),
      dvrFilesView = new dvrFiles(mainLayout, GovukButton, GovukDetails, GovukWarningText),
      cannotRaiseChallengeView = new cannotRaiseChallenge(mainLayout),
      propertyMissingView = new propertyMissing(mainLayout)
    )

    lazy val mockSubmissionIds = {
      val m = mock[SubmissionIdConnector]
      when(m.get(matching("DVR"))(any())).thenReturn(Future.successful("DVR123"))
      m
    }

    val organisation = arbitrary[GroupAccount].sample.get
    val person = arbitrary[DetailedIndividualAccount].sample.get
    val assessment = arbitrary[Assessment].sample.get
    val link: PropertyLink = arbitrary[PropertyLink]

    StubPropertyLinkConnector.stubLink(link)

    def assessments: ApiAssessments = apiAssessments(ownerAuthorisation)
  }

  "detailed valuation check" must "return 200 OK when DVR case does not exist" in new Setup {
    val now = LocalDateTime.now()

    when(mockPropertyLinkConnector.getOwnerAssessments(any())(any()))
      .thenReturn(Future.successful(Some(assessments)))
    when(mockPropertyLinkConnector.getMyOrganisationsCheckCases(any())(any()))
      .thenReturn(Future.successful(List.empty))
    when(mockChallengeConnector.getMyOrganisationsChallengeCases(any())(any()))
      .thenReturn(Future.successful(List.empty))
    when(mockDvrCaseManagement.getDvrDocuments(any(), any(), any())(any())).thenReturn(
      Future.successful(
        Some(
          DvrDocumentFiles(
            checkForm = Document(DocumentSummary("1L", "Check Document", now)),
            detailedValuation = Document(DocumentSummary("2L", "Detailed Valuation Document", now))
          ))))

    val result =
      controller.myOrganisationRequestDetailValuationCheck(
        propertyLinkSubmissionId = "1111",
        valuationId =
          assessments.assessments.headOption.fold(fail("expected to find at least 1 assessment"))(_.assessmentRef),
        uarn = 1L
      )(request)

    status(result) mustBe OK
    contentAsString(result) must include("You can download the")

    verify(mockPropertyLinkConnector).getMyOrganisationsCheckCases(any())(any())
    verify(mockChallengeConnector).getMyOrganisationsChallengeCases(any())(any())
  }

  "draft detailed valuation" must "return 200 OK and not fetch checks/challenges " in new Setup {
    val now = LocalDateTime.now()
    val firstAssessment: ApiAssessment =
      assessments.assessments.headOption.getOrElse(fail("expected to find at least 1 assessment"))
    val draftAssessment: ApiAssessment = firstAssessment.copy(listType = ListType.DRAFT)

    val ownerAssessments: ApiAssessments =
      assessments.copy(assessments = draftAssessment :: assessments.assessments.tail.toList)

    when(mockPropertyLinkConnector.getOwnerAssessments(any())(any()))
      .thenReturn(Future.successful(Some(ownerAssessments)))
    when(mockDvrCaseManagement.getDvrDocuments(any(), any(), any())(any())).thenReturn(
      Future.successful(
        Some(
          DvrDocumentFiles(
            checkForm = Document(DocumentSummary("1L", "Check Document", now)),
            detailedValuation = Document(DocumentSummary("2L", "Detailed Valuation Document", now))
          ))))

    val result =
      controller.myOrganisationRequestDetailValuationCheck(
        propertyLinkSubmissionId = "1111",
        valuationId = firstAssessment.assessmentRef,
        uarn = 1L
      )(request)

    status(result) mustBe OK
    contentAsString(result) must include("You can download the")

    verify(mockPropertyLinkConnector, never()).getMyOrganisationsCheckCases(any())(any())
    verify(mockChallengeConnector, never()).getMyOrganisationsChallengeCases(any())(any())
  }

  "detailed valuation check" must "return 303 SEE_OTHER when DVR case does exist" in new Setup {
    when(mockPropertyLinkConnector.getOwnerAssessments(any())(any()))
      .thenReturn(Future.successful(Some(assessments)))
    when(mockPropertyLinkConnector.getMyOrganisationsCheckCases(any())(any()))
      .thenReturn(Future.successful(List.empty))
    when(mockChallengeConnector.getMyOrganisationsChallengeCases(any())(any()))
      .thenReturn(Future.successful(List.empty))
    when(mockDvrCaseManagement.getDvrDocuments(any(), any(), any())(any()))
      .thenReturn(Future.successful(None))

    val result = controller.myOrganisationRequestDetailValuationCheck(
      propertyLinkSubmissionId = "1111",
      valuationId = link.authorisationId,
      uarn = assessment.assessmentRef)(request)

    status(result) mustBe SEE_OTHER
    redirectLocation(result) mustBe Some(
      s"/business-rates-property-linking/my-organisation/property-link/1111/valuations/${link.authorisationId}/exists")
  }

  "request detailed valuation" must "return 303 SEE_OTHER when request is valid" in new Setup {
    when(mockPropertyLinkConnector.getOwnerAssessments(any())(any()))
      .thenReturn(Future.successful(Some(assessments)))
    when(mockDvrCaseManagement.requestDetailedValuationV2(any())(any())).thenReturn(Future.successful(()))

    val result = controller.requestDetailedValuation(link.submissionId, 1L, true)(request)

    status(result) mustBe SEE_OTHER
    redirectLocation(result) mustBe Some(
      s"/business-rates-property-linking/my-organisation/property-link/${link.submissionId}/confirmation?submissionId=DVR123")
  }

  "request detailed valuation confirmation" must "return 200 OK when request is valid" in new Setup {
    when(mockPropertyLinkConnector.getOwnerAssessments(any())(any()))
      .thenReturn(Future.successful(Some(assessments)))

    val result = controller.confirmation(link.submissionId, link.submissionId, true)(request)

    status(result) mustBe OK
  }

  "request detailed valuation as Agent" must "return 303 SEE_OTHER when request is valid" in new Setup {
    when(mockPropertyLinkConnector.getClientAssessments(any())(any()))
      .thenReturn(Future.successful(Some(assessments)))
    when(mockDvrCaseManagement.requestDetailedValuationV2(any())(any())).thenReturn(Future.successful(()))

    val result = controller.requestDetailedValuation(link.submissionId, 1L, false)(request)

    status(result) mustBe SEE_OTHER
    redirectLocation(result) mustBe Some(
      s"/business-rates-property-linking/my-organisation/property-link/clients/all/${link.submissionId}/confirmation?submissionId=DVR123")
  }

  "request detailed valuation confirmation as Agent" must "return 200 OK when request is valid" in new Setup {
    when(mockPropertyLinkConnector.getClientAssessments(any())(any()))
      .thenReturn(Future.successful(Some(assessments)))

    val result = controller.confirmation(link.submissionId, link.submissionId, false)(request)

    status(result) mustBe OK
  }

  "already submitted detailed valuation request" must "return 200 OK when DVR does not exist" in new Setup {
    when(mockPropertyLinkConnector.getOwnerAssessments(any())(any()))
      .thenReturn(Future.successful(Some(assessments.copy(assessments = assessments.assessments.map(a =>
        a.copy(assessmentRef = 1L))))))
    when(mockDvrCaseManagement.dvrExists(any(), any())(any())).thenReturn(Future.successful(false))

    val result =
      controller.alreadySubmittedDetailedValuationRequest(submissionId = "11111", valuationId = 1L, owner = true)(
        request)

    status(result) mustBe OK
    contentAsString(result) must include("Already submitted a check?")
  }

  "already submitted detailed valuation request" must "return 200 OK without challenge section when viewing a draft assessment" in new Setup {
    when(mockPropertyLinkConnector.getOwnerAssessments(any())(any()))
      .thenReturn(Future.successful(Some(assessments.copy(assessments = assessments.assessments.map(a =>
        a.copy(assessmentRef = 1L, listType = ListType.DRAFT))))))
    when(mockDvrCaseManagement.dvrExists(any(), any())(any())).thenReturn(Future.successful(false))

    val result =
      controller.alreadySubmittedDetailedValuationRequest(submissionId = "11111", valuationId = 1L, owner = true)(
        request)

    status(result) mustBe OK
    contentAsString(result) must not include "Already submitted a check?"
  }

  "already submitted detailed valuation request" must "return 200 OK with check text when viewing a non-draft assessment" in new Setup {
    when(mockPropertyLinkConnector.getOwnerAssessments(any())(any()))
      .thenReturn(Future.successful(Some(assessments.copy(assessments = assessments.assessments.map(a =>
        a.copy(assessmentRef = 1L, listType = ListType.CURRENT))))))
    when(mockDvrCaseManagement.dvrExists(any(), any())(any())).thenReturn(Future.successful(true))

    val result =
      controller.alreadySubmittedDetailedValuationRequest(submissionId = "11111", valuationId = 1L, owner = true)(
        request)

    status(result) mustBe OK
    contentAsString(result) must include("If you need to submit a check urgently because of a change")
  }

  "already submitted detailed valuation request" must "return 200 OK without check text when viewing a draft assessment" in new Setup {
    when(mockPropertyLinkConnector.getOwnerAssessments(any())(any()))
      .thenReturn(Future.successful(Some(assessments.copy(assessments = assessments.assessments.map(a =>
        a.copy(assessmentRef = 1L, listType = ListType.DRAFT))))))
    when(mockDvrCaseManagement.dvrExists(any(), any())(any())).thenReturn(Future.successful(true))

    val result =
      controller.alreadySubmittedDetailedValuationRequest(submissionId = "11111", valuationId = 1L, owner = true)(
        request)

    status(result) mustBe OK
    contentAsString(result) must not include "If you need to submit a check urgently because of a change"
  }

}
