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

package uk.gov.hmrc.propertylinking.controllers.valuations

import actions.assessments.WithAssessmentsPageSessionRefiner
import controllers.VoaPropertyLinkingSpec
import models.ApiAssessment.AssessmentWithFromDate
import models.assessments.AssessmentsPageSession
import models.assessments.PreviousPage.SelectedClient
import models.{ApiAssessment, ApiAssessments, ClientPropertyLink}
import org.mockito.ArgumentMatchers.{any, eq => mEq}
import org.mockito.Mockito._
import org.scalacheck.Arbitrary.arbitrary
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
      withAssessmentsPageSession = new WithAssessmentsPageSessionRefiner(mockCustomErrorHandler, mockSessionRepository),
      controllerComponents = stubMessagesControllerComponents()
    )
  }

  trait ValuationsSetup extends Setup {

    def pLink: Future[Option[ApiAssessments]]

    when(mockPropertyLinkConnector.getOwnerAssessments(any())(any())).thenReturn(pLink)
    when(mockPropertyLinkConnector.getClientAssessments(any())(any())).thenReturn(pLink)
  }

  behavior of "saving the previous page"

  it should "save when called by the property link owner (IP)" in new Setup {
    val res = valuationsController
      .savePreviousPage(previousPage = previousPage.toString, submissionId = plSubId, owner = true)(request)

    status(res) mustBe SEE_OTHER
    header(LOCATION, res) mustBe Some(s"/business-rates-property-linking/property-link/$plSubId/assessments")

    verify(mockSessionRepository).start(mEq(AssessmentsPageSession(previousPage)))(any(), any())
  }

  it should "save when called by an agent" in new Setup {
    val res = valuationsController
      .savePreviousPage(previousPage = previousPage.toString, submissionId = plSubId, owner = false)(request)

    status(res) mustBe SEE_OTHER
    header(LOCATION, res) mustBe Some(
      s"/business-rates-property-linking/property-link/$plSubId/assessments?owner=false")

    verify(mockSessionRepository).start(mEq(AssessmentsPageSession(previousPage)))(any(), any())
  }

  behavior of "valuations page"

  it should "return 404 when no assessments for given property link are found" in new ValuationsSetup {

    override def pLink: Future[Option[ApiAssessments]] =
      Future.successful(Some(apiAssessments(ownerAuthorisation).copy(assessments = List.empty)))

    val res = valuationsController.valuations(plSubId, owner = true)(request)

    status(res) mustBe NOT_FOUND
  }

  it should "return 200 with assessments for OWNER" in new ValuationsSetup {
    lazy val assessments = apiAssessments(ownerAuthorisation)
    override def pLink: Future[Option[ApiAssessments]] = Future.successful(Some(assessments))

    val res = valuationsController.valuations(plSubId, owner = true)(request)
    status(res) mustBe OK
  }

  it should "return 200 with assessments sorted by currentFromDate DESC for OWNER" in new ValuationsSetup {
    lazy val assessments = apiAssessments(ownerAuthorisation.copy(status = "PENDING"))
    override def pLink: Future[Option[ApiAssessments]] = Future.successful(Some(assessments))

    val res = valuationsController.valuations(plSubId, owner = true)(request)
    status(res) mustBe OK

    val sortedAssessments: List[ApiAssessment] =
      valuationsController.assessmentsWithLinks(assessments, plSubId, owner = true).map(_._2).toList

    inside(sortedAssessments) {
      case AssessmentWithFromDate(fromDate1) :: AssessmentWithFromDate(fromDate2) :: Nil =>
        //first assessment in the list must have a "later" start date than the next one
        fromDate1.toEpochDay must be > fromDate2.toEpochDay
    }
  }

  it should "return 200 with assessments sorted by currentFromDate DESC for AGENT" in new ValuationsSetup {
    lazy val assessments = apiAssessments(ownerAuthorisation.copy(status = "PENDING"))
    override def pLink: Future[Option[ApiAssessments]] = Future.successful(Some(assessments))

    val clientProperty: ClientPropertyLink = arbitrary[ClientPropertyLink]

    when(mockPropertyLinkConnector.clientPropertyLink(mEq(plSubId))(any()))
      .thenReturn(Future.successful(Some(clientProperty)))

    val res = valuationsController.valuations(plSubId, owner = false)(request)
    status(res) mustBe OK

    val sortedAssessments: List[ApiAssessment] =
      valuationsController.assessmentsWithLinks(assessments, plSubId, owner = false).map(_._2).toList

    inside(sortedAssessments) {
      case AssessmentWithFromDate(fromDate1) :: AssessmentWithFromDate(fromDate2) :: Nil =>
        //first assessment in the list must have a "later" start date than the next one
        fromDate1.toEpochDay must be > fromDate2.toEpochDay
    }
  }

}
