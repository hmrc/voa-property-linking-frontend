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

package controllers.detailedvaluationrequest

import connectors.SubmissionIdConnector
import controllers.VoaPropertyLinkingSpec
import models._
import models.challenge.ChallengeCaseStatus
import models.dvr.cases.check.CheckCaseStatus
import models.dvr.documents.{Document, DocumentSummary, DvrDocumentFiles}
import org.mockito.ArgumentMatchers.{any, eq => matching}
import org.mockito.Mockito.{never, verify, when}
import org.scalacheck.Arbitrary.arbitrary
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils._

import scala.collection.JavaConverters._
import java.time.LocalDateTime
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.scalatest.Inspectors
import org.scalatest.Inspectors.forAll

import scala.collection.mutable
import scala.concurrent.Future

class DvrControllerSpec extends VoaPropertyLinkingSpec {

  trait Setup {
    implicit val request = FakeRequest()

    val controller = new DvrController(
      errorHandler = mockCustomErrorHandler,
      propertyLinks = mockPropertyLinkConnector,
      challengeConnector = mockChallengeConnector,
      vmvConnector = mockVmvConnector,
      withLinkingSession = mockWithLinkingSession,
      authenticated = preAuthenticatedActionBuilders(),
      submissionIds = mockSubmissionIds,
      dvrCaseManagement = mockDvrCaseManagement,
      alreadyRequestedDetailedValuationView = alreadyRequestedDetailedValuationView,
      requestDetailedValuationView = requestDetailedValuationView,
      requestedDetailedValuationView = requestedDetailedValuationView,
      dvrFilesView = dvrFilesView,
      cannotRaiseChallengeView = cannotRaiseChallengeView,
      propertyMissingView = propertyMissingView
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

    lazy val successfulDvrDocuments: Future[Some[DvrDocumentFiles]] = Future.successful(Some({
      val now = LocalDateTime.now()
      DvrDocumentFiles(
        checkForm = Document(DocumentSummary("1L", "Check Document", now)),
        detailedValuation = Document(DocumentSummary("2L", "Detailed Valuation Document", now))
      )
    }))

    StubPropertyLinkConnector.stubLink(link)

    def assessments: ApiAssessments = apiAssessments(ownerAuthorisation)
  }

  trait CanChallengeSetup extends Setup {

    val plSubmissionId = "123456"
    val assessmentRef = 55555
    val caseRef = "234234"
    val authorisationId = 4222211L
    val uarn = 123123
    val localAuthRef = "1234341234"

    val testPropertyHistory =
      propertyHistory.copy(uarn = uarn, localAuthorityReference = localAuthRef, addressFull = addressLine)

    when(mockVmvConnector.getPropertyHistory(any())(any()))
      .thenReturn(Future.successful(testPropertyHistory))

    def resultCanChallenge(isOwner: Boolean) =
      controller.canChallenge(
        plSubmissionId = plSubmissionId,
        assessmentRef = assessmentRef,
        caseRef = caseRef,
        authorisationId = authorisationId,
        uarn = uarn,
        isOwner = isOwner
      )(request)
  }

  "can Challenge for IP" should "redirect to challenge advice page when canChallenge return None" in new CanChallengeSetup {

    when(mockPropertyLinkConnector.canChallenge(any(), any(), any(), any())(any()))
      .thenReturn(Future.successful(None))

    val result = resultCanChallenge(true)

    val urlBack =
      s"http://localhost:9523/business-rates-property-linking/my-organisation/property-link/$plSubmissionId/valuations/$assessmentRef?uarn=$uarn"
    val expectedRedirect =
      s"http://localhost:9537/business-rates-valuation/property-link/valuations/startChallenge?backLinkUrl=$urlBack"

    status(result) shouldBe SEE_OTHER
    redirectLocation(result) shouldBe Some(expectedRedirect)
  }

