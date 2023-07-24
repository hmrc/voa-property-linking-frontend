/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.propertylinking.controllers.valuations

import actions.assessments.WithAssessmentsPageSessionRefiner
import controllers.VoaPropertyLinkingSpec
import models.ApiAssessment.AssessmentWithFromDate
import models.assessments.PreviousPage.SelectedClient
import models.assessments.{AssessmentsPageSession, PreviousPage}
import models.properties.AllowedAction
import models.{ApiAssessment, ApiAssessments, ClientPropertyLink, ListType}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.{any, eq => mEq}
import org.mockito.Mockito._
import org.scalacheck.Arbitrary.arbitrary
import play.api.mvc.Cookie
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import utils._

import scala.concurrent.Future

class ValuationsControllerSpec extends VoaPropertyLinkingSpec {

  implicit val request = FakeRequest()

  trait Setup {

    val plSubId = "pl-submission-id"
    val previousPage = SelectedClient

    when(mockSessionRepository.start(any())(any(), any())).thenReturn(Future.successful((): Unit))
    when(mockSessionRepository.get[AssessmentsPageSession](any(), any()))
      .thenReturn(Future.successful(Some(assessmentPageSession)))
    when(mockCustomErrorHandler.notFoundTemplate(any())).thenReturn(Html("not found"))

    val valuationsController = new ValuationsController(
      errorHandler = mockCustomErrorHandler,
      propertyLinks = mockPropertyLinkConnector,
      authenticated = preAuthenticatedActionBuilders(),
      sessionRepo = mockSessionRepository,
      assessmentsView = assessmentsView,
      withAssessmentsPageSession = new WithAssessmentsPageSessionRefiner(mockSessionRepository),
      controllerComponents = stubMessagesControllerComponents()
    )
  }

  trait ValuationsSetup extends Setup {

    def assessments: Future[Option[ApiAssessments]]

    when(mockPropertyLinkConnector.getOwnerAssessments(any())(any())).thenReturn(assessments)
    when(mockPropertyLinkConnector.getClientAssessments(any())(any())).thenReturn(assessments)
  }

  behavior of "saving the previous page"

  it should "save when called by the property link owner (IP) - no cache id" in new Setup {
    val res = valuationsController
      .savePreviousPage(previousPage = previousPage.toString, submissionId = plSubId, owner = true)(request)

    status(res) shouldBe SEE_OTHER
    header(LOCATION, res) shouldBe Some(s"/business-rates-property-linking/property-link/$plSubId/assessments")

    verify(mockSessionRepository).start(mEq(AssessmentsPageSession(previousPage)))(any(), any())
  }

  it should "save when called by the property link owner (IP) - with cache id" in new Setup {
    val res = valuationsController
      .savePreviousPage(previousPage = previousPage.toString, submissionId = plSubId, owner = true)(request)

    status(res) shouldBe SEE_OTHER
    header(LOCATION, res) shouldBe Some(s"/business-rates-property-linking/property-link/$plSubId/assessments")

    verify(mockSessionRepository).start(mEq(AssessmentsPageSession(previousPage)))(any(), any())
  }

  it should "save when called by an agent - no cache id" in new Setup {
    val res = valuationsController
      .savePreviousPage(previousPage = previousPage.toString, submissionId = plSubId, owner = false)(request)

    status(res) shouldBe SEE_OTHER
    header(LOCATION, res) shouldBe Some(
      s"/business-rates-property-linking/property-link/$plSubId/assessments?owner=false")

    verify(mockSessionRepository).start(mEq(AssessmentsPageSession(previousPage)))(any(), any())
  }

  it should "save when called by an agent - with cache id" in new Setup {
    val res = valuationsController
      .savePreviousPage(previousPage = previousPage.toString, submissionId = plSubId, owner = false)(request)

    status(res) shouldBe SEE_OTHER
    header(LOCATION, res) shouldBe Some(
      s"/business-rates-property-linking/property-link/$plSubId/assessments?owner=false")

    verify(mockSessionRepository).start(mEq(AssessmentsPageSession(previousPage)))(any(), any())
  }

  behavior of "valuations page"

  it should "return 404 when no assessments for given property link are found" in new ValuationsSetup {

    override def assessments: Future[Option[ApiAssessments]] =
      Future.successful(Some(apiAssessments(ownerAuthorisation).copy(assessments = List.empty)))

    val res = valuationsController.valuations(plSubId, owner = true)(request)

    status(res) shouldBe NOT_FOUND
  }

