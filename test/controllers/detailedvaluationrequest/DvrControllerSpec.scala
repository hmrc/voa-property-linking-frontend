/*
 * Copyright 2024 HM Revenue & Customs
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
import config.ApplicationConfig
import connectors.SubmissionIdConnector
import controllers.VoaPropertyLinkingSpec
import models._
import models.challenge.ChallengeCaseStatus
import models.dvr.DvrRecord
import models.dvr.cases.check.CheckCaseStatus
import models.dvr.documents.{Document, DocumentSummary, DvrDocumentFiles}
import org.mockito.ArgumentMatchers.{any, eq => matching}
import org.mockito.Mockito.{never, verify, when}
import org.scalacheck.Arbitrary.arbitrary
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils._

import java.time.{LocalDate, LocalDateTime}
import models.dvr.cases.check.common.Agent
import models.dvr.cases.check.projection.CaseDetails
import models.properties.AllowedAction
import org.jsoup.{Jsoup, nodes}
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.mockito.Mockito
import org.scalatest.Inspectors
import play.api.i18n.MessagesApi
import play.api.mvc.{MessagesControllerComponents, Result}

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.ListHasAsScala

class DvrControllerSpec extends VoaPropertyLinkingSpec {

  val compiledList: ApplicationConfig = {
    val spyConfig = Mockito.spy(applicationConfig)
    spyConfig
  }
  trait Setup {
    implicit val request = FakeRequest()
    val checkSummaryUrlTemplate =
      "/check-case/{checkRef}/summary?propertyLinkSubmissionId={propertyLinkSubmissionId}&isOwner={isOwner}&valuationId={valuationId}"
    val enquiryUrlTemplate = "/draft-list-enquiry/start-from-dvr-valuation"
    val estimatorUrlTemplate = "/estimate-your-business-rates/start-from-dvr-valuation"

    lazy val config: ApplicationConfig = applicationConfig

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
      checkSummaryUrlTemplate = checkSummaryUrlTemplate,
      enquiryUrlTemplate = enquiryUrlTemplate,
      estimatorUrlTemplate = estimatorUrlTemplate,
      checkService = mockCheckService
    )(implicitly[ExecutionContext], implicitly[MessagesApi], implicitly[MessagesControllerComponents], config)

    lazy val mockSubmissionIds = {
      val m = mock[SubmissionIdConnector]
      when(m.get(matching("DVR"))(any())).thenReturn(Future.successful("DVR123"))
      m
    }

    val organisation = arbitrary[GroupAccount].sample.get
    val person = arbitrary[DetailedIndividualAccount].sample.get
    val assessment = arbitrary[Assessment].sample.get
    val link: PropertyLink = arbitrary[PropertyLink]

    lazy val successfulDvrDocuments: Future[Some[DvrDocumentFiles]] = Future.successful(Some(dvrDocuments))

    lazy val dvrDocuments: DvrDocumentFiles = {
      val now = LocalDateTime.now()
      DvrDocumentFiles(
        checkForm = Document(DocumentSummary("1L", "Check Document", now)),
        detailedValuation = Document(DocumentSummary("2L", "Detailed Valuation Document", now))
      )
    }

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
      s"http://localhost:9523/business-rates-property-linking/my-organisation/property-link/$plSubmissionId/valuations/$assessmentRef"
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
      s"http://localhost:9523/business-rates-property-linking/my-organisation/property-link/clients/all/$plSubmissionId/valuations/$assessmentRef"
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
      """<a href="/business-rates-property-linking/my-organisation/property-link/123456/valuations/55555""")
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
      """<a href="/business-rates-property-linking/my-organisation/property-link/clients/all/123456/valuations/55555""")
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
      )(request)

    status(result) shouldBe OK

    val doc = Jsoup.parse(contentAsString(result))

    val page = HtmlPage(doc)

    page.shouldContainText("If you want to change something in this valuation")

    page.shouldContainText("Local authority reference: BAREF")
    page.titleShouldMatch("123, Some Address, Some Town, AB1 CD2 - Valuation Office Agency - GOV.UK")
    page.shouldContain("#valuation-tab", 1)
    page.shouldContain("#check-cases-tab", 1)
    page.shouldContain("#start-check-tab", 1)
    page.shouldContain("#challenge-cases-tab", 1)

    page.verifyElementTextByAttribute("href", "#check-cases-tab", "Checks (1)")
    page.verifyElementTextByAttribute("href", "#challenge-cases-tab", "Challenges (1)")

    page.verifyElementText("challenge-rules-rules-1", "within 4 months of our Check case decision")
    page.verifyElementText(
      "challenge-rules-rules-2",
      "if you have waited more than 12 months and not received our Check case decision")
    page.verifyElementText(
      "challenge-rules-rules-3",
      "if it’s about a change in the local area (such as long-term roadworks), you send it within 16 months of sending the Check case and you have received our Check case decision"
    )

    page.verifyElementTextByAttribute(
      "href",
      "/business-rates-property-linking/my-organisation/property-link/1111/valuations/1234/file/2L",
      "Download the detailed valuation"
    )
    page.verifyElementTextByAttribute(
      "href",
      "/business-rates-property-linking/my-organisation/property-link/1111/valuations/1234/file/1L",
      "Download the Check case form")

    val radiosOnStartCheckScreen: Elements = doc.select("#checkType-form input[type=radio]")
    radiosOnStartCheckScreen should have size 7

    verify(mockPropertyLinkConnector).getMyOrganisationsCheckCases(any())(any())
    verify(mockChallengeConnector).getMyOrganisationsChallengeCases(any())(any())
  }

  "detailed valuation" should "display correct rateable value caption when list year = 2017 and end date is present" in new Setup {

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
      )(request)

    status(result) shouldBe OK

    val doc = Jsoup.parse(contentAsString(result))

    val page = HtmlPage(doc)

    page.shouldContainText("If you want to change something in this valuation")

    page.shouldContainText("Local authority reference: BAREF")
    page.titleShouldMatch("123, Some Address, Some Town, AB1 CD2 - Valuation Office Agency - GOV.UK")
    page.shouldContain("#valuation-tab", 1)
    page.verifyElementText("rateable-value-caption", "Previous rateable value (1 April 2017 to 1 June 2017)")

    page.verifyElementTextByAttribute("href", "#check-cases-tab", "Checks (1)")
    page.verifyElementTextByAttribute("href", "#challenge-cases-tab", "Challenges (1)")

    page.verifyElementTextByAttribute(
      "href",
      "/business-rates-property-linking/my-organisation/property-link/1111/valuations/1234/file/2L",
      "Download the detailed valuation"
    )
    page.verifyElementTextByAttribute(
      "href",
      "/business-rates-property-linking/my-organisation/property-link/1111/valuations/1234/file/1L",
      "Download the Check case form")

    val radiosOnStartCheckScreen: Elements = doc.select("#checkType-form input[type=radio]")
    radiosOnStartCheckScreen should have size 7

    verify(mockPropertyLinkConnector).getMyOrganisationsCheckCases(any())(any())
    verify(mockChallengeConnector).getMyOrganisationsChallengeCases(any())(any())
  }

  "detailed valuation check" should "display correct rateable value caption when list year = 2017 and end date is None" in new Setup {

    val assessmentsCurrentEndDateIsNone = assessments.copy(assessments = assessments.assessments.map(assessment =>
      assessment.copy(currentToDate = None, listType = ListType.PREVIOUS)))
    when(mockPropertyLinkConnector.getOwnerAssessments(any())(any()))
      .thenReturn(Future.successful(Some(assessmentsCurrentEndDateIsNone)))
    when(mockPropertyLinkConnector.getMyOrganisationsCheckCases(any())(any()))
      .thenReturn(Future.successful(List(ownerCheckCaseDetails)))
    when(mockChallengeConnector.getMyOrganisationsChallengeCases(any())(any()))
      .thenReturn(Future.successful(List(ownerChallengeCaseDetails)))
    when(mockDvrCaseManagement.getDvrDocuments(any(), any(), any())(any())).thenReturn(successfulDvrDocuments)

    val result =
      controller.myOrganisationRequestDetailValuationCheck(
        propertyLinkSubmissionId = "1111",
        valuationId = assessmentsCurrentEndDateIsNone.assessments.headOption
          .fold(fail("expected to find at least 1 assessment"))(_.assessmentRef),
      )(request)

    status(result) shouldBe OK

    val doc = Jsoup.parse(contentAsString(result))

    val page = HtmlPage(doc)

    page.shouldContainText("If you want to change something in this valuation")

    page.shouldContainText("Local authority reference: BAREF")
    page.titleShouldMatch("123, Some Address, Some Town, AB1 CD2 - Valuation Office Agency - GOV.UK")
    page.shouldContain("#valuation-tab", 1)
    page.verifyElementText("rateable-value-caption", "Previous rateable value (1 April 2017 to 31 March 2023)")

    page.verifyElementTextByAttribute("href", "#check-cases-tab", "Checks (1)")
    page.verifyElementTextByAttribute("href", "#challenge-cases-tab", "Challenges (1)")

    page.verifyElementTextByAttribute(
      "href",
      "/business-rates-property-linking/my-organisation/property-link/1111/valuations/1234/file/2L",
      "Download the detailed valuation"
    )
    page.verifyElementTextByAttribute(
      "href",
      "/business-rates-property-linking/my-organisation/property-link/1111/valuations/1234/file/1L",
      "Download the Check case form")

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
      )(request)

    status(result) shouldBe OK

    val resString = contentAsString(result)
    val page = HtmlPage(Jsoup.parse(resString))
    page.html.getElementById("client-name").text() shouldBe "Client property: Client Org Name"

    page.html
      .getElementById("back-link")
      .attr("href") shouldBe "/business-rates-property-linking/property-link/1111/assessments?owner=false"
  }

  "english detailed valuation tab when in compiled list" should "display a dvr download button and no welsh language explainer" in new ValuationTabSetup(
    compiledList) {
    override def assessments: ApiAssessments = apiAssessments(ownerAuthorisation, isWelsh = false)

    downloadValuation.classNames should not contain "govuk-link"
    downloadValuation.classNames should contain("govuk-button")
    downloadValuation.attr("href") shouldBe controllers.detailedvaluationrequest.routes.DvrController
      .myOrganisationRequestDetailedValuationRequestFile(
        ownerAuthorisation.submissionId,
        assessment.assessmentRef,
        dvrDocuments.detailedValuation.documentSummary.documentId)
      .url
    downloadValuation.text shouldBe "Download the detailed valuation"
    Option(welshLanguageExplainer) should not be defined
    Option(emailCcaLink) should not be defined
  }

  "welsh detailed valuation tab when in compiled list" should "display a dvr download button and a welsh language explainer" in new ValuationTabSetup(
    compiledList) {
    override def assessments: ApiAssessments = apiAssessments(ownerAuthorisation, isWelsh = true)

    downloadValuation.classNames should not contain "govuk-link"
    downloadValuation.classNames should contain("govuk-button")
    downloadValuation.text shouldBe "Download the detailed valuation"
    downloadValuation.attr("href") shouldBe controllers.detailedvaluationrequest.routes.DvrController
      .myOrganisationRequestDetailedValuationRequestFile(
        ownerAuthorisation.submissionId,
        assessment.assessmentRef,
        dvrDocuments.detailedValuation.documentSummary.documentId)
      .url
    welshLanguageExplainer.text shouldBe "If you need this valuation in Welsh, email ccaservice@voa.gov.uk with your request. Include the property address and valuation period (1 April 2017 to 1 June 2017) in the email."
    emailCcaLink.text shouldBe "ccaservice@voa.gov.uk"
    emailCcaLink.attr("href") shouldBe "mailto:ccaservice@voa.gov.uk"
  }

  "detailed valuation tab when in compiled list" should "display the correct 'change something' section" in new ValuationTabSetup(
    compiledList) {
    changeSomethingHeading.text shouldBe "If you want to change something in this valuation"
    changeSomethingContent.children.asScala.map(_.text) should contain theSameElementsInOrderAs Seq(
      "If the property’s details are incorrect, or you believe the rateable value is wrong, you must complete a Check case form.",
      "Download the Check case form",
      "After completing the form, send it to us as part of a Check case.",
      "! Warning Some older Check case forms may tell you to email or post your form. Please ignore this and use the 'Send my completed Check case form' button instead.",
      "Send my completed Check case form"
    )
    downloadCheckFormLink.text shouldBe "Download the Check case form"
    downloadCheckFormLink.attr("href") shouldBe controllers.detailedvaluationrequest.routes.DvrController
      .myOrganisationRequestDetailedValuationRequestFile(
        ownerAuthorisation.submissionId,
        assessment.assessmentRef,
        dvrDocuments.checkForm.documentSummary.documentId)
      .url
    sendCheckFormButton.text shouldBe "Send my completed Check case form"
    sendCheckFormButton.attr("href") shouldBe "#start-check-tab"
    sendCheckFormButton.classNames should contain("govuk-button--secondary")
  }

  abstract class ValuationTabSetup(configuration: ApplicationConfig) extends Setup {
    override lazy val config: ApplicationConfig = configuration

    val testAssessments: ApiAssessments =
      assessments.copy(assessments = assessments.assessments.map(a => a.copy(assessmentRef = assessment.assessmentRef)))

    when(mockPropertyLinkConnector.getOwnerAssessments(any())(any()))
      .thenReturn(Future.successful(Some(testAssessments)))
    when(mockDvrCaseManagement.getDvrDocuments(any(), any(), any())(any())).thenReturn(successfulDvrDocuments)
    when(mockPropertyLinkConnector.getMyOrganisationsCheckCases(any())(any())).thenReturn(Future.successful(List.empty))
    when(mockChallengeConnector.getMyOrganisationsChallengeCases(any())(any()))
      .thenReturn(Future.successful(List.empty))

    private val response =
      controller.myOrganisationRequestDetailValuationCheck(ownerAuthorisation.submissionId, assessment.assessmentRef)(
        request)
    status(response) shouldBe OK
    val valuationTab: Element = Jsoup.parse(contentAsString(response)).getElementById("valuation-tab")

    val downloadValuation: Element = valuationTab.getElementById("valuation-tab-download-link")
    val welshLanguageExplainer: Element = valuationTab.getElementById("welsh-language-explainer")
    val emailCcaLink: Element = valuationTab.getElementById("valuation-tab-email-cca-link")
    val changeSomethingHeading: Element = valuationTab.getElementById("valuation-tab-change-something-heading")
    val changeSomethingContent: Element = valuationTab.getElementById("valuation-tab-change-something-content")
    val downloadCheckFormLink: Element = valuationTab.getElementById("valuation-tab-download-check-form")
    val sendCheckFormButton: Element = valuationTab.getElementById("valuation-tab-send-check-form")
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
        challengeCaseRef = Some(challengeRef)
      )(request)

    status(result) shouldBe OK

    val resString = contentAsString(result)
    val page = HtmlPage(Jsoup.parse(resString))
    page.html.getElementById("client-name").text() shouldBe "Client property: Client Org Name"
    page.html
      .getElementById("back-link")
      .attr("href") shouldBe s"http://localhost:9531/business-rates-challenge/summary/property-link/${ownerAuthorisation.authorisationId}/submission-id/$plSubmissionId/challenge-cases/$challengeRef?isAgent=true&valuationId=$valuationId"
  }

  "detailed valuation of a DVR property" should "display correct CHECKS tab and table of check cases" in new Setup {
    val checkCaseDetails: List[CaseDetails] =
      CheckCaseStatus.values.toList.map(status => ownerCheckCaseDetails.copy(status = status.toString))

    when(mockPropertyLinkConnector.getOwnerAssessments(any())(any()))
      .thenReturn(Future.successful(Some(assessments)))
    when(mockPropertyLinkConnector.getMyOrganisationsCheckCases(any())(any()))
      .thenReturn(Future.successful(checkCaseDetails))
    when(mockChallengeConnector.getMyOrganisationsChallengeCases(any())(any()))
      .thenReturn(Future.successful(List.empty))
    when(mockDvrCaseManagement.getDvrDocuments(any(), any(), any())(any())).thenReturn(successfulDvrDocuments)

    val result =
      controller.myOrganisationRequestDetailValuationCheck(
        propertyLinkSubmissionId = assessments.submissionId,
        valuationId =
          assessments.assessments.headOption.fold(fail("expected to find at least 1 assessment"))(_.assessmentRef),
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

    private val assessmentRef =
      assessments.assessments.headOption.fold(fail("expected to find at least one assessment"))(_.assessmentRef)

    Inspectors.forAll(checkCaseDetails) { details =>
      val expectedCheckSummaryLink =
        s"/check-case/${details.caseReference}/summary?propertyLinkSubmissionId=${assessments.submissionId}&isOwner=true&valuationId=$assessmentRef"

      checksTable
        .getElementsContainingOwnText(details.caseReference)
        .attr("href") shouldBe expectedCheckSummaryLink
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
        assessments.assessments.headOption.fold(fail("expected to find at least 1 assessment"))(_.assessmentRef)
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

  "previous detailed valuation" should "return 200 OK and not display 'start a check' tab when no check allowed action returned" in new Setup {
    val agent = assessments.agents.head

    val assessmentsAllowedActions = assessments.copy(assessments = assessments.assessments.map(assessment =>
      assessment.copy(allowedActions = List(AllowedAction.VIEW_DETAILED_VALUATION), listType = ListType.PREVIOUS)))

    when(mockPropertyLinkConnector.getOwnerAssessments(any())(any()))
      .thenReturn(Future.successful(Some(assessmentsAllowedActions)))

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
          assessments.assessments.headOption.fold(fail("expected to find at least 1 assessment"))(_.assessmentRef)
      )(request)

    status(result) shouldBe OK
    val page = HtmlPage(Jsoup.parse(contentAsString(result)))

    //verify all tabs displayed
    page.shouldContain("#valuation-tab", 1)
    page.shouldContain("#start-check-tab", 0)
    page.shouldContain("#check-cases-tab", 1)
    page.shouldContain("#challenge-cases-tab", 1)
    page.shouldContain("#agents-tab", 1)
    page.shouldContainText(
      "You can no longer tell us about a change to the property details for valuations in the 2017 rating list period by starting a Check case.")
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
      checkSummaryUrlTemplate = checkSummaryUrlTemplate,
      enquiryUrlTemplate = enquiryUrlTemplate,
      estimatorUrlTemplate = estimatorUrl,
      checkService = mockCheckService
    )

    lazy val currentAssessment: ApiAssessment = super.assessments.assessments.drop(1).head
    lazy val futureAssessment: ApiAssessment = {
      currentAssessment.copy(listType = ListType.DRAFT, effectiveDate = Some(april2023))
    }
    lazy override val assessments: ApiAssessments =
      super.assessments.copy(assessments = futureAssessment :: super.assessments.assessments.tail.toList)

    lazy val futureEffectiveDate: String = Formatters.formatDate(futureAssessment.effectiveDate.get)
    lazy val ipCurrentValuationUrl: String = controllers.routes.Assessments
      .viewDetailedAssessment(
        assessments.submissionId,
        assessments.authorisationId,
        currentAssessment.assessmentRef,
        owner = true,
        fromValuation = Some(futureAssessment.assessmentRef))
      .url
    lazy val clientCurrentValuationUrl: String = controllers.routes.Assessments
      .viewDetailedAssessment(
        assessments.submissionId,
        assessments.authorisationId,
        currentAssessment.assessmentRef,
        owner = false,
        fromValuation = Some(futureAssessment.assessmentRef))
      .url
    lazy val ipFutureValuationUrl: String = controllers.routes.Assessments
      .viewDetailedAssessment(
        assessments.submissionId,
        assessments.authorisationId,
        futureAssessment.assessmentRef,
        owner = true
      )
      .url
    lazy val clientFutureValuationUrl: String = controllers.routes.Assessments
      .viewDetailedAssessment(
        assessments.submissionId,
        assessments.authorisationId,
        futureAssessment.assessmentRef,
        owner = false
      )
      .url

    def estimatorUrl(isOwner: Boolean): String =
      raw"""/estimate-your-business-rates/start-from-dvr-valuation?
           |authorisationId=${futureAssessment.authorisationId}&
           |isOwner=$isOwner&
           |propertyLinkSubmissionId=${assessments.submissionId}&
           |valuationId=${futureAssessment.assessmentRef}""".stripMargin.replaceAll("\n", "")

    lazy val currentListYear = "2017"

    when(mockPropertyLinkConnector.getOwnerAssessments(any())(any()))
      .thenReturn(Future.successful(Some(assessments)))
    when(mockPropertyLinkConnector.getClientAssessments(any())(any()))
      .thenReturn(Future.successful(Some(assessments)))
    when(mockDvrCaseManagement.getDvrRecord(any(), any())(any())).thenReturn(Future.successful(None))
    when(mockDvrCaseManagement.getDvrDocuments(any(), any(), any())(any())).thenReturn(successfulDvrDocuments)
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
      valuationId = link.authorisationId)(request)

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
      s"/business-rates-property-linking/my-organisation/property-link/${link.submissionId}/confirmation?submissionId=DVR123&valuationId=1")
  }

  abstract class RequestDvrScreenTestCase(configuration: ApplicationConfig) extends Setup {
    override lazy val config: ApplicationConfig = configuration

    val testAssessments: ApiAssessments =
      assessments.copy(assessments = assessments.assessments.map(a => a.copy(assessmentRef = assessment.assessmentRef)))

    when(mockPropertyLinkConnector.getOwnerAssessments(any())(any()))
      .thenReturn(Future.successful(Some(testAssessments)))
    when(mockDvrCaseManagement.getDvrRecord(any(), any())(any())).thenReturn(Future.successful(None))
    private val result =
      controller.myOrganisationAlreadyRequestedDetailValuation(link.submissionId, assessment.assessmentRef)(request)
    val html: nodes.Document = Jsoup.parse(contentAsString(result))

    status(result) shouldBe OK

    val addressCaption: Element = html.getElementById("assessment-address-caption")
    val addressHeading: Element = html.getElementById("assessment-address")
    val effectiveDate: Element = html.getElementById("effective-date")
    val councilRef: Element = html.getElementById("local-council-reference")
    val valuationSubhead: Element = html.getElementById("valuation-subhead")
    val rateableValue: Element = html.getElementById("rateable-value")
    val insetRvExplainer: Element = html.getElementById("rv-explainer")
    val valuationDetailsSubhead: Element = html.getElementById("valuation-details-subhead")
    val requestExplainer: Element = html.getElementById("request-explainer")
    val returnHomeLink: Element = html.getElementById("request-explainer-return-home-link")
    val changeSomethingHeading: Element = html.getElementById("change-something-heading")
    val changeSomethingExplainer: Element = html.getElementById("change-something-explainer")
    val challengeLink: Element = html.getElementById("change-something-challenge-link")
  }

  "request detailed valuation" should "display the correct content in compiled list for CURRENT properties with no toDate" in new RequestDvrScreenTestCase(
    compiledList) {
    override def assessments: ApiAssessments = apiAssessments(ownerAuthorisation, toDate = None)

    html.title() shouldBe "123, Some Address, Some Town, AB1 CD2 - Valuation Office Agency - GOV.UK"
    addressCaption.text() shouldBe "Your property"
    addressHeading.text() shouldBe "123, Some Address, Some Town, AB1 CD2"
    Option(effectiveDate) should not be defined
    councilRef.text() shouldBe s"Local council reference: ${ownerAuthorisation.localAuthorityRef}"
    valuationSubhead.text() shouldBe "Valuation"
    rateableValue.text() shouldBe "Current rateable value (1 April 2017 to present) £123"
    insetRvExplainer
      .text() shouldBe "This is the rateable value for the property. It is not what you pay in business rates or rent. Your local council uses the rateable value to calculate the business rates bill."
    valuationDetailsSubhead.text() shouldBe "Valuation details"
    requestExplainer.children().asScala.map(_.text()) should contain theSameElementsInOrderAs Seq(
      "To see how we calculated this rateable value, request the detailed valuation.",
      "The detailed valuation will be available to download when your request is approved.",
      "Request the detailed valuation"
    )
    Option(returnHomeLink) should not be defined

    changeSomethingHeading.text() shouldBe "If you want to change something in this valuation"
    changeSomethingExplainer.children().asScala.map(_.text()) should contain theSameElementsInOrderAs Seq(
      "If the property details need changing, or you think the rateable value is too high, you must first request the detailed valuation so you can start a Check case.",
      "If you have already sent us a Check case, we will contact you with the decision.",
      "If you disagree with our Check case decision, you can start a Challenge case. You must complete a Check case before sending a Challenge case."
    )
    challengeLink.attr("href") shouldBe applicationConfig.businessRatesValuationFrontendUrl(
      s"property-link/valuations/startChallenge?backLinkUrl=${controllers.detailedvaluationrequest.routes.DvrController
        .myOrganisationRequestDetailValuationCheck(link.submissionId, assessment.assessmentRef, tabName = Some("valuation-tab"))
        .absoluteURL(false, "localhost:9523")}"
    )
  }

  "request detailed valuation" should "display the correct content in compiled list for CURRENT properties with a toDate" in new RequestDvrScreenTestCase(
    compiledList) {
    override def assessments: ApiAssessments =
      apiAssessments(ownerAuthorisation, toDate = Some(april2023.minusMonths(3)))

    html.title() shouldBe "123, Some Address, Some Town, AB1 CD2 - Valuation Office Agency - GOV.UK"
    addressCaption.text() shouldBe "Your property"
    addressHeading.text() shouldBe "123, Some Address, Some Town, AB1 CD2"
    Option(effectiveDate) should not be defined
    councilRef.text() shouldBe s"Local council reference: ${ownerAuthorisation.localAuthorityRef}"
    valuationSubhead.text() shouldBe "Valuation"
    rateableValue.text() shouldBe "Previous rateable value (1 April 2017 to 1 January 2023) £123"
    insetRvExplainer
      .text() shouldBe "This was the rateable value for the property. It is not what you would have paid in business rates or rent. Your local council uses the rateable value to calculate the business rates bill."
    valuationDetailsSubhead.text() shouldBe "Valuation details"
    requestExplainer.children().asScala.map(_.text()) should contain theSameElementsInOrderAs Seq(
      "To see how we calculated this rateable value, request the detailed valuation.",
      "The detailed valuation will be available to download when your request is approved.",
      "Request the detailed valuation"
    )
    Option(returnHomeLink) should not be defined

    changeSomethingHeading.text() shouldBe "If you want to change something in this valuation"
    changeSomethingExplainer.children().asScala.map(_.text()) should contain theSameElementsInOrderAs Seq(
      "If the property details need changing, or you think the rateable value is too high, you must first request the detailed valuation so you can start a Check case.",
      "If you have already sent us a Check case, we will contact you with the decision.",
      "If you disagree with our Check case decision, you can start a Challenge case. You must complete a Check case before sending a Challenge case."
    )
    challengeLink.attr("href") shouldBe applicationConfig.businessRatesValuationFrontendUrl(
      s"property-link/valuations/startChallenge?backLinkUrl=${controllers.detailedvaluationrequest.routes.DvrController
        .myOrganisationRequestDetailValuationCheck(link.submissionId, assessment.assessmentRef, tabName = Some("valuation-tab"))
        .absoluteURL(false, "localhost:9523")}"
    )
  }

  "request detailed valuation" should "display the correct content in compiled list for PREVIOUS properties" in new RequestDvrScreenTestCase(
    compiledList) {
    override def assessments: ApiAssessments = apiAssessments(ownerAuthorisation, listType = ListType.PREVIOUS)

    html.title() shouldBe "123, Some Address, Some Town, AB1 CD2 - Valuation Office Agency - GOV.UK"
    addressCaption.text() shouldBe "Your property"
    addressHeading.text() shouldBe "123, Some Address, Some Town, AB1 CD2"
    Option(effectiveDate) should not be defined
    councilRef.text() shouldBe s"Local council reference: ${ownerAuthorisation.localAuthorityRef}"
    valuationSubhead.text() shouldBe "Valuation"
    rateableValue.text() shouldBe "Previous rateable value (1 April 2017 to 1 June 2017) £123"
    insetRvExplainer
      .text() shouldBe "This was the rateable value for the property. It is not what you would have paid in business rates or rent. Your local council uses the rateable value to calculate the business rates bill."
    valuationDetailsSubhead.text() shouldBe "Valuation details"
    requestExplainer.children().asScala.map(_.text()) should contain theSameElementsInOrderAs Seq(
      "To see how we calculated this rateable value, request the detailed valuation.",
      "The detailed valuation will be available to download when your request is approved.",
      "Request the detailed valuation"
    )
    Option(returnHomeLink) should not be defined

    changeSomethingHeading.text() shouldBe "If you want to change something in this valuation"
    changeSomethingExplainer.children().asScala.map(_.text()) should contain theSameElementsInOrderAs Seq(
      "If the property details need changing, or you think the rateable value is too high, you must first request the detailed valuation so you can start a Check case.",
      "If you have already sent us a Check case, we will contact you with the decision.",
      "If you disagree with our Check case decision, you can start a Challenge case. You must complete a Check case before sending a Challenge case."
    )
    challengeLink.attr("href") shouldBe applicationConfig.businessRatesValuationFrontendUrl(
      s"property-link/valuations/startChallenge?backLinkUrl=${controllers.detailedvaluationrequest.routes.DvrController
        .myOrganisationRequestDetailValuationCheck(link.submissionId, assessment.assessmentRef, tabName = Some("valuation-tab"))
        .absoluteURL(false, "localhost:9523")}"
    )
  }

  "request current detailed valuation by ip" should "have the correct back link regardless of coming from Bulk/LP/DVR" in new FutureRequestSetup {
    val result: Future[Result] = controller.myOrganisationRequestDetailValuationCheck(
      propertyLinkSubmissionId = assessments.submissionId,
      valuationId = currentAssessment.assessmentRef,
      challengeCaseRef = None,
      fromValuation = Some(futureAssessment.assessmentRef)
    )(request)
    val page: HtmlPage = HtmlPage(Jsoup.parse(contentAsString(result)))
    page.html.getElementById("back-link").attr("href") shouldBe ipFutureValuationUrl + "#valuation-tab"
  }

  "request current detailed valuation by agent" should "have the correct back link regardless of coming from Bulk/LP/DVR" in new FutureRequestSetup {
    val result: Future[Result] = controller.myClientsRequestDetailValuationCheck(
      propertyLinkSubmissionId = assessments.submissionId,
      valuationId = currentAssessment.assessmentRef,
      challengeCaseRef = None,
      fromValuation = Some(futureAssessment.assessmentRef)
    )(request)
    val page: HtmlPage = HtmlPage(Jsoup.parse(contentAsString(result)))
    page.html.getElementById("back-link").attr("href") shouldBe clientFutureValuationUrl + "#valuation-tab"
  }

  "request english detailed valuation confirmation submitted by IP in compiled list" should "display the correct content" in new DvrConfirmationTestCase(
    compiledList,
    agent = false) {
    html.title() shouldBe "Confirmation - Valuation Office Agency - GOV.UK"
    Option(localAuthorityRef) should not be defined
    Option(propertyAddress) should not be defined
    panel.children().asScala.map(_.text()) should contain theSameElementsInOrderAs Seq(
      "Request for detailed valuation sent",
      "Your reference number DVR123"
    )
    dvrReferenceSent.text() shouldBe "You’ve sent us a request for a detailed valuation for the following property:"
    propertySummaryList.children().asScala.map(_.text()) should contain theSameElementsInOrderAs Seq(
      "Address 123, Some Address, Some Town, AB1 CD2",
      s"Local council reference ${ownerAuthorisation.localAuthorityRef}"
    )
    dvrReferenceNote
      .text() shouldBe "Make a note of your reference number as you’ll need to provide it if you contact us."
    whatsNextHeading.text() shouldBe "What happens next"
    whatsNextExplainer.children().asScala.map(_.text()) should contain theSameElementsInOrderAs Seq(
      "We will process your request within 20 working days. We will send you a message when the detailed valuation is available.",
      "You will be able to download the valuation from your properties.",
      "We will contact you if we need more information.",
      "Go to your account home"
    )
    yourPropertiesLink.attr("href") shouldBe compiledList.dashboardUrl("your-properties")
    backToDashboardLink.attr("href") shouldBe compiledList.dashboardUrl("home")
    Option(welshValuationHeading) should not be defined
    Option(welshValuationContent) should not be defined
    Option(ccaEmailLink) should not be defined
  }

  "request welsh CURRENT (no toDate) detailed valuation confirmation submitted by IP in compiled list" should "display the correct content" in new DvrConfirmationTestCase(
    compiledList,
    agent = false) {
    override def assessments: ApiAssessments =
      apiAssessments(ownerAuthorisation, toDate = None, listYear = 2023, isWelsh = true)
    html.title() shouldBe "Confirmation - Valuation Office Agency - GOV.UK"
    Option(localAuthorityRef) should not be defined
    Option(propertyAddress) should not be defined
    panel.children().asScala.map(_.text()) should contain theSameElementsInOrderAs Seq(
      "Request for detailed valuation sent",
      "Your reference number DVR123"
    )
    dvrReferenceSent.text() shouldBe "You’ve sent us a request for a detailed valuation for the following property:"
    propertySummaryList.children().asScala.map(_.text()) should contain theSameElementsInOrderAs Seq(
      "Address 123, Some Address, Some Town, AB1 CD2",
      s"Local council reference ${ownerAuthorisation.localAuthorityRef}"
    )
    dvrReferenceNote
      .text() shouldBe "Make a note of your reference number as you’ll need to provide it if you contact us."
    whatsNextHeading.text() shouldBe "What happens next"
    whatsNextExplainer.children().asScala.map(_.text()) should contain theSameElementsInOrderAs Seq(
      "We will process your request within 20 working days. We will send you a message when the detailed valuation is available.",
      "You will be able to download the valuation from your properties.",
      "We will contact you if we need more information."
    )
    yourPropertiesLink.attr("href") shouldBe compiledList.dashboardUrl("your-properties")
    welshValuationHeading.text() shouldBe "If you need this valuation in Welsh"
    welshValuationContent.children().asScala.map(_.text()) should contain theSameElementsInOrderAs Seq(
      "Email your request to ccaservice@voa.gov.uk. Include the property address and valuation period (1 April 2017 to present) in the email.",
      "Go to your account home"
    )
    ccaEmailLink.attr("href") shouldBe "mailto:ccaservice@voa.gov.uk"
    backToDashboardLink.attr("href") shouldBe compiledList.dashboardUrl("home")
  }

  "request welsh CURRENT (with toDate) detailed valuation confirmation submitted by IP in compiled list" should "display the correct content" in new DvrConfirmationTestCase(
    compiledList,
    agent = false) {
    override def assessments: ApiAssessments =
      apiAssessments(ownerAuthorisation, toDate = Some(april2023.minusMonths(3)), isWelsh = true)

    html.title() shouldBe "Confirmation - Valuation Office Agency - GOV.UK"
    Option(localAuthorityRef) should not be defined
    Option(propertyAddress) should not be defined
    panel.children().asScala.map(_.text()) should contain theSameElementsInOrderAs Seq(
      "Request for detailed valuation sent",
      "Your reference number DVR123"
    )
    dvrReferenceSent.text() shouldBe "You’ve sent us a request for a detailed valuation for the following property:"
    propertySummaryList.children().asScala.map(_.text()) should contain theSameElementsInOrderAs Seq(
      "Address 123, Some Address, Some Town, AB1 CD2",
      s"Local council reference ${ownerAuthorisation.localAuthorityRef}"
    )
    dvrReferenceNote
      .text() shouldBe "Make a note of your reference number as you’ll need to provide it if you contact us."
    whatsNextHeading.text() shouldBe "What happens next"
    whatsNextExplainer.children().asScala.map(_.text()) should contain theSameElementsInOrderAs Seq(
      "We will process your request within 20 working days. We will send you a message when the detailed valuation is available.",
      "You will be able to download the valuation from your properties.",
      "We will contact you if we need more information."
    )
    yourPropertiesLink.attr("href") shouldBe compiledList.dashboardUrl("your-properties")
    welshValuationHeading.text() shouldBe "If you need this valuation in Welsh"
    welshValuationContent.children().asScala.map(_.text()) should contain theSameElementsInOrderAs Seq(
      "Email your request to ccaservice@voa.gov.uk. Include the property address and valuation period (1 April 2017 to 1 January 2023) in the email.",
      "Go to your account home"
    )
    ccaEmailLink.attr("href") shouldBe "mailto:ccaservice@voa.gov.uk"
    backToDashboardLink.attr("href") shouldBe compiledList.dashboardUrl("home")
  }

  "request english detailed valuation confirmation submitted by Agent in compiled list" should "display the correct content" in new DvrConfirmationTestCase(
    compiledList,
    agent = true) {
    html.title() shouldBe "Confirmation - Valuation Office Agency - GOV.UK"
    Option(localAuthorityRef) should not be defined
    Option(propertyAddress) should not be defined
    panel.children().asScala.map(_.text()) should contain theSameElementsInOrderAs Seq(
      "Request for detailed valuation sent",
      "Your reference number DVR123"
    )
    dvrReferenceSent.text() shouldBe "You’ve sent us a request for a detailed valuation for the following property:"
    propertySummaryList.children().asScala.map(_.text()) should contain theSameElementsInOrderAs Seq(
      "Address 123, Some Address, Some Town, EF3 GH4",
      s"Local council reference ${ownerAuthorisation.localAuthorityRef}"
    )
    dvrReferenceNote
      .text() shouldBe "Make a note of your reference number as you’ll need to provide it if you contact us."
    whatsNextHeading.text() shouldBe "What happens next"
    whatsNextExplainer.children().asScala.map(_.text()) should contain theSameElementsInOrderAs Seq(
      "We will process your request within 20 working days. We will send you a message when the detailed valuation is available.",
      "You will be able to download the valuation from your client’s properties.",
      "We will contact you if we need more information.",
      "Go to your account home"
    )
    yourPropertiesLink.attr("href") shouldBe compiledList.dashboardUrl(
      s"selected-client-properties?clientOrganisationId=${clientPropertyLink.client.organisationId}&clientName=${clientPropertyLink.client.organisationName}"
    )
    backToDashboardLink.attr("href") shouldBe compiledList.dashboardUrl("home")
  }

  "request welsh CURRENT (no toDate) detailed valuation confirmation submitted by Agent in compiled list" should "display the correct content" in new DvrConfirmationTestCase(
    compiledList,
    agent = true) {
    override def assessments: ApiAssessments =
      clientApiAssessments(clientPropertyLink, toDate = None, listYear = 2023, isWelsh = true)

    html.title() shouldBe "Confirmation - Valuation Office Agency - GOV.UK"
    Option(localAuthorityRef) should not be defined
    Option(propertyAddress) should not be defined
    panel.children().asScala.map(_.text()) should contain theSameElementsInOrderAs Seq(
      "Request for detailed valuation sent",
      "Your reference number DVR123"
    )
    dvrReferenceSent.text() shouldBe "You’ve sent us a request for a detailed valuation for the following property:"
    propertySummaryList.children().asScala.map(_.text()) should contain theSameElementsInOrderAs Seq(
      "Address 123, Some Address, Some Town, EF3 GH4",
      s"Local council reference ${ownerAuthorisation.localAuthorityRef}"
    )
    dvrReferenceNote
      .text() shouldBe "Make a note of your reference number as you’ll need to provide it if you contact us."
    whatsNextHeading.text() shouldBe "What happens next"
    whatsNextExplainer.children().asScala.map(_.text()) should contain theSameElementsInOrderAs Seq(
      "We will process your request within 20 working days. We will send you a message when the detailed valuation is available.",
      "You will be able to download the valuation from your client’s properties.",
      "We will contact you if we need more information."
    )
    yourPropertiesLink.attr("href") shouldBe compiledList.dashboardUrl(
      s"selected-client-properties?clientOrganisationId=${clientPropertyLink.client.organisationId}&clientName=${clientPropertyLink.client.organisationName}"
    )
    welshValuationHeading.text() shouldBe "If you need this valuation in Welsh"
    welshValuationContent.children().asScala.map(_.text()) should contain theSameElementsInOrderAs Seq(
      "Email your request to ccaservice@voa.gov.uk. Include the property address and valuation period (1 April 2017 to present) in the email.",
      "Go to your account home"
    )
    ccaEmailLink.attr("href") shouldBe "mailto:ccaservice@voa.gov.uk"
    backToDashboardLink.attr("href") shouldBe compiledList.dashboardUrl("home")
  }

  "request welsh CURRENT (with toDate) detailed valuation confirmation submitted by Agent in compiled list" should "display the correct content" in new DvrConfirmationTestCase(
    compiledList,
    agent = false) {
    override def assessments: ApiAssessments =
      clientApiAssessments(clientPropertyLink, toDate = Some(april2023.minusMonths(3)), isWelsh = true)

    html.title() shouldBe "Confirmation - Valuation Office Agency - GOV.UK"
    Option(localAuthorityRef) should not be defined
    Option(propertyAddress) should not be defined
    panel.children().asScala.map(_.text()) should contain theSameElementsInOrderAs Seq(
      "Request for detailed valuation sent",
      "Your reference number DVR123"
    )
    dvrReferenceSent.text() shouldBe "You’ve sent us a request for a detailed valuation for the following property:"
    propertySummaryList.children().asScala.map(_.text()) should contain theSameElementsInOrderAs Seq(
      "Address 123, Some Address, Some Town, EF3 GH4",
      s"Local council reference ${ownerAuthorisation.localAuthorityRef}"
    )
    dvrReferenceNote
      .text() shouldBe "Make a note of your reference number as you’ll need to provide it if you contact us."
    whatsNextHeading.text() shouldBe "What happens next"
    whatsNextExplainer.children().asScala.map(_.text()) should contain theSameElementsInOrderAs Seq(
      "We will process your request within 20 working days. We will send you a message when the detailed valuation is available.",
      "You will be able to download the valuation from your properties.",
      "We will contact you if we need more information."
    )
    yourPropertiesLink.attr("href") shouldBe compiledList.dashboardUrl("your-properties")
    welshValuationHeading.text() shouldBe "If you need this valuation in Welsh"
    welshValuationContent.children().asScala.map(_.text()) should contain theSameElementsInOrderAs Seq(
      "Email your request to ccaservice@voa.gov.uk. Include the property address and valuation period (1 April 2017 to 1 January 2023) in the email.",
      "Go to your account home"
    )
    ccaEmailLink.attr("href") shouldBe "mailto:ccaservice@voa.gov.uk"
    backToDashboardLink.attr("href") shouldBe compiledList.dashboardUrl("home")
  }

  "request detailed valuation as Agent" should "return 303 SEE_OTHER when request is valid" in new Setup {
    when(mockPropertyLinkConnector.getClientAssessments(any())(any()))
      .thenReturn(Future.successful(Some(assessments)))
    when(mockDvrCaseManagement.requestDetailedValuationV2(any())(any())).thenReturn(Future.successful(()))

    val result = controller.requestDetailedValuation(link.submissionId, 1L, false)(request)

    status(result) shouldBe SEE_OTHER
    redirectLocation(result) shouldBe Some(
      s"/business-rates-property-linking/my-organisation/property-link/clients/all/${link.submissionId}/confirmation?submissionId=DVR123&valuationId=1")
  }

  abstract class DvrConfirmationTestCase(configuration: ApplicationConfig, agent: Boolean) extends Setup {
    override lazy val config: ApplicationConfig = configuration

    override def assessments: ApiAssessments =
      if (agent) clientApiAssessments(clientPropertyLink)
      else apiAssessments(ownerAuthorisation)

    if (agent) {
      when(mockPropertyLinkConnector.clientPropertyLink(any())(any()))
        .thenReturn(Future.successful(Some(clientPropertyLinks)))
      when(mockPropertyLinkConnector.getClientAssessments(any())(any()))
        .thenReturn(Future.successful(Some(assessments)))
    } else {
      when(mockPropertyLinkConnector.getOwnerAssessments(any())(any()))
        .thenReturn(Future.successful(Some(assessments)))
    }

    private val result =
      if (agent)
        controller.myClientsRequestDetailValuationConfirmation(clientPropertyLink.submissionId, "DVR123", 1234L)(
          request)
      else
        controller.myOrganisationRequestDetailValuationConfirmation(ownerAuthorisation.submissionId, "DVR123", 1234L)(
          request)

    val html: nodes.Document = Jsoup.parse(contentAsString(result))

    status(result) shouldBe OK

    val localAuthorityRef: Element = html.getElementById("local-authority-reference")
    val propertyAddress: Element = html.getElementById("address")
    val panel: Element = html.getElementById("dvr-confirmation-panel")
    val dvrReferenceSent: Element = html.getElementById("dvr-reference-property-explainer")
    val propertySummaryList: Element = html.getElementById("property-summary-list")
    val dvrReferenceNote: Element = html.getElementById("dvr-reference-note-explainer")
    val whatsNextHeading: Element = html.getElementById("whats-next-heading")
    val whatsNextExplainer: Element = html.getElementById("whats-next-explainer")
    val yourPropertiesLink: Element = html.getElementById("whats-next-your-properties-link")
    val welshValuationHeading: Element = html.getElementById("welsh-valuation-heading")
    val welshValuationContent: Element = html.getElementById("welsh-valuation-content")
    val ccaEmailLink: Element = html.getElementById("welsh-valuation-email-cca")
    val backToDashboardLink: Element = html.getElementById("whats-next-back-to-dashboard-link")
  }

  "already sent dvr screen" should "display correctly in compiled list when viewing a previous valuation when a dvrSubmissionId has been stored" in new AlreadySentDvrTestCase(
    compiledList,
    dvrRecord) {
    override def assessments: ApiAssessments =
      apiAssessments(ownerAuthorisation, toDate = Some(april2023.minusMonths(3)), listType = ListType.PREVIOUS)

    html.title() shouldBe "123, Some Address, Some Town, AB1 CD2 - Valuation Office Agency - GOV.UK"
    headingCaption.text() shouldBe "Your property"
    heading.text() shouldBe "123, Some Address, Some Town, AB1 CD2"
    councilReference.text() shouldBe s"Local council reference: ${ownerAuthorisation.localAuthorityRef}"
    rvHeading.text() shouldBe "Valuation"
    rateableValue.children().asScala.map(_.text()) should contain theSameElementsInOrderAs Seq(
      "Previous rateable value (1 April 2017 to 1 January 2023)",
      "£123",
      "This was the rateable value for the property. It is not what you would have paid in business rates or rent. Your local council uses the rateable value to calculate the business rates bill."
    )
    detailsHeading.text() shouldBe "Valuation details"
    explainer.children().asScala.map(_.text()) should contain theSameElementsInOrderAs Seq(
      "You have sent us a request for the detailed valuation.",
      "The detailed valuation will be available to download within 20 working days of sending the request. We will send you a message when it is available.",
      s"Your request reference number is ${dvrRecord.dvrSubmissionId.getOrElse(fail())}."
    )
    mccHeading.text() shouldBe "If you need to tell us about a change in the local area"
    mccExplainer.children().asScala.map(_.text()) should contain theSameElementsInOrderAs Seq(
      "If you need to tell us about a change in the local area that affects your property, and you are waiting for the detailed valuation to be available, you can email ccaservice@voa.gov.uk and ask for your request to be prioritised.",
      "Include 'Urgent: external Material Change of Circumstances' in the subject line and include the property address and the request reference number in the email."
    )
    emailCcaLink.attr("href") shouldBe "mailto:ccaservice@voa.gov.uk"
    Option(homePageLink) should not be defined
  }
  "already sent dvr screen" should "display correctly in compiled list when viewing a previous valuation when no dvrSubmissionId has been stored" in new AlreadySentDvrTestCase(
    compiledList,
    dvrRecord.copy(dvrSubmissionId = None)) {
    when(mockDvrCaseManagement.getDvrRecord(any(), any())(any()))
      .thenReturn(Future.successful(Some(dvrRecord.copy(dvrSubmissionId = None))))
    override def assessments: ApiAssessments =
      apiAssessments(ownerAuthorisation, toDate = Some(april2023.minusMonths(3)), listType = ListType.PREVIOUS)

    html.title() shouldBe "123, Some Address, Some Town, AB1 CD2 - Valuation Office Agency - GOV.UK"
    headingCaption.text() shouldBe "Your property"
    heading.text() shouldBe "123, Some Address, Some Town, AB1 CD2"
    councilReference.text() shouldBe s"Local council reference: ${ownerAuthorisation.localAuthorityRef}"
    rvHeading.text() shouldBe "Valuation"
    rateableValue.children().asScala.map(_.text()) should contain theSameElementsInOrderAs Seq(
      "Previous rateable value (1 April 2017 to 1 January 2023)",
      "£123",
      "This was the rateable value for the property. It is not what you would have paid in business rates or rent. Your local council uses the rateable value to calculate the business rates bill."
    )
    detailsHeading.text() shouldBe "Valuation details"
    explainer.children().asScala.map(_.text()) should contain theSameElementsInOrderAs Seq(
      "You have sent us a request for the detailed valuation.",
      "The detailed valuation will be available to download within 20 working days of sending the request. We will send you a message when it is available.",
    )
    mccHeading.text() shouldBe "If you need to tell us about a change in the local area"
    mccExplainer.children().asScala.map(_.text()) should contain theSameElementsInOrderAs Seq(
      "If you need to tell us about a change in the local area that affects your property, and you are waiting for the detailed valuation to be available, you can email ccaservice@voa.gov.uk and ask for your request to be prioritised.",
      "Include 'Urgent: external Material Change of Circumstances' in the subject line and include the property address and the request reference number in the email."
    )
    emailCcaLink.attr("href") shouldBe "mailto:ccaservice@voa.gov.uk"
    Option(homePageLink) should not be defined
  }
  "already sent dvr screen" should "display correctly in compiled list when viewing a current valuation with no toDate" in new AlreadySentDvrTestCase(
    compiledList,
    dvrRecord) {
    override def assessments: ApiAssessments = apiAssessments(ownerAuthorisation, toDate = None, listYear = 2023)

    html.title() shouldBe "123, Some Address, Some Town, AB1 CD2 - Valuation Office Agency - GOV.UK"
    headingCaption.text() shouldBe "Your property"
    heading.text() shouldBe "123, Some Address, Some Town, AB1 CD2"
    councilReference.text() shouldBe s"Local council reference: ${ownerAuthorisation.localAuthorityRef}"
    rvHeading.text() shouldBe "Valuation"
    rateableValue.children().asScala.map(_.text()) should contain theSameElementsInOrderAs Seq(
      "Current rateable value (1 April 2017 to present)",
      "£123",
      "This is the rateable value for the property. It is not what you pay in business rates or rent. Your local council uses the rateable value to calculate the business rates bill."
    )
    detailsHeading.text() shouldBe "Valuation details"
    explainer.children().asScala.map(_.text()) should contain theSameElementsInOrderAs Seq(
      "You have sent us a request for the detailed valuation.",
      "The detailed valuation will be available to download within 20 working days of sending the request. We will send you a message when it is available.",
      s"Your request reference number is ${dvrRecord.dvrSubmissionId.getOrElse(fail())}."
    )
    mccHeading.text() shouldBe "If you need to tell us about a change in the local area"
    mccExplainer.children().asScala.map(_.text()) should contain theSameElementsInOrderAs Seq(
      "If you need to tell us about a change in the local area that affects your property, and you are waiting for the detailed valuation to be available, you can email ccaservice@voa.gov.uk and ask for your request to be prioritised.",
      "Include 'Urgent: external Material Change of Circumstances' in the subject line and include the property address and the request reference number in the email."
    )
    emailCcaLink.attr("href") shouldBe "mailto:ccaservice@voa.gov.uk"
    Option(homePageLink) should not be defined
  }
  "already sent dvr screen" should "display correctly in compiled list when viewing a current valuation with a toDate" in new AlreadySentDvrTestCase(
    compiledList,
    dvrRecord) {
    override def assessments: ApiAssessments =
      apiAssessments(ownerAuthorisation, toDate = Some(april2023.minusMonths(3)), listYear = 2023)

    html.title() shouldBe "123, Some Address, Some Town, AB1 CD2 - Valuation Office Agency - GOV.UK"
    headingCaption.text() shouldBe "Your property"
    heading.text() shouldBe "123, Some Address, Some Town, AB1 CD2"
    councilReference.text() shouldBe s"Local council reference: ${ownerAuthorisation.localAuthorityRef}"
    rvHeading.text() shouldBe "Valuation"
    rateableValue.children().asScala.map(_.text()) should contain theSameElementsInOrderAs Seq(
      "Previous rateable value (1 April 2017 to 1 January 2023)",
      "£123",
      "This was the rateable value for the property. It is not what you would have paid in business rates or rent. Your local council uses the rateable value to calculate the business rates bill."
    )
    detailsHeading.text() shouldBe "Valuation details"
    explainer.children().asScala.map(_.text()) should contain theSameElementsInOrderAs Seq(
      "You have sent us a request for the detailed valuation.",
      "The detailed valuation will be available to download within 20 working days of sending the request. We will send you a message when it is available.",
      s"Your request reference number is ${dvrRecord.dvrSubmissionId.getOrElse(fail())}."
    )
    mccHeading.text() shouldBe "If you need to tell us about a change in the local area"
    mccExplainer.children().asScala.map(_.text()) should contain theSameElementsInOrderAs Seq(
      "If you need to tell us about a change in the local area that affects your property, and you are waiting for the detailed valuation to be available, you can email ccaservice@voa.gov.uk and ask for your request to be prioritised.",
      "Include 'Urgent: external Material Change of Circumstances' in the subject line and include the property address and the request reference number in the email."
    )
    emailCcaLink.attr("href") shouldBe "mailto:ccaservice@voa.gov.uk"
    Option(homePageLink) should not be defined
  }

  abstract class AlreadySentDvrTestCase(configuration: ApplicationConfig, dvrRecord: DvrRecord) extends Setup {
    override lazy val config: ApplicationConfig = configuration

    private val testAssessments =
      assessments.copy(assessments = assessments.assessments.map(a => a.copy(assessmentRef = assessment.assessmentRef)))

    when(mockPropertyLinkConnector.getOwnerAssessments(any())(any()))
      .thenReturn(Future.successful(Some(testAssessments)))
    when(mockDvrCaseManagement.getDvrRecord(any(), any())(any())).thenReturn(Future.successful(Some(dvrRecord)))

    private val result =
      controller.alreadySubmittedDetailedValuationRequest(
        ownerAuthorisation.submissionId,
        assessment.assessmentRef,
        owner = true)(request)

    val html: nodes.Document = Jsoup.parse(contentAsString(result))

    status(result) shouldBe OK

    val headingCaption: Element = html.getElementById("already-requested-heading-caption")
    val heading: Element = html.getElementById("already-requested-heading")
    val councilReference: Element = html.getElementById("local-council-reference")
    val rvHeading: Element = html.getElementById("rv-heading")
    val rateableValue: Element = html.getElementById("rateable-value")
    val detailsHeading: Element = html.getElementById("valuation-details-heading")
    val explainer: Element = html.getElementById("valuation-details-explainer")
    val mccHeading: Element = html.getElementById("mcc-heading")
    val mccExplainer: Element = html.getElementById("mcc-explainer")
    val emailCcaLink: Element = html.getElementById("email-cca-link")
    val homePageLink: Element = html.getElementById("dvr-home-link")
  }

  "already submitted detailed valuation request by ip" should "have the correct back link regardless of coming from Bulk/LP/DVR" in new FutureRequestSetup {
    when(mockDvrCaseManagement.getDvrRecord(any(), any())(any())).thenReturn(Future.successful(None))
    val result: Future[Result] = controller.myOrganisationRequestDetailValuationCheck(
      propertyLinkSubmissionId = assessments.submissionId,
      valuationId = currentAssessment.assessmentRef,
      challengeCaseRef = None,
      fromValuation = Some(futureAssessment.assessmentRef)
    )(request)
    val page: HtmlPage = HtmlPage(Jsoup.parse(contentAsString(result)))
    page.html.getElementById("back-link").attr("href") shouldBe ipFutureValuationUrl + "#valuation-tab"
  }

  "already submitted detailed valuation request by agent" should "have the correct back link regardless of coming from Bulk/LP/DVR" in new FutureRequestSetup {
    when(mockDvrCaseManagement.getDvrRecord(any(), any())(any())).thenReturn(Future.successful(None))
    val result: Future[Result] = controller.myClientsRequestDetailValuationCheck(
      propertyLinkSubmissionId = assessments.submissionId,
      valuationId = currentAssessment.assessmentRef,
      challengeCaseRef = None,
      fromValuation = Some(futureAssessment.assessmentRef)
    )(request)
    val page: HtmlPage = HtmlPage(Jsoup.parse(contentAsString(result)))
    page.html.getElementById("back-link").attr("href") shouldBe clientFutureValuationUrl + "#valuation-tab"
  }

  "already submitted detailed valuation" should "return correct content when current valuation present" in new FutureRequestSetup {
    when(mockDvrCaseManagement.getDvrRecord(any(), any())(any())).thenReturn(Future.successful(None))
    val result: Future[Result] = controller.myOrganisationRequestDetailValuationCheck(
      propertyLinkSubmissionId = assessments.submissionId,
      valuationId = currentAssessment.assessmentRef,
    )(request)
    val page: HtmlPage = HtmlPage(Jsoup.parse(contentAsString(result)))

    page.html.getElementById("valuation-tab-change-something-content").text() should include(
      "we have altered this valuation in the last 6 months and you send a Check case from the current valuation")
    page.html.getElementById("valuation-tab-change-something-content").text() should include(
      "a court decision affected this property’s rateable value and before 1 October 2023 you send a Check case from the current valuation")
  }

  "already submitted detailed valuation" should "return correct content if current valuation is not available " in new FutureRequestSetup {

    val assessmentsAllowedActions = assessments.copy(assessments = assessments.assessments.map(assessment =>
      assessment.copy(allowedActions = List(AllowedAction.VIEW_DETAILED_VALUATION), listType = ListType.PREVIOUS)))
    when(mockPropertyLinkConnector.getOwnerAssessments(any())(any()))
      .thenReturn(Future.successful(Some(assessmentsAllowedActions)))
    when(mockPropertyLinkConnector.getMyOrganisationsCheckCases(any())(any()))
      .thenReturn(Future.successful(CheckCaseStatus.values.toList.map(status =>
        ownerCheckCaseDetails.copy(status = status.toString))))
    when(mockChallengeConnector.getMyOrganisationsChallengeCases(any())(any()))
      .thenReturn(Future.successful(ChallengeCaseStatus.values.toList.map(status =>
        ownerChallengeCaseDetails.copy(status = status.toString))))
    when(mockDvrCaseManagement.getDvrDocuments(any(), any(), any())(any())).thenReturn(successfulDvrDocuments)

    val result =
      controller.myOrganisationRequestDetailValuationCheck(
        propertyLinkSubmissionId = "1111",
        valuationId = currentAssessment.assessmentRef
      )(request)

    status(result) shouldBe OK
    val page = HtmlPage(Jsoup.parse(contentAsString(result)))

    page.html.getElementById("valuation-tab-change-something-content").text() should include(
      "we have altered this valuation in the last 6 months and you email us at ccaservice@voa.gov.uk to send a Check case")
    page.html.getElementById("valuation-tab-change-something-content").text() should include(
      "a court decision affected this property’s rateable value and before 1 October 2023 you email us at ccaservice@voa.gov.uk to send a Check case")
  }
}