  "can Challenge for Agent" should "redirect to challenge advice page when canChallenge return None" in new CanChallengeSetup {

    when(mockPropertyLinkConnector.canChallenge(any(), any(), any(), any())(any()))
      .thenReturn(Future.successful(None))

    val result = resultCanChallenge(false)

    val urlBack =
      s"http://localhost:9523/business-rates-property-linking/my-organisation/property-link/clients/all/$plSubmissionId/valuations/$assessmentRef?uarn=$uarn"
    val expectedRedirect =
      s"http://localhost:9537/business-rates-valuation/property-link/valuations/startChallenge?backLinkUrl=$urlBack"

    status(result) shouldBe SEE_OTHER
    redirectLocation(result) shouldBe Some(expectedRedirect)
  }

  "can Challenge for IP" should "redirect to start challenge when canChallenge response is true" in new CanChallengeSetup {

    val testCanChallengeResponse = canChallengeResponse.copy(result = true)

    when(mockPropertyLinkConnector.canChallenge(any(), any(), any(), any())(any()))
      .thenReturn(Future.successful(Some(testCanChallengeResponse)))

    val result = resultCanChallenge(true)

    val expectedRedirect =
      s"http://localhost:9531/business-rates-challenge/property-link/$plSubmissionId/valuation/$assessmentRef/check/$caseRef/party/client/start?isDvr=true"

    status(result) shouldBe SEE_OTHER
    redirectLocation(result) shouldBe Some(expectedRedirect)
  }

  "can Challenge for Agent" should "redirect to start challenge when canChallenge response is true" in new CanChallengeSetup {

    val testCanChallengeResponse = canChallengeResponse.copy(result = true)

    when(mockPropertyLinkConnector.canChallenge(any(), any(), any(), any())(any()))
      .thenReturn(Future.successful(Some(testCanChallengeResponse)))

    val result = resultCanChallenge(false)

    val expectedRedirect =
      s"http://localhost:9531/business-rates-challenge/property-link/$plSubmissionId/valuation/$assessmentRef/check/$caseRef/party/agent/start?isDvr=true"

    status(result) shouldBe SEE_OTHER
    redirectLocation(result) shouldBe Some(expectedRedirect)
  }

  "can Challenge for IP" should "redirect to start challenge when canChallenge response is false" in new CanChallengeSetup {

    val testCanChallengeResponse = canChallengeResponse.copy(result = false)

    when(mockPropertyLinkConnector.canChallenge(any(), any(), any(), any())(any()))
      .thenReturn(Future.successful(Some(testCanChallengeResponse)))

    val result = resultCanChallenge(true)

    status(result) shouldBe OK
    contentAsString(result) should include(
      "<title>You cannot challenge this valuation - Valuation Office Agency - GOV.UK</title>")
    // Backlink
    contentAsString(result) should include(
      """<a href="/business-rates-property-linking/my-organisation/property-link/123456/valuations/55555?uarn=123123""")
  }

  "can Challenge for Agent" should "redirect to start challenge when canChallenge response is false" in new CanChallengeSetup {

    val testCanChallengeResponse = canChallengeResponse.copy(result = false)

    when(mockPropertyLinkConnector.canChallenge(any(), any(), any(), any())(any()))
      .thenReturn(Future.successful(Some(testCanChallengeResponse)))

    val result = resultCanChallenge(false)

    status(result) shouldBe OK
    contentAsString(result) should include(
      "<title>You cannot challenge this valuation - Valuation Office Agency - GOV.UK</title>")
    // Backlink
    contentAsString(result) should include(
      """<a href="/business-rates-property-linking/my-organisation/property-link/clients/all/123456/valuations/55555?uarn=123123""")
  }

