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

package controllers

import models._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.OptionValues
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils._

import scala.concurrent.Future

class ViewAssessmentSpec extends VoaPropertyLinkingSpec with OptionValues {

  private object TestAssessmentController
      extends Assessments(
        mockCustomErrorHandler,
        mockPropertyLinkService,
        preAuthenticatedActionBuilders(),
        mockBusinessRatesValuationConnector,
        stubMessagesControllerComponents()
      )

  "viewOwnerSummary" must "redirect to business-rates-valuation view owner summary details" in {
    val res = TestAssessmentController.viewOwnerSummary(123L, true)(FakeRequest())

    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(
      "http://localhost:9537/business-rates-valuation/property-link/123/valuation/summary")
  }

  "viewClientSummary" must "redirect to business-rates-valuation view client summary details" in {
    val res = TestAssessmentController.viewClientSummary(123L, true)(FakeRequest())

    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(
      "http://localhost:9537/business-rates-valuation/property-link/clients/all/123/valuation/summary")
  }

  "Viewing a detailed valuation" must "redirect to business rates valuation if it's viewable and user is owner" in {
    val link: PropertyLink = arbitrary[PropertyLink]

    //return TRUE - i.e. viewable
    when(mockBusinessRatesValuationConnector.isViewable(any(), any(), any())(any())).thenReturn(Future.successful(true))

    when(mockPropertyLinkService.getSingularPropertyLink(any(), any())(any())).thenReturn(Future.successful(Some(link)))

    StubPropertyLinkConnector.stubLink(link)

    val res = TestAssessmentController.viewDetailedAssessment(
      submissionId = link.submissionId,
      authorisationId = link.authorisationId,
      assessmentRef = 1234L,
      baRef = "BA Ref",
      owner = true,
    )(FakeRequest())
    status(res) mustBe SEE_OTHER

    redirectLocation(res).value must endWith(
      s"/business-rates-valuation/property-link/${link.authorisationId}/valuations/1234?submissionId=${link.submissionId}")
  }

  "Viewing a detailed valuation" must "redirect to business rates valuation if it's viewable and user is agent" in {
    val link: PropertyLink = arbitrary[PropertyLink]

    //return TRUE - i.e. viewable
    when(mockBusinessRatesValuationConnector.isViewable(any(), any(), any())(any())).thenReturn(Future.successful(true))

    when(mockPropertyLinkService.getSingularPropertyLink(any(), any())(any())).thenReturn(Future.successful(Some(link)))

    StubPropertyLinkConnector.stubLink(link)

    val res = TestAssessmentController.viewDetailedAssessment(
      submissionId = link.submissionId,
      authorisationId = link.authorisationId,
      assessmentRef = 1234L,
      baRef = "BA Ref",
      owner = false,
    )(FakeRequest())
    status(res) mustBe SEE_OTHER

    redirectLocation(res).value must endWith(
      s"/business-rates-valuation/property-link/clients/${link.authorisationId}/valuations/1234?submissionId=${link.submissionId}")
  }

  "Viewing a detailed valuation" must "redirect to property linking if it's NOT viewable and user is owner" in {
    val link: PropertyLink = arbitrary[PropertyLink]

    //return FALSE - i.e. NOT viewable
    when(mockBusinessRatesValuationConnector.isViewable(any(), any(), any())(any()))
      .thenReturn(Future.successful(false))

    when(mockPropertyLinkService.getSingularPropertyLink(any(), any())(any())).thenReturn(Future.successful(Some(link)))

    StubPropertyLinkConnector.stubLink(link)

    val res = TestAssessmentController.viewDetailedAssessment(
      submissionId = link.submissionId,
      authorisationId = link.authorisationId,
      assessmentRef = 1234L,
      baRef = "BA Ref",
      owner = true,
    )(FakeRequest())
    status(res) mustBe SEE_OTHER

    redirectLocation(res).value must endWith(
      s"/business-rates-property-linking/my-organisation/property-link/${link.submissionId}/valuations/1234?uarn=${link.uarn}")
  }

  "Viewing a detailed valuation" must "redirect to business rates valuation if it's NOT viewable and user is Agent" in {
    val link: PropertyLink = arbitrary[PropertyLink]

    //return FALSE - i.e. NOT viewable
    when(mockBusinessRatesValuationConnector.isViewable(any(), any(), any())(any())).thenReturn(Future.successful(true))

    when(mockPropertyLinkService.getSingularPropertyLink(any(), any())(any())).thenReturn(Future.successful(Some(link)))

    StubPropertyLinkConnector.stubLink(link)

    val res = TestAssessmentController.viewDetailedAssessment(
      submissionId = link.submissionId,
      authorisationId = link.authorisationId,
      assessmentRef = 1234L,
      baRef = "BA Ref",
      owner = false,
    )(FakeRequest())
    status(res) mustBe SEE_OTHER

    redirectLocation(res).value must endWith(
      s"/business-rates-valuation/property-link/clients/${link.authorisationId}/valuations/1234?submissionId=${link.submissionId}")
  }

}