  it should "return 200 with assessments for OWNER" in new ValuationsSetup {
    lazy val as = apiAssessments(ownerAuthorisation)
    override def assessments: Future[Option[ApiAssessments]] = Future.successful(Some(as))

    val res = valuationsController.valuations(plSubId, owner = true)(request)
    status(res) shouldBe OK
  }

  it should "return 200 with assessments for OWNER with the correct back link" in new ValuationsSetup {
    when(mockSessionRepository.get[AssessmentsPageSession](any(), any()))
      .thenReturn(Future.successful(Some(AssessmentsPageSession(PreviousPage.MyProperties))))

    lazy val as = apiAssessments(ownerAuthorisation)
    override def assessments: Future[Option[ApiAssessments]] = Future.successful(Some(as))

    val res = valuationsController.valuations(plSubId, owner = true)(request)
    status(res) shouldBe OK

    val returnedHtml = contentAsString(res)
    val document: Document = Jsoup.parse(returnedHtml)

    document.getElementById("back-link").attr("href") shouldBe applicationConfig.dashboardUrl(
      "return-to-your-properties")

  }

  it should "exclude any assessments, for which viewing the detailed valuation is not an allowed action" in new ValuationsSetup {
    //this produces 3 assessments with IDs 1234, 1235 and 1236
    //only 1234 and 1235 have VIEW_DETAILED_VALUATION in the list of allowedActions
    //1236 should not be rendered in the page, as the user can't view the detailed valuation
    lazy val as = apiAssessments(ownerAuthorisation)

    val nonViewableAssesement =
      as.assessments.find(_.assessmentRef == 1236L).getOrElse(fail("expecting to find valuation ref 1236"))

    nonViewableAssesement.allowedActions should not contain AllowedAction.VIEW_DETAILED_VALUATION
    nonViewableAssesement.allowedActions should contain(AllowedAction.ENQUIRY)

    override def assessments: Future[Option[ApiAssessments]] = Future.successful(Some(as))

    val res = valuationsController.valuations(plSubId, owner = true)(request)
    val returnedHtml = contentAsString(res)
    val doc: Document = Jsoup.parse(returnedHtml)
    Option(doc.getElementById("viewAssessmentLink-1234")).map(_.text()) shouldBe Some("1 April 2017 to 1 June 2017")
    Option(doc.getElementById("viewAssessmentLink-1235")).map(_.text()) shouldBe Some("1 June 2017 to present")
    Option(doc.getElementById("viewAssessmentLink-1236")) shouldBe None
  }

  it should "return 200 with assessments sorted by currentFromDate DESC for OWNER" in new ValuationsSetup {
    lazy val as = apiAssessments(ownerAuthorisation.copy(status = "PENDING"))
    override def assessments: Future[Option[ApiAssessments]] = Future.successful(Some(as))

    val res = valuationsController.valuations(plSubId, owner = true)(request)
    status(res) shouldBe OK

    val sortedAssessments: List[ApiAssessment] =
      valuationsController.assessmentsWithLinks(as, plSubId, owner = true).map(_._2).toList

    inside(sortedAssessments) {
      case AssessmentWithFromDate(fromDate1) :: AssessmentWithFromDate(fromDate2) :: Nil =>
        //first assessment in the list should have a "later" start date than the next one
        fromDate1.toEpochDay should be > fromDate2.toEpochDay
    }
  }

  it should "return 200 with assessments sorted by currentFromDate DESC for AGENT" in new ValuationsSetup {
    lazy val as = apiAssessments(ownerAuthorisation.copy(status = "PENDING"))
    override def assessments: Future[Option[ApiAssessments]] = Future.successful(Some(as))

    val clientProperty: ClientPropertyLink = arbitrary[ClientPropertyLink]

    when(mockPropertyLinkConnector.clientPropertyLink(mEq(plSubId))(any()))
      .thenReturn(Future.successful(Some(clientProperty)))

    val res = valuationsController.valuations(plSubId, owner = false)(request)
    status(res) shouldBe OK

    val sortedAssessments: List[ApiAssessment] =
      valuationsController.assessmentsWithLinks(as, plSubId, owner = false).map(_._2).toList

    inside(sortedAssessments) {
      case AssessmentWithFromDate(fromDate1) :: AssessmentWithFromDate(fromDate2) :: Nil =>
        //first assessment in the list should have a "later" start date than the next one
        fromDate1.toEpochDay should be > fromDate2.toEpochDay
    }
  }

