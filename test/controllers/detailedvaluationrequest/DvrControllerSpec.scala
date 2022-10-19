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

import _root_.utils.Formatters
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
import java.time.{LocalDate, LocalDateTime}

import models.dvr.cases.check.common.Agent
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.scalatest.Inspectors
import play.api.mvc.Result

import scala.concurrent.Future

class DvrControllerSpec extends VoaPropertyLinkingSpec {

  trait Setup {
    implicit val request = FakeRequest()
    val enquiryUrlTemplate = "/draft-list-enquiry/start-from-dvr-valuation"
    val estimatorUrlTemplate = "/estimate-your-business-rates/start-from-dvr-valuation"

    val controller = new DvrController(
      errorHandler = mockCustomErrorHandler,
      propertyLinks = mockPropertyLinkConnector,
      challengeConnector = mockChallengeConnector,
      vmvConnector = mockVmvConnector,
      authenticated = preAuthenticatedActionBuilders(),
      submissionIds = mockSubmissionIds,
      dvrCaseManagement = mockDvrCaseManagement,
      alreadyRequestedDetailedValuationView = alreadyRequestedDetailedValuationView,
      requestDetailedValuationView = requestDetailedValuationView,
      requestedDetailedValuationView = requestedDetailedValuationView,
      dvrFilesView = dvrFilesView,
      cannotRaiseChallengeView = cannotRaiseChallengeView,
      propertyMissingView = propertyMissingView,
      enquiryUrlTemplate = enquiryUrlTemplate,
      estimatorUrlTemplate = estimatorUrlTemplate
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

    def clientPropertyLinks: ClientPropertyLink = clientProperties(clientPropertyLink)
  }

  trait CanChallengeSetup extends Setup {

    val plSubmissionId = "123456"
    val assessmentRef = 55555
    val caseRef = "234234"
    val authorisationId = 4222211L
    val uarn = 123123
    val localAuthRef = "1234341234"
    val listYear = "2017"
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
        isOwner = isOwner,
        listYear = listYear
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
      s"http://localhost:9531/business-rates-challenge/property-link/$plSubmissionId/valuation/$assessmentRef/check/$caseRef/party/client/start?isDvr=true&valuationListYear=2017"

    status(result) shouldBe SEE_OTHER
    redirectLocation(result) shouldBe Some(expectedRedirect)
  }