  "detailed valuation check" should "return 200 OK when DVR case does not exist" in new Setup {

    when(mockPropertyLinkConnector.getOwnerAssessments(any())(any()))
      .thenReturn(Future.successful(Some(assessments)))
    when(mockPropertyLinkConnector.getMyOrganisationsCheckCases(any())(any()))
      .thenReturn(Future.successful(List.empty))
    when(mockChallengeConnector.getMyOrganisationsChallengeCases(any())(any()))
      .thenReturn(Future.successful(List.empty))
    when(mockDvrCaseManagement.getDvrDocuments(any(), any(), any())(any())).thenReturn(successfulDvrDocuments)

    val result =
      controller.myOrganisationRequestDetailValuationCheck(
        propertyLinkSubmissionId = "1111",
        valuationId =
          assessments.assessments.headOption.fold(fail("expected to find at least 1 assessment"))(_.assessmentRef),
        uarn = 1L
      )(request)

    status(result) shouldBe OK
    val page = HtmlPage(Jsoup.parse(contentAsString(result)))

    contentAsString(result) should include("If you want to change something in this valuation")
    page.shouldContain("#valuation-tab", 1)
    page.shouldContain("#check-cases-tab", 1)
    page.shouldContain("#challenge-cases-tab", 0)

    page.verifyElementTextByAttribute("href", "#check-cases-tab", "Checks")

    verify(mockPropertyLinkConnector).getMyOrganisationsCheckCases(any())(any())
    verify(mockChallengeConnector).getMyOrganisationsChallengeCases(any())(any())
  }

  "detailed valuation check" should "show all 3 tabs when checks and challenges are available" in new Setup {

    when(mockPropertyLinkConnector.getOwnerAssessments(any())(any()))
      .thenReturn(Future.successful(Some(assessments)))
    when(mockPropertyLinkConnector.getMyOrganisationsCheckCases(any())(any()))
      .thenReturn(Future.successful(List(ownerCheckCaseDetails)))
    when(mockChallengeConnector.getMyOrganisationsChallengeCases(any())(any()))
      .thenReturn(Future.successful(List(ownerChallengeCaseDetails)))
    when(mockDvrCaseManagement.getDvrDocuments(any(), any(), any())(any())).thenReturn(successfulDvrDocuments)

    val result =
      controller.myOrganisationRequestDetailValuationCheck(
        propertyLinkSubmissionId = "1111",
        valuationId =
          assessments.assessments.headOption.fold(fail("expected to find at least 1 assessment"))(_.assessmentRef),
        uarn = 1L
      )(request)

    status(result) shouldBe OK

    val page = HtmlPage(Jsoup.parse(contentAsString(result)))

    page.shouldContainText("If you want to change something in this valuation")

    page.shouldContainText("Local authority reference: BAREF")
    page.titleShouldMatch("123, SOME ADDRESS - Valuation Office Agency - GOV.UK")
    page.shouldContain("#valuation-tab", 1)
    page.shouldContain("#check-cases-tab", 1)
    page.shouldContain("#challenge-cases-tab", 1)

    page.verifyElementTextByAttribute("href", "#check-cases-tab", "Checks (1)")
    page.verifyElementTextByAttribute("href", "#challenge-cases-tab", "Challenges (1)")

    page.verifyElementTextByAttribute(
      "href",
      "/business-rates-property-linking/my-organisation/property-link/1111/valuations/1234/file/2L",
      "Download the detailed valuation for this property"
    )
    page.verifyElementTextByAttribute(
      "href",
      "/business-rates-property-linking/my-organisation/property-link/1111/valuations/1234/file/1L",
      "download and complete a Check form")

    verify(mockPropertyLinkConnector).getMyOrganisationsCheckCases(any())(any())
    verify(mockChallengeConnector).getMyOrganisationsChallengeCases(any())(any())
  }

  "detailed valuation of a client's DVR property" should "display the client's name above the address" in new Setup {
    when(mockPropertyLinkConnector.getClientAssessments(any())(any()))
      .thenReturn(Future.successful(Some(assessments.copy(clientOrgName = Some("Client Org Name")))))
    when(mockPropertyLinkConnector.getMyClientsCheckCases(any())(any()))
      .thenReturn(Future.successful(List(agentCheckCaseDetails)))
    when(mockChallengeConnector.getMyClientsChallengeCases(any())(any()))
      .thenReturn(Future.successful(List.empty))
    when(mockDvrCaseManagement.getDvrDocuments(any(), any(), any())(any())).thenReturn(successfulDvrDocuments)

    val result =
      controller.myClientsRequestDetailValuationCheck(
        propertyLinkSubmissionId = "1111",
        valuationId =
          assessments.assessments.headOption.fold(fail("expected to find at least 1 assessment"))(_.assessmentRef),
        uarn = 1L
      )(request)

    status(result) shouldBe OK

    val resString = contentAsString(result)
    val page = HtmlPage(Jsoup.parse(resString))
    page.html.getElementById("client-name").text() shouldBe "Client property: Client Org Name"
  }

