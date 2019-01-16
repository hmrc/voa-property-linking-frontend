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

import java.time.LocalDate

import actions.BasicAuthenticatedRequest
import config.ApplicationConfig
import connectors.{Authenticated, DVRCaseManagementConnector, SubmissionIdConnector}
import models._
import models.dvr.{DetailedValuationRequest, DetailedValuationRequestTypes}
import org.mockito.ArgumentMatchers._
import org.mockito.ArgumentMatchers.{eq => matching}
import org.mockito.Mockito._
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
import resources._
import utils._

import scala.concurrent.Future
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}

class RequestDetailedValuationSpec extends VoaPropertyLinkingSpec with MockitoSugar {

  private object TestAssessments extends Assessments( StubPropertyLinkConnector,
    StubAuthentication, mockSubmissionIds, mockDvrCaseManagement, StubBusinessRatesValuation, StubBusinessRatesAuthorisation)

  lazy val mockDvrCaseManagement = {
    val m = mock[DVRCaseManagementConnector]
    when(m.requestDetailedValuation(any[DetailedValuationRequest])(any[HeaderCarrier])).thenReturn(Future.successful(()))
    m
  }

  lazy val mockSubmissionIds = {
    val m = mock[SubmissionIdConnector]
    when(m.get(matching("EMAIL"))(any[HeaderCarrier])).thenReturn(Future.successful("EMAIL123"))
    when(m.get(matching("POST"))(any[HeaderCarrier])).thenReturn(Future.successful("POST123"))
    m
  }

  val accounts: Accounts = arbitrary[Accounts]
  val authId: Long = positiveLong
  val assessmentRef: Long = positiveLong
  val baRef: String = shortString

  "The request detailed valuation page" should "give the user a choice between receiving the valuation in the post or by email" in {
    StubAuthentication.stubAuthenticationResult(Authenticated(accounts))
    val res = TestAssessments.requestDetailedValuation(authId, assessmentRef, baRef)(FakeRequest())

    status(res) mustBe OK

    val html = HtmlPage(res)
    html.mustContainRadioSelect("requestType", DetailedValuationRequestTypes.options)
  }

  it should "display the duplicated request page if user has already requested a DVR within 14 days" in {
   StubAuthentication.stubAuthenticationResult(Authenticated(accounts))
   val res = TestAssessments.duplicateRequestDetailedValuation(authId, assessmentRef)(FakeRequest())

    status(res) mustBe OK

    val html = contentAsString(res)
    html must include ("You’ve already requested this detailed valuation.")
    html must include ("We’ve received your request for a copy of the detailed valuation for this property in the last 14 days.")

  }

  it should "require the user to choose how they want to receive the detailed valuation" in {
    StubAuthentication.stubAuthenticationResult(Authenticated(accounts))
    val res = TestAssessments.detailedValuationRequested(authId, assessmentRef, baRef)(FakeRequest())

    status(res) mustBe BAD_REQUEST

    val html = HtmlPage(res)
    html.mustContainFieldErrors("requestType" -> "Please select an option")
  }

  it should """redirect the user to the duplicate request page if they choose to receive the detailed valuation by email""" in {
    StubAuthentication.stubAuthenticationResult(Authenticated(accounts))
    when(mockDvrCaseManagement.dvrExists(any(), any())(any[HeaderCarrier])).thenReturn(Future.successful(true))
    val res = TestAssessments.detailedValuationRequested(authId, assessmentRef, baRef)(FakeRequest().withFormUrlEncodedBody("requestType" -> "email"))

    await(res)
    verify(mockSubmissionIds, times(1)).get(matching("EMAIL"))(any[HeaderCarrier])
    verify(mockDvrCaseManagement, times(0)).requestDetailedValuation(any())(any[HeaderCarrier])

    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(routes.Assessments.duplicateRequestDetailedValuation(authId, assessmentRef).url)
  }