  it should "return 200 with assessments for AGENT with the correct back link for all clients" in new ValuationsSetup {
    when(mockSessionRepository.get[AssessmentsPageSession](any(), any()))
      .thenReturn(Future.successful(Some(AssessmentsPageSession(PreviousPage.AllClients))))

    lazy val as = apiAssessments(ownerAuthorisation)
    override def assessments: Future[Option[ApiAssessments]] = Future.successful(Some(as))

    val clientProperty: ClientPropertyLink = arbitrary[ClientPropertyLink]

    when(mockPropertyLinkConnector.clientPropertyLink(mEq(plSubId))(any()))
      .thenReturn(Future.successful(Some(clientProperty)))

    val res = valuationsController.valuations(plSubId, owner = false)(request)
    status(res) shouldBe OK

    val returnedHtml = contentAsString(res)
    val document: Document = Jsoup.parse(returnedHtml)

    document.getElementById("back-link").attr("href") shouldBe applicationConfig.dashboardUrl(
      "return-to-client-properties")
  }

  it should "return 200 with assessments for AGENT with the correct back link for selected client" in new ValuationsSetup {

    val clientProperty: ClientPropertyLink = arbitrary[ClientPropertyLink]

    when(mockSessionRepository.get[AssessmentsPageSession](any(), any()))
      .thenReturn(Future.successful(Some(AssessmentsPageSession(PreviousPage.SelectedClient))))

    when(mockPropertyLinkConnector.clientPropertyLink(mEq(plSubId))(any()))
      .thenReturn(Future.successful(Some(clientProperty)))

    lazy val as = apiAssessments(ownerAuthorisation)
    override def assessments: Future[Option[ApiAssessments]] = Future.successful(Some(as))

    val res = valuationsController.valuations(plSubId, owner = false)(request)
    status(res) shouldBe OK

    val returnedHtml = contentAsString(res)
    val document: Document = Jsoup.parse(returnedHtml)

    document.getElementById("back-link").attr("href") shouldBe applicationConfig.dashboardUrl(
      s"return-to-selected-client-properties?organisationId=${clientProperty.client.organisationId}&organisationName=${clientProperty.client.organisationName}")
  }

  it should "return 200 with assessments for AGENT with the correct back link when Dashboard is the previous page" in new ValuationsSetup {

    val clientProperty: ClientPropertyLink = arbitrary[ClientPropertyLink]

    when(mockSessionRepository.get[AssessmentsPageSession](any(), any()))
      .thenReturn(Future.successful(Some(AssessmentsPageSession(PreviousPage.Dashboard))))

    when(mockPropertyLinkConnector.clientPropertyLink(mEq(plSubId))(any()))
      .thenReturn(Future.successful(Some(clientProperty)))

    lazy val as = apiAssessments(ownerAuthorisation)
    override def assessments: Future[Option[ApiAssessments]] = Future.successful(Some(as))

    val res = valuationsController.valuations(plSubId, owner = false)(request)
    status(res) shouldBe OK

    val returnedHtml = contentAsString(res)
    val document: Document = Jsoup.parse(returnedHtml)

    document.getElementById("back-link").attr("href") shouldBe applicationConfig.dashboardUrl("home")
  }

  it should "return 200 with assessments for OWNER with the correct back link when Dashboard is the previous page" in new ValuationsSetup {

    when(mockSessionRepository.get[AssessmentsPageSession](any(), any()))
      .thenReturn(Future.successful(Some(AssessmentsPageSession(PreviousPage.Dashboard))))

    lazy val as = apiAssessments(ownerAuthorisation)
    override def assessments: Future[Option[ApiAssessments]] = Future.successful(Some(as))

    val res = valuationsController.valuations(plSubId, owner = true)(request)
    status(res) shouldBe OK

    val returnedHtml = contentAsString(res)
    val document: Document = Jsoup.parse(returnedHtml)

    document.getElementById("back-link").attr("href") shouldBe applicationConfig.dashboardUrl("home")
  }

  it should "previous/current valuations toDate should display correctly when no toDate returned from modernise" in new ValuationsSetup {

    when(mockSessionRepository.get[AssessmentsPageSession](any(), any()))
      .thenReturn(Future.successful(Some(AssessmentsPageSession(PreviousPage.Dashboard))))

    lazy val assessmentsData = apiAssessments(ownerAuthorisation, None, ListType.PREVIOUS)
    override def assessments: Future[Option[ApiAssessments]] = Future.successful(Some(assessmentsData))

    val res = valuationsController.valuations(plSubId, owner = true)(request)
    status(res) shouldBe OK

    val returnedHtml = contentAsString(res)
    val document: Document = Jsoup.parse(returnedHtml)

    Option(document.getElementById("viewAssessmentLink-1234")).map(_.text()) shouldBe Some(
      "1 April 2017 to 31 March 2023")
    Option(document.getElementById("viewAssessmentLink-1235")).map(_.text()) shouldBe Some("1 June 2017 to present")
  }

