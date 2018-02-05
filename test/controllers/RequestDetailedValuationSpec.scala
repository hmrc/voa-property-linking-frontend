/*
 * Copyright 2018 HM Revenue & Customs
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

import config.ApplicationConfig
import connectors.{Authenticated, DVRCaseManagementConnector, SubmissionIdConnector}
import models._
import org.mockito.ArgumentMatchers._
import org.mockito.ArgumentMatchers.{eq => matching}
import org.mockito.Mockito._
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
import resources._
import utils.{HtmlPage, StubAuthentication, StubBusinessRatesValuation, StubPropertyLinkConnector}

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class RequestDetailedValuationSpec extends ControllerSpec with MockitoSugar {

  private object TestAssessments extends Assessments(app.injector.instanceOf[ApplicationConfig], StubPropertyLinkConnector,
    StubAuthentication, mockSubmissionIds, mockDvrCaseManagement, StubBusinessRatesValuation)

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

  it should "require the user to choose how they want to receive the detailed valuation" in {
    StubAuthentication.stubAuthenticationResult(Authenticated(accounts))
    val res = TestAssessments.detailedValuationRequested(authId, assessmentRef, baRef)(FakeRequest())

    status(res) mustBe BAD_REQUEST

    val html = HtmlPage(res)
    html.mustContainFieldErrors("requestType" -> "Please select an option")
  }

  it should """generate a submission ID starting with "EMAIL" if they choose to receive the detailed valuation by email""" in {
    StubAuthentication.stubAuthenticationResult(Authenticated(accounts))
    val res = TestAssessments.detailedValuationRequested(authId, assessmentRef, baRef)(FakeRequest().withFormUrlEncodedBody("requestType" -> "email"))

    await(res)
    verify(mockSubmissionIds, times(1)).get(matching("EMAIL"))(any[HeaderCarrier])

    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(routes.Assessments.dvRequestConfirmation("EMAIL123", authId).url)
  }

  it should """generate a submission ID starting with "POST" if they choose to receive the detailed valuation by post""" in {
    StubAuthentication.stubAuthenticationResult(Authenticated(accounts))
    val res = TestAssessments.detailedValuationRequested(authId, assessmentRef, baRef)(FakeRequest().withFormUrlEncodedBody("requestType" -> "post"))

    await(res)
    verify(mockSubmissionIds, times(1)).get(matching("POST"))(any[HeaderCarrier])

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
}
