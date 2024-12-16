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
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import utils._

import scala.concurrent.Future

class ValuationsControllerSpec extends VoaPropertyLinkingSpec {

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait Setup {

    val plSubId = "pl-submission-id"
    val previousPage = SelectedClient

    when(mockSessionRepository.start(any())(any(), any())).thenReturn(Future.successful((): Unit))
    when(mockSessionRepository.get[AssessmentsPageSession](any(), any()))
      .thenReturn(Future.successful(Some(assessmentPageSession)))
    when(mockCustomErrorHandler.notFoundTemplate(any())).thenReturn(Future.successful(Html("not found")))
    when(mockAgentRelationshipService.getMyOrganisationAgents()(any()))
      .thenReturn(Future.successful(organisationsAgentsListWithOneAgent))

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
      s"/business-rates-property-linking/property-link/$plSubId/assessments?owner=false"
    )

    verify(mockSessionRepository).start(mEq(AssessmentsPageSession(previousPage)))(any(), any())
  }

  it should "save when called by an agent - with cache id" in new Setup {
    val res = valuationsController
      .savePreviousPage(previousPage = previousPage.toString, submissionId = plSubId, owner = false)(request)

    status(res) shouldBe SEE_OTHER
    header(LOCATION, res) shouldBe Some(
      s"/business-rates-property-linking/property-link/$plSubId/assessments?owner=false"
    )

    verify(mockSessionRepository).start(mEq(AssessmentsPageSession(previousPage)))(any(), any())
  }
}