  it should "previous valuations toDate should be displayed when toDate returned from modernise" in new ValuationsSetup {

    when(mockSessionRepository.get[AssessmentsPageSession](any(), any()))
      .thenReturn(Future.successful(Some(AssessmentsPageSession(PreviousPage.Dashboard))))

    lazy val assessmentsData = apiAssessments(ownerAuthorisation, listType = ListType.PREVIOUS)

    override def assessments: Future[Option[ApiAssessments]] = Future.successful(Some(assessmentsData))

    val res = valuationsController.valuations(plSubId, owner = true)(request)
    status(res) shouldBe OK

    val returnedHtml = contentAsString(res)
    val document: Document = Jsoup.parse(returnedHtml)

    Option(document.getElementById("viewAssessmentLink-1234")).map(_.text()) shouldBe Some(
      "1 April 2017 to 1 June 2017")
    Option(document.getElementById("viewAssessmentLink-1235")).map(_.text()) shouldBe Some("1 June 2017 to present")

  }

  it should "display the correct table headings in welsh" in new ValuationsSetup {

    when(mockSessionRepository.get[AssessmentsPageSession](any(), any()))
      .thenReturn(Future.successful(Some(AssessmentsPageSession(PreviousPage.Dashboard))))

    lazy val as = apiAssessments(ownerAuthorisation)
    override def assessments: Future[Option[ApiAssessments]] = Future.successful(Some(as))

    val res = valuationsController.valuations(plSubId, owner = true)(welshFakeRequest)
    status(res) shouldBe OK

    val returnedHtml = contentAsString(res)
    val document: Document = Jsoup.parse(returnedHtml)

    Option(document.select("#assessments-table > thead > tr:nth-child(1) > th:nth-child(1)").first().text()) shouldBe Some(
      "Prisiadau Cymorth gyda Prisiadau")
    Option(document.select("#assessments-table > thead > tr:nth-child(1) > th:nth-child(2)")).map(_.text()) shouldBe
      Some("Dyddiad dod i rym Cymorth gyda Dyddiad dod i rym")
    Option(document.select("#assessments-table > thead > tr:nth-child(1) > th:nth-child(3)")).map(_.text()) shouldBe
      Some("Cysylltiad a r eiddo")
    Option(document.select("#assessments-table > thead > tr:nth-child(1) > th:nth-child(4)")).map(_.text()) shouldBe
      Some("Gwerth ardrethol")
  }

  it should "display the correct row information in welsh" in new ValuationsSetup {

    when(mockSessionRepository.get[AssessmentsPageSession](any(), any()))
      .thenReturn(Future.successful(Some(AssessmentsPageSession(PreviousPage.Dashboard))))

    lazy val as = apiAssessments(ownerAuthorisation)
    override def assessments: Future[Option[ApiAssessments]] = Future.successful(Some(as))

    val res = valuationsController.valuations(plSubId, owner = true)(welshFakeRequest)
    status(res) shouldBe OK

    val returnedHtml = contentAsString(res)
    val document: Document = Jsoup.parse(returnedHtml)

    Option(document.getElementsByClass("govuk-tag govuk-!-margin-right-2").first()).map(_.text()) shouldBe Some(
      "PRESENNOL")
    Option(document.getElementById("viewAssessmentLink-1234")).map(_.text()) shouldBe Some(
      "1 Ebrill 2017 i 1 Mehefin 2017")
    Option(document.select("#assessments-table > tbody > tr:nth-child(2) > td:nth-child(3)"))
      .map(_.text()) shouldBe Some("Perchennog")
  }

  it should "display the n/a rateable help for a historic property with n/a" in new ValuationsSetup {

    when(mockSessionRepository.get[AssessmentsPageSession](any(), any()))
      .thenReturn(Future.successful(Some(AssessmentsPageSession(PreviousPage.Dashboard))))

    lazy val as = apiAssessmentsNoRateableInfo(ownerAuthorisation)

    override def assessments: Future[Option[ApiAssessments]] = Future.successful(Some(as))

    val res = valuationsController.valuations(plSubId, owner = true)(request)
    status(res) shouldBe OK

    val returnedHtml = contentAsString(res)
    val document: Document = Jsoup.parse(returnedHtml)
    Option(document.select("#main-content > div > div > details:nth-child(2) > summary")).map(_.text()) shouldBe Some(
      "Help with rateable value not available (N/A)")

  }

}