  "can Challenge for Agent" should "redirect to start challenge when canChallenge response is true" in new CanChallengeSetup {

    val testCanChallengeResponse = canChallengeResponse.copy(result = true)

    when(mockPropertyLinkConnector.canChallenge(any(), any(), any(), any())(any()))
      .thenReturn(Future.successful(Some(testCanChallengeResponse)))

    val result = resultCanChallenge(false)

    val expectedRedirect =
      s"http://localhost:9531/business-rates-challenge/property-link/$plSubmissionId/valuation/$assessmentRef/check/$caseRef/party/agent/start?isDvr=true&valuationListYear=2017"

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

  "current valuation" should "return 200 OK and have the correct caption" in new Setup {

    val ownerAssessments = assessments.copy(assessments = Seq(apiAssessment(ownerAuthorisation)))

    when(mockPropertyLinkConnector.getOwnerAssessments(any())(any()))
      .thenReturn(Future.successful(Some(ownerAssessments)))
    when(mockPropertyLinkConnector.getMyOrganisationsCheckCases(any())(any()))
      .thenReturn(Future.successful(List(ownerCheckCaseDetails)))
    when(mockChallengeConnector.getMyOrganisationsChallengeCases(any())(any()))
      .thenReturn(Future.successful(List(ownerChallengeCaseDetails)))
    when(mockDvrCaseManagement.getDvrDocuments(any(), any(), any())(any())).thenReturn(successfulDvrDocuments)

    val result =
      controller.myOrganisationRequestDetailValuationCheck(
        propertyLinkSubmissionId = "1111",
        valuationId =
          ownerAssessments.assessments.headOption.fold(fail("expected to find at least 1 assessment"))(_.assessmentRef),
        uarn = 1L
      )(request)

    status(result) shouldBe OK

    val doc = Jsoup.parse(contentAsString(result))

    val page = HtmlPage(doc)

    page.html.getElementById("rateable-value-caption").text().startsWith("Current rateable value") shouldBe true
  }

  "previous valuation" should "return 200 OK and have the correct caption" in new Setup {

    val ownerAssessments = assessments.copy(
      assessments = Seq(apiAssessment(ownerAuthorisation)
        .copy(listType = ListType.PREVIOUS, currentToDate = Some(LocalDate.now().minusDays(1)))))

    when(mockPropertyLinkConnector.getOwnerAssessments(any())(any()))
      .thenReturn(Future.successful(Some(ownerAssessments)))
    when(mockPropertyLinkConnector.getMyOrganisationsCheckCases(any())(any()))
      .thenReturn(Future.successful(List(ownerCheckCaseDetails)))
    when(mockChallengeConnector.getMyOrganisationsChallengeCases(any())(any()))
      .thenReturn(Future.successful(List(ownerChallengeCaseDetails)))
    when(mockDvrCaseManagement.getDvrDocuments(any(), any(), any())(any())).thenReturn(successfulDvrDocuments)

    val result =
      controller.myOrganisationRequestDetailValuationCheck(
        propertyLinkSubmissionId = "1111",
        valuationId =
          ownerAssessments.assessments.headOption.fold(fail("expected to find at least 1 assessment"))(_.assessmentRef),
        uarn = 1L
      )(request)

    status(result) shouldBe OK

    val doc = Jsoup.parse(contentAsString(result))

    val page = HtmlPage(doc)

    page.html.getElementById("rateable-value-caption").text().startsWith("Previous rateable value") shouldBe true
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
    page.shouldContain("#start-check-tab", 1)
    page.shouldContain("#challenge-cases-tab", 0)

    page.verifyElementTextByAttribute("href", "#check-cases-tab", "Checks")

    verify(mockPropertyLinkConnector).getMyOrganisationsCheckCases(any())(any())
    verify(mockChallengeConnector).getMyOrganisationsChallengeCases(any())(any())
  }

  "detailed valuation check" should "show all 4 tabs when checks and challenges are available" in new Setup {

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

    val doc = Jsoup.parse(contentAsString(result))

    val page = HtmlPage(doc)

    page.shouldContainText("If you want to change something in this valuation")

    page.shouldContainText("Local authority reference: BAREF")
    page.titleShouldMatch("123, SOME ADDRESS - Valuation Office Agency - GOV.UK")
    page.shouldContain("#valuation-tab", 1)
    page.shouldContain("#check-cases-tab", 1)
    page.shouldContain("#start-check-tab", 1)
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

    val radiosOnStartCheckScreen: Elements = doc.select("#checkType-form input[type=radio]")
    radiosOnStartCheckScreen should have size 7

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

    page.html
      .getElementById("back-link")
      .attr("href") shouldBe "/business-rates-property-linking/property-link/1111/assessments?owner=false"
  }

  "detailed valuation of a client's DVR property" should "have a back link to challenge case details when challenge case ref is provided" in new Setup {

    when(mockPropertyLinkConnector.getClientAssessments(any())(any()))
      .thenReturn(Future.successful(Some(assessments.copy(clientOrgName = Some("Client Org Name")))))
    when(mockPropertyLinkConnector.getMyClientsCheckCases(any())(any()))
      .thenReturn(Future.successful(List(agentCheckCaseDetails)))
    when(mockChallengeConnector.getMyClientsChallengeCases(any())(any()))
      .thenReturn(Future.successful(List.empty))
    when(mockDvrCaseManagement.getDvrDocuments(any(), any(), any())(any())).thenReturn(successfulDvrDocuments)

    private val challengeRef = "CHG345678"
    private val plSubmissionId = "1111"
    private val valuationId: Long =
      assessments.assessments.headOption.fold(fail("expected to find at least 1 assessment"))(_.assessmentRef)
    val result =
      controller.myClientsRequestDetailValuationCheck(
        propertyLinkSubmissionId = plSubmissionId,
        valuationId = valuationId,
        uarn = 1L,
        challengeCaseRef = Some(challengeRef)
      )(request)

    status(result) shouldBe OK

    val resString = contentAsString(result)
    val page = HtmlPage(Jsoup.parse(resString))
    page.html.getElementById("client-name").text() shouldBe "Client property: Client Org Name"
    page.html
      .getElementById("back-link")
      .attr("href") shouldBe s"http://localhost:9531/business-rates-challenge/summary/property-link/${ownerAuthorisation.authorisationId}/submission-id/$plSubmissionId/challenge-cases/$challengeRef?isAgent=true&isDvr=true&valuationId=$valuationId"
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

  "draft detailed valuation" should "return 200 OK and display agent tab when no agents assigned" in new Setup {
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

    page.html.getElementById("rateable-value-caption").text().startsWith("Future rateable value") shouldBe true
    page.shouldContain("#agents-tab", 1)

    private val tabs: Element = page.html.getElementsByClass("govuk-tabs__list").first()

    tabs.getElementsByTag("li").size() shouldBe 2
    tabs.getElementsByAttributeValue("href", "#agents-tab").text shouldBe "Agents"

    page.html.getElementById("agents-tab-heading").text() shouldBe "Agents assigned to this property"
    page.html.getElementById("no-agents-text").text() shouldBe "There are no agents assigned to this property."
    page.html.getElementById("assign-agent-link").text() shouldBe "Assign an agent to this property"
    page.html.getElementById("help-appointing-agent-link").text() shouldBe "Help with appointing an agent"
  }

  "draft detailed valuation" should "return 200 OK and display agent tab when agents assigned" in new Setup {
    val agent = assessments.agents.head

    when(mockPropertyLinkConnector.getOwnerAssessments(any())(any()))
      .thenReturn(Future.successful(Some(assessments)))

    when(mockPropertyLinkConnector.getMyOrganisationsCheckCases(any())(any()))
      .thenReturn(Future.successful(CheckCaseStatus.values.toList.map(status =>
        ownerCheckCaseDetails.copy(status = status.toString, agent = Some(Agent(agent))))))

    when(mockChallengeConnector.getMyOrganisationsChallengeCases(any())(any()))
      .thenReturn(Future.successful(ChallengeCaseStatus.values.toList.map(status =>
        ownerChallengeCaseDetails.copy(status = status.toString, agent = Some(Agent(agent))))))

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

    //verify all tabs displayed
    page.shouldContain("#valuation-tab", 1)
    page.shouldContain("#start-check-tab", 1)
    page.shouldContain("#check-cases-tab", 1)
    page.shouldContain("#challenge-cases-tab", 1)
    page.shouldContain("#agents-tab", 1)

    //verify back link
    page.html
      .getElementById("back-link")
      .attr("href") shouldBe "/business-rates-property-linking/property-link/1111/assessments"

    private val tabs: Element = page.html.getElementsByClass("govuk-tabs__list").first()

    tabs.getElementsByTag("li").size() shouldBe 5
    tabs.getElementsByAttributeValue("href", "#agents-tab").text shouldBe "Agents (1)"

    page.html.getElementById("agents-tab-heading").text() shouldBe "Agents assigned to this property"
    page.html.getElementById("assign-agent-link").text() shouldBe "Assign an agent to this property"
    page.html.getElementById("help-appointing-agent-link").text() shouldBe "Help with appointing an agent"

    val agentsTable = page.html.getElementById("agentCounts-table")

    agentsTable.getElementById("agent-name-1").text.contains(agent.organisationName) shouldBe true
    agentsTable.getElementById("open-cases-1").text shouldBe "12"
    agentsTable.getElementById("total-cases-1").text shouldBe "18"
  }

  "draft detailed valuation" should "return 200 OK and have correct  back link when challenge case is provided" in new Setup {
    val agent = assessments.agents.head

    when(mockPropertyLinkConnector.getOwnerAssessments(any())(any()))
      .thenReturn(Future.successful(Some(assessments)))

    when(mockPropertyLinkConnector.getMyOrganisationsCheckCases(any())(any()))
      .thenReturn(Future.successful(CheckCaseStatus.values.toList.map(status =>
        ownerCheckCaseDetails.copy(status = status.toString, agent = Some(Agent(agent))))))

    when(mockChallengeConnector.getMyOrganisationsChallengeCases(any())(any()))
      .thenReturn(Future.successful(ChallengeCaseStatus.values.toList.map(status =>
        ownerChallengeCaseDetails.copy(status = status.toString, agent = Some(Agent(agent))))))

    when(mockDvrCaseManagement.getDvrDocuments(any(), any(), any())(any())).thenReturn(successfulDvrDocuments)

    private val challengeRef = "CHG53626"
    private val plSubmissionId = "1111"
    private val valuationId: Long =
      assessments.assessments.headOption.fold(fail("expected to find at least 1 assessment"))(_.assessmentRef)
    val result =
      controller.myOrganisationRequestDetailValuationCheck(
        propertyLinkSubmissionId = plSubmissionId,
        valuationId = valuationId,
        uarn = 1L,
        challengeCaseRef = Some(challengeRef)
      )(request)

    status(result) shouldBe OK
    val page = HtmlPage(Jsoup.parse(contentAsString(result)))

    //verify all tabs displayed
    page.shouldContain("#valuation-tab", 1)
    page.shouldContain("#start-check-tab", 1)
    page.shouldContain("#check-cases-tab", 1)
    page.shouldContain("#challenge-cases-tab", 1)
    page.shouldContain("#agents-tab", 1)

    //verify back link
    page.html
      .getElementById("back-link")
      .attr("href") shouldBe s"http://localhost:9531/business-rates-challenge/summary/property-link/${ownerAuthorisation.authorisationId}/submission-id/$plSubmissionId/challenge-cases/$challengeRef?isAgent=false&isDvr=true&valuationId=$valuationId"
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
    page.shouldContain("#agents-tab", 1)
    page.shouldContainText("If you want to change something in this valuation")

    verify(mockPropertyLinkConnector, never()).getMyOrganisationsCheckCases(any())(any())
    verify(mockChallengeConnector, never()).getMyOrganisationsChallengeCases(any())(any())
  }

  trait FutureRequestSetup extends Setup {

    def testController(estimatorUrl: String) = new DvrController(
      errorHandler = mockCustomErrorHandler,
      propertyLinks = mockPropertyLinkConnector,
      challengeConnector = mockChallengeConnector,
      vmvConnector = mockVmvConnector,
      authenticated = preAuthenticatedActionBuilders(),
      submissionIds = mockSubmissionIds,
      dvrCaseManagement = mockDvrCaseManagement,
      alreadyRequestedDetailedValuationView = alreadyRequestedDetailedValuationView,
      requestDetailedValuationView = requestDetailedValuationView,
      requestedDetailedValuationView = requestedDetailedValuationView,
      dvrFilesView = dvrFilesView,
      cannotRaiseChallengeView = cannotRaiseChallengeView,
      propertyMissingView = propertyMissingView,
      enquiryUrlTemplate = enquiryUrlTemplate,
      estimatorUrlTemplate = estimatorUrl
    )

    lazy val currentAssessment: ApiAssessment = super.assessments.assessments.drop(1).head
    lazy val futureAssessment: ApiAssessment = {
      currentAssessment.copy(listType = ListType.DRAFT, effectiveDate = Some(april2023))
    }
    lazy override val assessments: ApiAssessments =
      super.assessments.copy(assessments = futureAssessment :: super.assessments.assessments.tail.toList)

    lazy val futureEffectiveDate: String = Formatters.formatDate(futureAssessment.effectiveDate.get)
    lazy val ipCurrentValuationUrl: String = routes.DvrController
      .myOrganisationRequestDetailValuationCheck(
        assessments.submissionId,
        currentAssessment.assessmentRef,
        currentAssessment.uarn,
        challengeCaseRef = None)
      .url
    lazy val clientCurrentValuationUrl: String = routes.DvrController
      .myClientsRequestDetailValuationCheck(
        assessments.submissionId,
        currentAssessment.assessmentRef,
        currentAssessment.uarn,
        challengeCaseRef = None)
      .url
    lazy val ipFutureValuationUrl: String = routes.DvrController
      .myOrganisationRequestDetailValuationCheck(
        assessments.submissionId,
        futureAssessment.assessmentRef,
        futureAssessment.uarn,
        challengeCaseRef = None)
      .url
    lazy val clientFutureValuationUrl: String = routes.DvrController
      .myClientsRequestDetailValuationCheck(
        assessments.submissionId,
        futureAssessment.assessmentRef,
        futureAssessment.uarn,
        challengeCaseRef = None)
      .url

    def estimatorUrl(isOwner: Boolean): String =
      raw"""/estimate-your-business-rates/start-from-dvr-valuation?
           |authorisationId=${futureAssessment.authorisationId}&
           |isOwner=$isOwner&
           |propertyLinkSubmissionId=${assessments.submissionId}&
           |valuationId=${futureAssessment.assessmentRef}""".stripMargin.replaceAll("\n", "")

    def enquiryUrl(isOwner: Boolean): String =
      raw"""/draft-list-enquiry/start-from-dvr-valuation?
           |authorisationId=${futureAssessment.authorisationId}&
           |isOwner=$isOwner&
           |propertyLinkSubmissionId=${assessments.submissionId}&
           |valuationId=${futureAssessment.assessmentRef}""".stripMargin.replaceAll("\n", "")

    lazy val currentListYear = "2017"

    when(mockPropertyLinkConnector.getOwnerAssessments(any())(any()))
      .thenReturn(Future.successful(Some(assessments)))
    when(mockPropertyLinkConnector.getClientAssessments(any())(any()))
      .thenReturn(Future.successful(Some(assessments)))
    when(mockDvrCaseManagement.dvrExists(any(), any())(any())).thenReturn(Future.successful(false))
    when(mockDvrCaseManagement.getDvrDocuments(any(), any(), any())(any())).thenReturn(successfulDvrDocuments)
  }

  "draft request valuation page for an IP" should "display the valuation tab's dates and links correctly" in new FutureRequestSetup {

    override val controller = testController(estimatorUrl(isOwner = true))

    val result: Future[Result] = controller.myOrganisationAlreadyRequestedDetailValuation(
      propertyLinkSubmissionId = assessments.submissionId,
      valuationId = futureAssessment.assessmentRef
    )(request)

    val page = HtmlPage(Jsoup.parse(contentAsString(result)))

    // request button should not exist
    page.shouldNotContainText("Request a valuation")

    page.html
      .getElementById("future-valuation-caption")
      .text shouldBe s"Future rateable value (from $futureEffectiveDate)"

    val estimatorLink: Element = page.html.getElementById("future-estimate-link")
    estimatorLink.text should include(futureEffectiveDate)
    estimatorLink.attr("href") shouldBe estimatorUrl(isOwner = true) + "&tabName=valuation-tab"

    page.html.getElementById("future-valuation-not-available").text should include(futureEffectiveDate)

    page.html.getElementById("request-current-valuation").text should include(currentListYear)
    page.html.getElementById("current-valuation-link").attr("href") shouldBe ipCurrentValuationUrl + "&fromFuture=true"
  }

  "draft request valuation page for an IP" should "display the future help tab's dates and links correctly" in new FutureRequestSetup {

    override val controller = testController(estimatorUrl(isOwner = true))

    val result: Future[Result] = controller.myOrganisationAlreadyRequestedDetailValuation(
      propertyLinkSubmissionId = assessments.submissionId,
      valuationId = futureAssessment.assessmentRef
    )(request)

    val page: HtmlPage = HtmlPage(Jsoup.parse(contentAsString(result)))

    page.html.getElementById("rateable-value-may-change-subhead").text should include(futureEffectiveDate)
    page.html.getElementById("rateable-value-may-change-content").text should include(futureEffectiveDate)

    page.html.getElementById("rateable-value-too-high-subhead").text shouldBe "You think the rateable value is too high"
    page.html.getElementById("rateable-value-too-high-content-1").text should include(futureEffectiveDate)
    page.html
      .getElementById("rateable-value-too-high-link")
      .attr("href") shouldBe ipCurrentValuationUrl + "&fromFuture=true&tabName=help-tab#start-check-tab"

    page.html.getElementById("property-details-changing-subhead").text shouldBe "Your property details need changing"
    page.html
      .getElementById("property-details-changing-content")
      .text shouldBe "Send us a Check case to tell us that your property details (such as floor area sizes and parking) need changing. We may accept your changes and update the current and future valuations."
    page.html
      .getElementById("property-details-changing-link")
      .attr("href") shouldBe ipCurrentValuationUrl + "&fromFuture=true&tabName=help-tab#start-check-tab"

    page.html.getElementById("other-question-subhead").text shouldBe "You have some other question about your valuation"
    page.html.getElementById("other-question-content").text should include(futureEffectiveDate)

    page.html
      .getElementById("covid-subhead")
      .text shouldBe "How Coronavirus (COVID-19) affected the future rateable value"
    val estimatorLink: Element = page.html.getElementById("help-estimator-link")
    estimatorLink.text should include(futureEffectiveDate)
    estimatorLink.attr("href") shouldBe estimatorUrl(isOwner = true) + "&tabName=help-tab"
  }

  "draft request valuation page for an agent" should "display the valuation tab's dates and links correctly" in new FutureRequestSetup {
    override val controller = testController(estimatorUrl(isOwner = false))

    val result: Future[Result] = controller.myClientsAlreadyRequestedDetailValuation(
      propertyLinkSubmissionId = assessments.submissionId,
      valuationId = futureAssessment.assessmentRef
    )(request)

    val page: HtmlPage = HtmlPage(Jsoup.parse(contentAsString(result)))

    // request button should not exist
    page.shouldNotContainText("Request a valuation")

    page.html.getElementById("rateable-value-may-change-subhead").text should include(futureEffectiveDate)
    page.html.getElementById("rateable-value-may-change-content").text should include(futureEffectiveDate)

    page.html.getElementById("rateable-value-too-high-subhead").text shouldBe "You think the rateable value is too high"
    page.html.getElementById("rateable-value-too-high-content-1").text should include(futureEffectiveDate)
    page.html
      .getElementById("rateable-value-too-high-link")
      .attr("href") shouldBe clientCurrentValuationUrl + "&fromFuture=true&tabName=help-tab#start-check-tab"

    page.html.getElementById("property-details-changing-subhead").text shouldBe "Your property details need changing"
    page.html
      .getElementById("property-details-changing-content")
      .text shouldBe "Send us a Check case to tell us that your property details (such as floor area sizes and parking) need changing. We may accept your changes and update the current and future valuations."
    page.html
      .getElementById("property-details-changing-link")
      .attr("href") shouldBe clientCurrentValuationUrl + "&fromFuture=true&tabName=help-tab#start-check-tab"

    page.html.getElementById("other-question-subhead").text shouldBe "You have some other question about your valuation"
    page.html.getElementById("other-question-content").text should include(futureEffectiveDate)

    page.html
      .getElementById("covid-subhead")
      .text shouldBe "How Coronavirus (COVID-19) affected the future rateable value"
    val estimatorLink: Element = page.html.getElementById("help-estimator-link")
    estimatorLink.text should include(futureEffectiveDate)
    estimatorLink.attr("href") shouldBe estimatorUrl(isOwner = false) + "&tabName=help-tab"
  }

  "draft request valuation page for an agent" should "display the future help tab's dates and links correctly" in new FutureRequestSetup {

    override val controller = testController(estimatorUrl(isOwner = false))

    val result: Future[Result] = controller.myClientsAlreadyRequestedDetailValuation(
      propertyLinkSubmissionId = assessments.submissionId,
      valuationId = futureAssessment.assessmentRef
    )(request)

    val page: HtmlPage = HtmlPage(Jsoup.parse(contentAsString(result)))

    page.html.getElementById("rateable-value-may-change-subhead").text should include(futureEffectiveDate)
    page.html.getElementById("rateable-value-may-change-content").text should include(futureEffectiveDate)

    page.html.getElementById("rateable-value-too-high-subhead").text shouldBe "You think the rateable value is too high"
    page.html.getElementById("rateable-value-too-high-content-1").text should include(futureEffectiveDate)
    page.html
      .getElementById("rateable-value-too-high-link")
      .attr("href") shouldBe clientCurrentValuationUrl + "&fromFuture=true&tabName=help-tab#start-check-tab"

    page.html.getElementById("property-details-changing-subhead").text shouldBe "Your property details need changing"
    page.html
      .getElementById("property-details-changing-content")
      .text shouldBe "Send us a Check case to tell us that your property details (such as floor area sizes and parking) need changing. We may accept your changes and update the current and future valuations."
    page.html
      .getElementById("property-details-changing-link")
      .attr("href") shouldBe clientCurrentValuationUrl + "&fromFuture=true&tabName=help-tab#start-check-tab"

    page.html.getElementById("other-question-subhead").text shouldBe "You have some other question about your valuation"
    page.html.getElementById("other-question-content").text should include(futureEffectiveDate)

    page.html
      .getElementById("covid-subhead")
      .text shouldBe "How Coronavirus (COVID-19) affected the future rateable value"
    val estimatorLink: Element = page.html.getElementById("help-estimator-link")
    estimatorLink.text should include(futureEffectiveDate)
    estimatorLink.attr("href") shouldBe estimatorUrl(isOwner = false) + "&tabName=help-tab"
  }

  "welsh draft request valuation page for an IP" should "display the future help tab's dates and links correctly" in new FutureRequestSetup {
    lazy override val futureAssessment: ApiAssessment = {
      currentAssessment
        .copy(listType = ListType.DRAFT, effectiveDate = Some(april2023), billingAuthorityCode = Some("6000"))
    }

    override val controller = testController(estimatorUrl(isOwner = true))

    val result: Future[Result] = controller.myOrganisationAlreadyRequestedDetailValuation(
      propertyLinkSubmissionId = assessments.submissionId,
      valuationId = futureAssessment.assessmentRef
    )(request)

    val page: HtmlPage = HtmlPage(Jsoup.parse(contentAsString(result)))

    page.html.getElementById("rateable-value-may-change-subhead").text should include(futureEffectiveDate)
    page.html.getElementById("rateable-value-may-change-content").text should include(futureEffectiveDate)

    page.html.getElementById("rateable-value-too-high-subhead").text shouldBe "You think the rateable value is too high"
    page.html.getElementById("rateable-value-too-high-welsh-content-1").text should include(futureEffectiveDate)

    page.html.getElementById("property-details-changing-subhead").text shouldBe "Your property details need changing"
    page.html
      .getElementById("property-details-changing-welsh-content-1")
      .text shouldBe "Before 1 April 2023, send an enquiry to tell us about changes to your property details such as floor area sizes and parking. We may accept your changes and update the current and future valuations."
    page.html
      .getElementById("property-details-changing-welsh-content-2")
      .text shouldBe "From 1 April 2023, use your business rates valuation account to send us a Check case."

    page.html.getElementById("other-question-subhead").text shouldBe "You have some other question about your valuation"
    page.html.getElementById("other-question-content").text should include(futureEffectiveDate)

    page.html
      .getElementById("covid-subhead")
      .text shouldBe "How Coronavirus (COVID-19) affected the future rateable value"
    val estimatorLink: Element = page.html.getElementById("help-estimator-link")
    estimatorLink.text should include(futureEffectiveDate)
    estimatorLink.attr("href") shouldBe estimatorUrl(isOwner = true) + "&tabName=help-tab"
  }

  "welsh draft request valuation page for an agent" should "display the valuation tab's dates and links correctly" in new FutureRequestSetup {

    lazy override val futureAssessment: ApiAssessment = {
      currentAssessment
        .copy(listType = ListType.DRAFT, effectiveDate = Some(april2023), billingAuthorityCode = Some("6000"))
    }

    override val controller = testController(estimatorUrl(isOwner = false))

    val result: Future[Result] = controller.myClientsAlreadyRequestedDetailValuation(
      propertyLinkSubmissionId = assessments.submissionId,
      valuationId = futureAssessment.assessmentRef
    )(request)

    val page: HtmlPage = HtmlPage(Jsoup.parse(contentAsString(result)))
    page.html.getElementById("rateable-value-may-change-subhead").text should include(futureEffectiveDate)
    page.html.getElementById("rateable-value-may-change-content").text should include(futureEffectiveDate)

    page.html.getElementById("rateable-value-too-high-subhead").text shouldBe "You think the rateable value is too high"
    page.html.getElementById("rateable-value-too-high-welsh-content-1").text should include(futureEffectiveDate)

    page.html.getElementById("property-details-changing-subhead").text shouldBe "Your property details need changing"
    page.html
      .getElementById("property-details-changing-welsh-content-1")
      .text shouldBe "Before 1 April 2023, send an enquiry to tell us about changes to your property details such as floor area sizes and parking. We may accept your changes and update the current and future valuations."
    page.html
      .getElementById("property-details-changing-welsh-content-2")
      .text shouldBe "From 1 April 2023, use your business rates valuation account to send us a Check case."

    page.html.getElementById("other-question-subhead").text shouldBe "You have some other question about your valuation"
    page.html.getElementById("other-question-content").text should include(futureEffectiveDate)

    page.html
      .getElementById("covid-subhead")
      .text shouldBe "How Coronavirus (COVID-19) affected the future rateable value"
    val estimatorLink: Element = page.html.getElementById("help-estimator-link")
    estimatorLink.text should include(futureEffectiveDate)
    estimatorLink.attr("href") shouldBe estimatorUrl(isOwner = false) + "&tabName=help-tab"
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

  "request current detailed valuation by ip" should "have the correct back link when coming from the future valuation request screen" in new FutureRequestSetup {
    val result: Future[Result] = controller.myOrganisationRequestDetailValuationCheck(
      propertyLinkSubmissionId = assessments.submissionId,
      valuationId = currentAssessment.assessmentRef,
      uarn = currentAssessment.uarn,
      challengeCaseRef = None,
      fromFuture = Some(true)
    )(request)
    val page: HtmlPage = HtmlPage(Jsoup.parse(contentAsString(result)))
    page.html.getElementById("back-link").attr("href") shouldBe ipFutureValuationUrl + "#valuation-tab"
  }

  "request current detailed valuation by agent" should "have the correct back link when coming from the future valuation request screen" in new FutureRequestSetup {
    val result: Future[Result] = controller.myClientsRequestDetailValuationCheck(
      propertyLinkSubmissionId = assessments.submissionId,
      valuationId = currentAssessment.assessmentRef,
      uarn = currentAssessment.uarn,
      challengeCaseRef = None,
      fromFuture = Some(true)
    )(request)
    val page: HtmlPage = HtmlPage(Jsoup.parse(contentAsString(result)))
    page.html.getElementById("back-link").attr("href") shouldBe clientFutureValuationUrl + "#valuation-tab"
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
    when(mockPropertyLinkConnector.clientPropertyLink(any())(any()))
      .thenReturn(Future.successful(Some(clientPropertyLinks)))

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

  "already submitted detailed valuation request by ip" should "have the correct back link when coming from the future valuation request screen" in new FutureRequestSetup {
    when(mockDvrCaseManagement.dvrExists(any(), any())(any())).thenReturn(Future.successful(false))
    val result: Future[Result] = controller.myOrganisationRequestDetailValuationCheck(
      propertyLinkSubmissionId = assessments.submissionId,
      valuationId = currentAssessment.assessmentRef,
      uarn = currentAssessment.uarn,
      challengeCaseRef = None,
      fromFuture = Some(true)
    )(request)
    val page: HtmlPage = HtmlPage(Jsoup.parse(contentAsString(result)))
    page.html.getElementById("back-link").attr("href") shouldBe ipFutureValuationUrl + "#valuation-tab"
  }

  "already submitted detailed valuation request by agent" should "have the correct back link when coming from the future valuation request screen" in new FutureRequestSetup {
    when(mockDvrCaseManagement.dvrExists(any(), any())(any())).thenReturn(Future.successful(false))
    val result: Future[Result] = controller.myClientsRequestDetailValuationCheck(
      propertyLinkSubmissionId = assessments.submissionId,
      valuationId = currentAssessment.assessmentRef,
      uarn = currentAssessment.uarn,
      challengeCaseRef = None,
      fromFuture = Some(true)
    )(request)
    val page: HtmlPage = HtmlPage(Jsoup.parse(contentAsString(result)))
    page.html.getElementById("back-link").attr("href") shouldBe clientFutureValuationUrl + "#valuation-tab"
  }

  "an IP starting a check case" should "get redirected to a page in check-frontend" in new Setup {
    val checkType: String = "internal"
    val result: Future[Result] =
      controller.myOrganisationStartCheck(propertyLinkSubmissionId = "PL123", valuationId = 1L, uarn = 123L)(
        FakeRequest().withFormUrlEncodedBody("checkType" -> checkType, "authorisationId" -> "12345"))
    status(result) shouldBe SEE_OTHER
    redirectLocation(result) shouldBe Some(
      s"http://localhost:9534/business-rates-check/property-link/12345/assessment/1/$checkType?propertyLinkSubmissionId=PL123&dvrCheck=true&rvth=false")
  }

  "an IP starting a check case" should "get redirected to a page in check-frontend - rateableValueTooHigh" in new Setup {
    val result: Future[Result] =
      controller.myOrganisationStartCheck(propertyLinkSubmissionId = "PL123", valuationId = 1L, uarn = 123L)(
        FakeRequest().withFormUrlEncodedBody("checkType" -> "rateableValueTooHigh", "authorisationId" -> "12345"))
    status(result) shouldBe SEE_OTHER
    redirectLocation(result) shouldBe Some(
      s"http://localhost:9534/business-rates-check/property-link/12345/assessment/1/internal?propertyLinkSubmissionId=PL123&dvrCheck=true&rvth=true")
  }

  "an IP starting a check case without selecting one of the reasons for check" should "stay on the same page with error summary at the top" in new Setup {

    when(mockPropertyLinkConnector.getOwnerAssessments(any())(any()))
      .thenReturn(Future.successful(Some(assessments)))
    when(mockPropertyLinkConnector.getMyOrganisationsCheckCases(any())(any()))
      .thenReturn(Future.successful(List.empty))
    when(mockChallengeConnector.getMyOrganisationsChallengeCases(any())(any()))
      .thenReturn(Future.successful(List.empty))
    when(mockDvrCaseManagement.getDvrDocuments(any(), any(), any())(any())).thenReturn(successfulDvrDocuments)

    val result: Future[Result] =
      controller.myOrganisationStartCheck(
        propertyLinkSubmissionId = "PL123",
        valuationId =
          assessments.assessments.headOption.fold(fail("expected to find at least 1 assessment"))(_.assessmentRef),
        uarn = 123L
      )(FakeRequest().withFormUrlEncodedBody())
    status(result) shouldBe BAD_REQUEST

    val doc = Jsoup.parse(contentAsString(result))
    Option(doc.getElementById("error-summary")) should not be empty
  }

  "an agent starting a check case" should "get redirected to a page in check-frontend" in new Setup {
    val checkType: String = "internal"
    val result: Future[Result] =
      controller.myClientsStartCheck(propertyLinkSubmissionId = "PL123", valuationId = 1L, uarn = 123L)(
        FakeRequest().withFormUrlEncodedBody("checkType" -> checkType, "authorisationId" -> "12345"))
    status(result) shouldBe SEE_OTHER
    redirectLocation(result) shouldBe Some(
      s"http://localhost:9534/business-rates-check/property-link/12345/assessment/1/$checkType?propertyLinkSubmissionId=PL123&dvrCheck=true&rvth=false")
  }

  "an agent starting a check case without selecting one of the reasons for check" should "stay on the same page with error summary at the top" in new Setup {

    when(mockPropertyLinkConnector.getClientAssessments(any())(any()))
      .thenReturn(Future.successful(Some(assessments)))
    when(mockPropertyLinkConnector.getMyClientsCheckCases(any())(any()))
      .thenReturn(Future.successful(List.empty))
    when(mockChallengeConnector.getMyClientsChallengeCases(any())(any()))
      .thenReturn(Future.successful(List.empty))
    when(mockDvrCaseManagement.getDvrDocuments(any(), any(), any())(any())).thenReturn(successfulDvrDocuments)

    val result: Future[Result] =
      controller.myClientsStartCheck(
        propertyLinkSubmissionId = "PL123",
        valuationId =
          assessments.assessments.headOption.fold(fail("expected to find at least 1 assessment"))(_.assessmentRef),
        uarn = 123L
      )(FakeRequest().withFormUrlEncodedBody())
    status(result) shouldBe BAD_REQUEST

    val doc = Jsoup.parse(contentAsString(result))
    Option(doc.getElementById("error-summary")) should not be empty
  }

}
