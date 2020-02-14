/*
 * Copyright 2020 HM Revenue & Customs
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

import connectors.propertyLinking.PropertyLinkConnector
import controllers.VoaPropertyLinkingSpec
import models._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalacheck.Arbitrary.arbitrary
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepo
import resources._
import uk.gov.hmrc.http.HeaderCarrier
import utils.{HtmlPage, StubSubmissionIdConnector, StubWithLinkingSession}

import scala.concurrent.Future

class ClaimPropertySpec extends VoaPropertyLinkingSpec {

  private lazy val testClaimProperty = new ClaimProperty(
    mockCustomErrorHandler,
    StubSubmissionIdConnector,
    mockSessionRepo,
    preAuthenticatedActionBuilders(),
    new StubWithLinkingSession(mock[SessionRepo]),
    propertyLinkingConnector,
    configuration
  )

  lazy val submissionId: String = shortString
  override val testAccounts: Accounts = arbitrary[Accounts]
  lazy val anEnvelopeId = java.util.UUID.randomUUID().toString

  lazy val mockSessionRepo = {
    val f = mock[SessionRepo]
    when(f.start(any())(any(), any())).thenReturn(Future.successful(()))
    f
  }

  lazy val propertyLinkingConnector = mock[PropertyLinkConnector]

  implicit val hc = HeaderCarrier()

  "The claim property page" should "contain the claim property form" in {
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
    StubSubmissionIdConnector.stubId(submissionId)

    val res = testClaimProperty.attemptLink(positiveLong, shortString)(FakeRequest())
    status(res) mustBe BAD_REQUEST
  }

  it should "redirect to the choose evidence page on valid submissions" in {
    StubSubmissionIdConnector.stubId(submissionId)

    val res = testClaimProperty.attemptLink(positiveLong, shortString)(
      FakeRequest().withFormUrlEncodedBody(
        "capacity"             -> "OWNER",
        "interestedBefore2017" -> "true",
        "stillInterested"      -> "true"
      ))
    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(routes.ChooseEvidence.show.url)
  }

  it should "initialise the linking session on submission" in {
    StubSubmissionIdConnector.stubId(submissionId)

    val uarn: Long = positiveLong
    val address: String = shortString

    val declaration: CapacityDeclaration =
      CapacityDeclaration(Owner, false, Some(LocalDate.of(2017, 4, 2)), false, Some(LocalDate.of(2017, 4, 5)))

    val res = testClaimProperty.attemptLink(uarn, address)(
      FakeRequest().withFormUrlEncodedBody(
        "capacity"             -> declaration.capacity.toString,
        "interestedBefore2017" -> declaration.interestedBefore2017.toString,
        "fromDate.year"        -> declaration.fromDate.fold("")(_.getYear.toString),
        "fromDate.month"       -> declaration.fromDate.fold("")(_.getMonthValue.toString),
        "fromDate.day"         -> declaration.fromDate.fold("")(_.getDayOfMonth.toString),
        "stillInterested"      -> declaration.stillInterested.toString,
        "toDate.year"          -> declaration.fromDate.fold("")(_.getYear.toString),
        "toDate.month"         -> declaration.toDate.fold("")(_.getMonthValue.toString),
        "toDate.day"           -> declaration.toDate.fold("")(_.getDayOfMonth.toString)
      ))

    status(res) mustBe SEE_OTHER

    verify(mockSessionRepo, times(2)).start(any())(any(), any())
  }

  "show" must "redirect the user to vmv search for property page" in {
    StubSubmissionIdConnector.stubId(submissionId)

    val res = testClaimProperty.show()(FakeRequest())

    status(res) mustBe SEE_OTHER

    redirectLocation(res) mustBe Some("http://localhost:9300/business-rates-find/search")
  }

}