  it should """redirect the user to the duplicate request page if they choose to receive the detailed valuation by post""" in {
    StubAuthentication.stubAuthenticationResult(Authenticated(accounts))
    when(mockDvrCaseManagement.dvrExists(any(), any())(any[HeaderCarrier])).thenReturn(Future.successful(true))
    val res = TestAssessments.detailedValuationRequested(authId, assessmentRef, baRef)(FakeRequest().withFormUrlEncodedBody("requestType" -> "post"))

    await(res)
    verify(mockSubmissionIds, times(1)).get(matching("POST"))(any[HeaderCarrier])
    verify(mockDvrCaseManagement, times(0)).requestDetailedValuation(any())(any[HeaderCarrier])

    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(routes.Assessments.duplicateRequestDetailedValuation(authId, assessmentRef).url)
  }
  it should """generate a submission ID starting with "EMAIL" if they choose to receive the detailed valuation by email""" in {
    StubAuthentication.stubAuthenticationResult(Authenticated(accounts))
    when(mockDvrCaseManagement.dvrExists(any(), any())(any[HeaderCarrier])).thenReturn(Future.successful(false))
    val res = TestAssessments.detailedValuationRequested(authId, assessmentRef, baRef)(FakeRequest().withFormUrlEncodedBody("requestType" -> "email"))

    await(res)
    verify(mockSubmissionIds, times(2)).get(matching("EMAIL"))(any[HeaderCarrier])

    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(routes.Assessments.dvRequestConfirmation("EMAIL123", authId).url)
  }

  it should """generate a submission ID starting with "POST" if they choose to receive the detailed valuation by post""" in {
    StubAuthentication.stubAuthenticationResult(Authenticated(accounts))
      val res = TestAssessments.detailedValuationRequested(authId, assessmentRef, baRef)(FakeRequest().withFormUrlEncodedBody("requestType" -> "post"))
      when(mockDvrCaseManagement.dvrExists(any(), any())(any[HeaderCarrier])).thenReturn(Future.successful(false))

      await(res)
      verify(mockSubmissionIds, times(2)).get(matching("POST"))(any[HeaderCarrier])

      status(res) mustBe SEE_OTHER
      redirectLocation(res) mustBe Some(routes.Assessments.dvRequestConfirmation("POST123", authId).url)
    }

  it should "confirm that the user will receive the detailed valuation by email if that is their preference" in {
    StubAuthentication.stubAuthenticationResult(Authenticated(accounts))
    StubPropertyLinkConnector.stubLink(PropertyLink(authId, "SomeId", 0l, 0l, "address", Capacity(Occupier, LocalDate.now(), Some(LocalDate.now())), LocalDate.now(), true, Nil, Nil))
    val res = TestAssessments.dvRequestConfirmation("EMAIL123", authId)(FakeRequest())

    status(res) mustBe OK

    val html = contentAsString(res)
    html must include ("Your reference number is EMAIL123")
    html must include ("We’ll send this to you by email")
  }

  it should "confirm that the user will receive the detailed valuation by post if that is their preference" in {
    StubAuthentication.stubAuthenticationResult(Authenticated(accounts))
    StubPropertyLinkConnector.stubLink(PropertyLink(authId, "SomeId", 0l, 0l, "address", Capacity(Occupier, LocalDate.now(), Some(LocalDate.now())), LocalDate.now(), true, Nil, Nil))
    val res = TestAssessments.dvRequestConfirmation("POST123", authId)(FakeRequest())

    status(res) mustBe OK

    val html = contentAsString(res)
    html must include ("Your reference number is POST123")
    html must include ("We’ll send this to you by post")
  }

  it should "return a 404 response when the action throws a NotFoundException" in {
    when(mockDvrCaseManagement.dvrExists(any(), any())(any[HeaderCarrier])).thenReturn(Future.successful(false))
    val res = TestAssessments.dvRequestConfirmation("1234566", authId)(FakeRequest())

    status(res) mustBe NOT_FOUND
  }

  "startChallengeFromDVR" should "display 'Challenge the Valuation' page" in {
    StubAuthentication.stubAuthenticationResult(Authenticated(accounts))
    val res = TestAssessments.startChallengeFromDVR(authId, assessmentRef, baRef)(FakeRequest())

    status(res) mustBe OK

    val html = contentAsString(res)
    html must include ("Challenge this valuation")
  }

}