  "detailed valuation of a DVR property" should "display correct CHECKS tab and table of check cases" in new Setup {
    when(mockPropertyLinkConnector.getOwnerAssessments(any())(any()))
      .thenReturn(Future.successful(Some(assessments)))
    when(mockPropertyLinkConnector.getMyOrganisationsCheckCases(any())(any()))
      .thenReturn(Future.successful(CheckCaseStatus.values.toList.map(status =>
        ownerCheckCaseDetails.copy(status = status.toString))))
    when(mockChallengeConnector.getMyOrganisationsChallengeCases(any())(any()))
      .thenReturn(Future.successful(List.empty))
    when(mockDvrCaseManagement.getDvrDocuments(any(), any(), any())(any())).thenReturn(successfulDvrDocuments)

    val result =
      controller.myOrganisationRequestDetailValuationCheck(
        propertyLinkSubmissionId = "1111",
        valuationId =
          assessments.assessments.headOption.fold(fail("expected to find at least 1 assessment"))(_.assessmentRef),
        uarn = 1L
      )(request)

    status(result) shouldBe OK

    val page = HtmlPage(Jsoup.parse(contentAsString(result)))
    val checksTable: Element = page.html.getElementById("checkcases-table")
    val headings: List[String] = checksTable.select("th").eachText().asScala.toList

    headings should contain theSameElementsInOrderAs List(
      "Check reference",
      "Submitted date",
      "Status",
      "Closed date",
      "Submitted by",
      "Action")

    val statuses = page.html.select("#checkcases-table .govuk-tag").asScala.toList
    val statusIcons: List[Element] = page.html.select("#checkcases-table .govuk-tag + a").asScala.toList
    val invisibleSpansText = statusIcons.flatMap(icon => icon.select("span").eachText().asScala).sorted
    invisibleSpansText should contain theSameElementsInOrderAs List(
      "Help with status: ASSIGNED",
      "Help with status: CANCELLED",
      "Help with status: CLOSED",
      "Help with status: DECISION SENT",
      "Help with status: OPEN",
      "Help with status: PENDING",
      "Help with status: RECEIVED",
      "Help with status: UNDER REVIEW"
    )

    val statusColours: Seq[(String, String)] = statuses.map(e => (e.text(), e.attr("class")))
    Inspectors.forAll(statusColours) {
      case (status, cssClasses) if Set("CANCELLED", "CLOSED", "DECISION SENT").contains(status) =>
        cssClasses should include("govuk-tag--grey")
      case (_, cssClasses) =>
        cssClasses should not include "govuk-tag--grey"
    }
  }

