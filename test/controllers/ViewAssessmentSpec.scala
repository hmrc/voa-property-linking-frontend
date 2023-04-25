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

  "Viewing a detailed valuation" should "redirect to business rates valuation if it's viewable and user is owner" in new Viewable {
    private val res = TestAssessmentController.viewDetailedAssessment(
      submissionId = link.submissionId,
      authorisationId = link.authorisationId,
      assessmentRef = 1234L,
      owner = true,
    )(FakeRequest())
    status(res) shouldBe SEE_OTHER

    redirectLocation(res).value should endWith(
      s"/business-rates-valuation/property-link/${link.authorisationId}/valuations/1234?submissionId=${link.submissionId}")
  }

  "Viewing a detailed valuation" should "redirect to business rates valuation with fromValuation if it's viewable and user is owner" in new Viewable {
    private val res = TestAssessmentController.viewDetailedAssessment(
      submissionId = link.submissionId,
      authorisationId = link.authorisationId,
      assessmentRef = 1234L,
      owner = true,
      fromValuation = Some(98765L)
    )(FakeRequest())
    status(res) shouldBe SEE_OTHER

    redirectLocation(res).value should endWith(
      s"/business-rates-valuation/property-link/${link.authorisationId}/valuations/1234?submissionId=${link.submissionId}&fromValuation=98765")
  }

  "Viewing a detailed valuation" should "redirect to business rates valuation if it's viewable and user is agent" in new Viewable {
    private val res = TestAssessmentController.viewDetailedAssessment(
      submissionId = link.submissionId,
      authorisationId = link.authorisationId,
      assessmentRef = 1234L,
      owner = false,
    )(FakeRequest())
    status(res) shouldBe SEE_OTHER
    redirectLocation(res).value should endWith(
      s"/business-rates-valuation/property-link/clients/${link.authorisationId}/valuations/1234?submissionId=${link.submissionId}")
  }

  "Viewing a detailed valuation" should "redirect to business rates valuation with fromValuation if it's viewable and user is agent" in new Viewable {
    private val res = TestAssessmentController.viewDetailedAssessment(
      submissionId = link.submissionId,
      authorisationId = link.authorisationId,
      assessmentRef = 1234L,
      owner = false,
      fromValuation = Some(98765L)
    )(FakeRequest())
    status(res) shouldBe SEE_OTHER
    redirectLocation(res).value should endWith(
      s"/business-rates-valuation/property-link/clients/${link.authorisationId}/valuations/1234?submissionId=${link.submissionId}&fromValuation=98765")
  }

  "Viewing a detailed valuation" should "redirect to property linking if it's NOT viewable and user is owner" in new NotViewable {
    private val res = TestAssessmentController.viewDetailedAssessment(
      submissionId = link.submissionId,
      authorisationId = link.authorisationId,
      assessmentRef = 1234L,
      owner = true,
    )(FakeRequest())
    status(res) shouldBe SEE_OTHER

    redirectLocation(res).value should endWith(
      s"/business-rates-property-linking/my-organisation/property-link/${link.submissionId}/valuations/1234")
  }

  "Viewing a detailed valuation" should "redirect to property linking with fromValuation if it's NOT viewable and user is owner" in new NotViewable {
    private val res = TestAssessmentController.viewDetailedAssessment(
      submissionId = link.submissionId,
      authorisationId = link.authorisationId,
      assessmentRef = 1234L,
      owner = true,
      fromValuation = Some(98765L)
    )(FakeRequest())
    status(res) shouldBe SEE_OTHER

    redirectLocation(res).value should endWith(
      s"/business-rates-property-linking/my-organisation/property-link/${link.submissionId}/valuations/1234?fromValuation=98765")
  }

  "Viewing a detailed valuation" should "redirect to property linking if it's NOT viewable and user is Agent" in new NotViewable {
    private val res = TestAssessmentController.viewDetailedAssessment(
      submissionId = link.submissionId,
      authorisationId = link.authorisationId,
      assessmentRef = 1234L,
      owner = false,
    )(FakeRequest())
    status(res) shouldBe SEE_OTHER
    redirectLocation(res).value should endWith(
      s"/my-organisation/property-link/clients/all/${link.submissionId}/valuations/1234")
  }

  "Viewing a detailed valuation" should "redirect to property linking with fromValuation if it's NOT viewable and user is Agent" in new NotViewable {
    private val res = TestAssessmentController.viewDetailedAssessment(
      submissionId = link.submissionId,
      authorisationId = link.authorisationId,
      assessmentRef = 1234L,
      owner = false,
      fromValuation = Some(98765L)
    )(FakeRequest())
    status(res) shouldBe SEE_OTHER
    redirectLocation(res).value should endWith(
      s"/my-organisation/property-link/clients/all/${link.submissionId}/valuations/1234?fromValuation=98765")
  }

  trait Viewable {
    val link: PropertyLink = arbitrary[PropertyLink]

    //return TRUE - i.e. viewable
    when(mockBusinessRatesValuationConnector.isViewable(any(), any(), any())(any())).thenReturn(Future.successful(true))
    when(mockPropertyLinkService.getSingularPropertyLink(any(), any())(any())).thenReturn(Future.successful(Some(link)))
    StubPropertyLinkConnector.stubLink(link)
  }

  trait NotViewable {
    val link: PropertyLink = arbitrary[PropertyLink]

    //return FALSE - i.e. NOT viewable
    when(mockBusinessRatesValuationConnector.isViewable(any(), any(), any())(any()))
      .thenReturn(Future.successful(false))
    when(mockPropertyLinkService.getSingularPropertyLink(any(), any())(any())).thenReturn(Future.successful(Some(link)))
    StubPropertyLinkConnector.stubLink(link)
  }

}
