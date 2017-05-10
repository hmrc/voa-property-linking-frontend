/*
 * Copyright 2017 HM Revenue & Customs
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

import connectors.Authenticated
import connectors.fileUpload.{EnvelopeMetadata, FileUploadConnector}
import models._
import org.joda.time.LocalDate
import org.mockito.ArgumentMatchers.{eq => matching, _}
import org.mockito.Mockito._
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepo
import resources._
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.{HtmlPage, StubAuthentication, StubSubmissionIdConnector}

import scala.concurrent.Future

class ClaimPropertySpec extends ControllerSpec with MockitoSugar {

  private class TestClaimProperty(fileUploadConnector: FileUploadConnector,
                              sessionRepository: SessionRepo) extends ClaimProperty(fileUploadConnector, sessionRepository) {
    override lazy val authenticated = StubAuthentication
    override lazy val submissionIdConnector = StubSubmissionIdConnector
  }
  private val testClaimProperty = new TestClaimProperty(mockFileUploads, mockSessionRepo)

  lazy val submissionId: String = shortString
  lazy val accounts: Accounts = arbitrary[Accounts]
  lazy val anEnvelopeId = java.util.UUID.randomUUID().toString

  lazy val mockFileUploads = {
    val f = mock[FileUploadConnector]
    when(f.createEnvelope(
      any[EnvelopeMetadata])(any[HeaderCarrier]())
    ).thenReturn(Future.successful(anEnvelopeId))
    f
  }
  lazy val mockSessionRepo = {
    val f = mock[SessionRepo]
    when(f.start(any())(any(), any())
    ).thenReturn(Future.successful(()))
    f
  }

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

    val declaration: CapacityDeclaration = CapacityDeclaration(Owner, false, Option(LocalDate.parse("2017-04-2")),
      false, Option(LocalDate.parse("2017-04-5")))

    val res = testClaimProperty.attemptLink(uarn, address)(FakeRequest().withFormUrlEncodedBody(
      "capacity" -> declaration.capacity.toString,
      "interestedBefore2017" -> declaration.interestedBefore2017.toString,
      "fromDate.year" -> declaration.fromDate.fold("")(_.getYear.toString),
      "fromDate.month" -> declaration.fromDate.fold("")(_.getMonthOfYear.toString),
      "fromDate.day" -> declaration.fromDate.fold("")(_.getDayOfMonth.toString),
      "stillInterested" -> declaration.stillInterested.toString,
      "toDate.year" -> declaration.fromDate.fold("")(_.getYear.toString),
      "toDate.month" -> declaration.toDate.fold("")(_.getMonthOfYear.toString),
      "toDate.day" -> declaration.toDate.fold("")(_.getDayOfMonth.toString)
    ))

    status(res) mustBe SEE_OTHER
    verify(mockSessionRepo, times(2)).start(any())(any(), any())
  }
}