  "detailed valuation of a DVR property" should "display correct CHALLENGES tab and table of challenge cases" in new Setup {
    when(mockPropertyLinkConnector.getOwnerAssessments(any())(any()))
      .thenReturn(Future.successful(Some(assessments)))
    when(mockPropertyLinkConnector.getMyOrganisationsCheckCases(any())(any()))
      .thenReturn(Future.successful(List.empty))
    when(mockChallengeConnector.getMyOrganisationsChallengeCases(any())(any()))
      .thenReturn(Future.successful(ChallengeCaseStatus.values.toList.map(status =>
        ownerChallengeCaseDetails.copy(status = status.toString))))
    when(mockDvrCaseManagement.getDvrDocuments(any(), any(), any())(any())).thenReturn(successfulDvrDocuments)

    val result =
      controller.myOrganisationRequestDetailValuationCheck(
        propertyLinkSubmissionId = "1111",
        valuationId =
          assessments.assessments.headOption.fold(fail("expected to find at least 1 assessment"))(_.assessmentRef),
        uarn = 1L
      )(request)

    status(result) shouldBe OK

    val page = HtmlPage(Jsoup.parse(contentAsString(result)))
    val checksTable: Element = page.html.getElementById("challengecases-table")
    val headings: List[String] = checksTable.select("th").eachText().asScala.toList

    headings should contain theSameElementsInOrderAs List(
      "Challenge reference",
      "Submitted date",
      "Status",
      "Closed date",
      "Submitted by"
    )

    val statuses = page.html.select("#checkcases-table .govuk-tag").asScala.toList
    val statusIcons: List[Element] = page.html.select("#challengecases-table .govuk-tag + a").asScala.toList
    val invisibleSpansText = statusIcons.flatMap(icon => icon.select("span").eachText().asScala).sorted

    invisibleSpansText should contain theSameElementsInOrderAs List(
      "Help with status: ASSIGNED",
      "Help with status: CANCELLED",
      "Help with status: CLOSED",
      "Help with status: DECISION SENT",
      "Help with status: INITIAL RESPONSE",
      "Help with status: MORE INFO NEEDED",
      "Help with status: OPEN",
      "Help with status: PENDING",
      "Help with status: RECEIVED",
      "Help with status: UNDER REVIEW"
    )

    val statusColours: Seq[(String, String)] = statuses.map(e => (e.text(), e.attr("class")))
    Inspectors.forAll(statusColours) {
      case (status, cssClasses) if Set("CANCELLED", "CLOSED", "DECISION SENT").contains(status) =>
        cssClasses should include("govuk-tag--grey")
      case (_, cssClasses) =>
        cssClasses should not include "govuk-tag--grey"
    }
  }

  "draft detailed valuation" should "return 200 OK and not fetch checks/challenges " in new Setup {
    val firstAssessment: ApiAssessment =
      assessments.assessments.headOption.getOrElse(fail("expected to find at least 1 assessment"))
    val draftAssessment: ApiAssessment = firstAssessment.copy(listType = ListType.DRAFT)

    val ownerAssessments: ApiAssessments =
      assessments.copy(assessments = draftAssessment :: assessments.assessments.tail.toList)

    when(mockPropertyLinkConnector.getOwnerAssessments(any())(any()))
      .thenReturn(Future.successful(Some(ownerAssessments)))
    when(mockDvrCaseManagement.getDvrDocuments(any(), any(), any())(any())).thenReturn(successfulDvrDocuments)

    val result =
      controller.myOrganisationRequestDetailValuationCheck(
        propertyLinkSubmissionId = "1111",
        valuationId = firstAssessment.assessmentRef,
        uarn = 1L
      )(request)

    status(result) shouldBe OK
    val page = HtmlPage(Jsoup.parse(contentAsString(result)))
    page.shouldContain("#valuation-tab", 1)
    page.shouldContain("#check-cases-tab", 0)
    page.shouldContain("#challenge-cases-tab", 0)
    page.shouldContainText("If you want to change something in this valuation")

    verify(mockPropertyLinkConnector, never()).getMyOrganisationsCheckCases(any())(any())
    verify(mockChallengeConnector, never()).getMyOrganisationsChallengeCases(any())(any())
  }

  "detailed valuation check" should "return 303 SEE_OTHER when DVR case does exist" in new Setup {
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

    status(result) shouldBe SEE_OTHER
    redirectLocation(result) shouldBe Some(
      s"/business-rates-property-linking/my-organisation/property-link/1111/valuations/${link.authorisationId}/exists")
  }

  "request detailed valuation" should "return 303 SEE_OTHER when request is valid" in new Setup {
    when(mockPropertyLinkConnector.getOwnerAssessments(any())(any()))
      .thenReturn(Future.successful(Some(assessments)))
    when(mockDvrCaseManagement.requestDetailedValuationV2(any())(any())).thenReturn(Future.successful(()))

    val result = controller.requestDetailedValuation(link.submissionId, 1L, true)(request)

    status(result) shouldBe SEE_OTHER
    redirectLocation(result) shouldBe Some(
      s"/business-rates-property-linking/my-organisation/property-link/${link.submissionId}/confirmation?submissionId=DVR123")
  }

  "request detailed valuation confirmation" should "return 200 OK when request is valid" in new Setup {
    when(mockPropertyLinkConnector.getOwnerAssessments(any())(any()))
      .thenReturn(Future.successful(Some(assessments)))

    val result = controller.confirmation(link.submissionId, link.submissionId, true)(request)

    status(result) shouldBe OK
  }

