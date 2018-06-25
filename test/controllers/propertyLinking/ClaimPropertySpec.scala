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

package controllers.propertyLinking

import java.time.LocalDate

import config.ApplicationConfig
import connectors.propertyLinking.{PropertyLinkConnector}
import connectors.{Authenticated, EnvelopeConnector, EnvelopeMetadata}
import controllers.VoaPropertyLinkingSpec
import models._
import org.mockito.ArgumentMatchers.{eq => matching, _}
import org.mockito.Mockito._
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepo
import resources._
import utils.{HtmlPage, StubAuthentication, StubSubmissionIdConnector, StubWithLinkingSession}

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class ClaimPropertySpec extends VoaPropertyLinkingSpec with MockitoSugar {

  private lazy val testClaimProperty = new ClaimProperty(mockEnvelopes, StubAuthentication, StubSubmissionIdConnector,
    mockSessionRepo, new StubWithLinkingSession(mock[SessionRepo]), propertyLinkingConnector)

  lazy val submissionId: String = shortString
  lazy val accounts: Accounts = arbitrary[Accounts]
  lazy val anEnvelopeId = java.util.UUID.randomUUID().toString

  lazy val mockEnvelopes = {
    val f = mock[EnvelopeConnector]
    when(f.createEnvelope(any[EnvelopeMetadata])(any[HeaderCarrier]())).thenReturn(Future.successful(anEnvelopeId))
    f
  }

  lazy val mockSessionRepo = {
    val f = mock[SessionRepo]
    when(f.start(any())(any(), any())
    ).thenReturn(Future.successful(()))
    f
  }

  lazy val propertyLinkingConnector = mock[PropertyLinkConnector]

  "The claim property page" should "contain the claim property form" in {
    StubAuthentication.stubAuthenticationResult(Authenticated(accounts))
    StubSubmissionIdConnector.stubId(submissionId)

    val res = testClaimProperty.declareCapacity(positiveLong, shortString)(FakeRequest())
    status(res) mustBe OK

    val html = HtmlPage(res)
    html.mustContainRadioSelect("capacity", CapacityType.options)
    html.mustContainRadioSelect("interestedBefore2017", Seq("true", "false"))
    html.mustContainDateSelect("fromDate")
    html.mustContainRadioSelect("stillInterested", Seq("true", "false"))
    html.mustContainDateSelect("toDate")
  }

  it should "reject invalid form submissions" in {
    StubAuthentication.stubAuthenticationResult(Authenticated(accounts))
    StubSubmissionIdConnector.stubId(submissionId)

    val res = testClaimProperty.attemptLink(positiveLong, shortString)(FakeRequest())
    status(res) mustBe BAD_REQUEST
  }

  it should "redirect to the choose evidence page on valid submissions" in {
    StubAuthentication.stubAuthenticationResult(Authenticated(accounts))
    StubSubmissionIdConnector.stubId(submissionId)

    val res = testClaimProperty.attemptLink(positiveLong, shortString)(FakeRequest().withFormUrlEncodedBody(
      "capacity" -> "OWNER",
      "interestedBefore2017" -> "true",
      "stillInterested" -> "true"
    ))
    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(routes.ChooseEvidence.show.url)
  }

  it should "initialise the linking session on submission" in {
    StubAuthentication.stubAuthenticationResult(Authenticated(accounts))
    StubSubmissionIdConnector.stubId(submissionId)

    val uarn: Long = positiveLong
    val address: String = shortString

    val declaration: CapacityDeclaration = CapacityDeclaration(Owner, false, Some(LocalDate.of(2017, 4, 2)),
      false, Some(LocalDate.of(2017, 4, 5)))

    val res = testClaimProperty.attemptLink(uarn, address)(FakeRequest().withFormUrlEncodedBody(
      "capacity" -> declaration.capacity.toString,
      "interestedBefore2017" -> declaration.interestedBefore2017.toString,
      "fromDate.year" -> declaration.fromDate.fold("")(_.getYear.toString),
      "fromDate.month" -> declaration.fromDate.fold("")(_.getMonthValue.toString),
      "fromDate.day" -> declaration.fromDate.fold("")(_.getDayOfMonth.toString),
      "stillInterested" -> declaration.stillInterested.toString,
      "toDate.year" -> declaration.fromDate.fold("")(_.getYear.toString),
      "toDate.month" -> declaration.toDate.fold("")(_.getMonthValue.toString),
      "toDate.day" -> declaration.toDate.fold("")(_.getDayOfMonth.toString)
    ))

    status(res) mustBe SEE_OTHER
    verify(mockEnvelopes, atLeastOnce()).createEnvelope(any[EnvelopeMetadata])(any[HeaderCarrier])

    verify(mockSessionRepo, times(2)).start(any())(any(), any())
  }
}