  "request detailed valuation as Agent" should "return 303 SEE_OTHER when request is valid" in new Setup {
    when(mockPropertyLinkConnector.getClientAssessments(any())(any()))
      .thenReturn(Future.successful(Some(assessments)))
    when(mockDvrCaseManagement.requestDetailedValuationV2(any())(any())).thenReturn(Future.successful(()))

    val result = controller.requestDetailedValuation(link.submissionId, 1L, false)(request)

    status(result) shouldBe SEE_OTHER
    redirectLocation(result) shouldBe Some(
      s"/business-rates-property-linking/my-organisation/property-link/clients/all/${link.submissionId}/confirmation?submissionId=DVR123")
  }

  "request detailed valuation confirmation as Agent" should "return 200 OK when request is valid" in new Setup {
    when(mockPropertyLinkConnector.getClientAssessments(any())(any()))
      .thenReturn(Future.successful(Some(assessments)))

    val result = controller.confirmation(link.submissionId, link.submissionId, false)(request)

    status(result) shouldBe OK
  }

  "already submitted detailed valuation request" should "return 200 OK when DVR does not exist" in new Setup {
    when(mockPropertyLinkConnector.getOwnerAssessments(any())(any()))
      .thenReturn(Future.successful(Some(assessments.copy(assessments = assessments.assessments.map(a =>
        a.copy(assessmentRef = 1L))))))
    when(mockDvrCaseManagement.dvrExists(any(), any())(any())).thenReturn(Future.successful(false))

    val result =
      controller.alreadySubmittedDetailedValuationRequest(submissionId = "11111", valuationId = 1L, owner = true)(
        request)

    status(result) shouldBe OK
    contentAsString(result) should include("Already submitted a check?")
  }

  "already submitted detailed valuation request" should "return 200 OK without challenge section when viewing a draft assessment" in new Setup {
    when(mockPropertyLinkConnector.getOwnerAssessments(any())(any()))
      .thenReturn(Future.successful(Some(assessments.copy(assessments = assessments.assessments.map(a =>
        a.copy(assessmentRef = 1L, listType = ListType.DRAFT))))))
    when(mockDvrCaseManagement.dvrExists(any(), any())(any())).thenReturn(Future.successful(false))

    val result =
      controller.alreadySubmittedDetailedValuationRequest(submissionId = "11111", valuationId = 1L, owner = true)(
        request)

    status(result) shouldBe OK
    contentAsString(result) should not include "Already submitted a check?"
  }

  "already submitted detailed valuation request" should "return 200 OK with check text when viewing a non-draft assessment" in new Setup {
    when(mockPropertyLinkConnector.getOwnerAssessments(any())(any()))
      .thenReturn(Future.successful(Some(assessments.copy(assessments = assessments.assessments.map(a =>
        a.copy(assessmentRef = 1L, listType = ListType.CURRENT))))))
    when(mockDvrCaseManagement.dvrExists(any(), any())(any())).thenReturn(Future.successful(true))

    val result =
      controller.alreadySubmittedDetailedValuationRequest(submissionId = "11111", valuationId = 1L, owner = true)(
        request)

    status(result) shouldBe OK
    contentAsString(result) should include("If you need to submit a check urgently because of a change")
  }

  "already submitted detailed valuation request" should "return 200 OK without check text when viewing a draft assessment" in new Setup {
    when(mockPropertyLinkConnector.getOwnerAssessments(any())(any()))
      .thenReturn(Future.successful(Some(assessments.copy(assessments = assessments.assessments.map(a =>
        a.copy(assessmentRef = 1L, listType = ListType.DRAFT))))))
    when(mockDvrCaseManagement.dvrExists(any(), any())(any())).thenReturn(Future.successful(true))

    val result =
      controller.alreadySubmittedDetailedValuationRequest(submissionId = "11111", valuationId = 1L, owner = true)(
        request)

    status(result) shouldBe OK
    contentAsString(result) should not include "If you need to submit a check urgently because of a change"
  }

}
