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

package controllers.propertyLinking

import java.time.LocalDate

import connectors.propertyLinking.PropertyLinkConnector
import connectors.vmv.VmvConnector
import controllers.VoaPropertyLinkingSpec
import models._
import models.properties.PropertyHistory
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalacheck.Arbitrary.arbitrary
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import repositories.SessionRepo
import utils.{HtmlPage, StubSubmissionIdConnector, StubWithLinkingSession, _}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class ClaimPropertyRelationshipControllerSpec extends VoaPropertyLinkingSpec {

  implicit val hc = HeaderCarrier()
  private val mockRelationshipToPropertyPage = mock[views.html.propertyLinking.relationshipToProperty]
  private lazy val testClaimProperty = new ClaimPropertyRelationshipController(
    mockCustomErrorHandler,
    StubSubmissionIdConnector,
    mockSessionRepo,
    preAuthenticatedActionBuilders(),
    new StubWithLinkingSession(mock[SessionRepo]),
    propertyLinkingConnector,
    vmvConnector,
    configuration,
    mockRelationshipToPropertyPage
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

  lazy val vmvConnector = {
    val vmvConnector = mock[VmvConnector]
    when(vmvConnector.getPropertyHistory(any())(any())).thenReturn(Future.successful(propertyHistory))
    vmvConnector
  }

  "The claim property relationship page" should "return valid page" in {
    StubSubmissionIdConnector.stubId(submissionId)

    when(mockRelationshipToPropertyPage.apply(any(), any(), any())(any(), any(), any()))
      .thenReturn(Html("claim property relationship page"))

    val res = testClaimProperty.showRelationship(positiveLong)(FakeRequest())
    status(res) mustBe OK

    val html = HtmlPage(res)
    html.mustContainText("claim property relationship page")

  }

  "The claim property relationship page on client behalf" should "return valid page" in {
    StubSubmissionIdConnector.stubId(submissionId)

    when(mockRelationshipToPropertyPage.apply(any(), any(), any())(any(), any(), any()))
      .thenReturn(Html("claim property relationship page on client behalf"))

    val res = testClaimProperty
      .showRelationship(positiveLong, Some(ClientDetails(positiveLong, shortString)))(FakeRequest())
    status(res) mustBe OK

    val html = HtmlPage(res)
    html.mustContainText("claim property relationship page on client behalf")
  }

  it should "contain link back to business-rates-find if thats where the request came from" in {

    val res =
      testClaimProperty.showRelationship(positiveLong, Some(ClientDetails(positiveLong, shortString)))(
        FakeRequest().withHeaders(
          ("referer", "http://localhost:9542/business-rates-find/summary/10361354?uarn=156039182")))
    status(res) mustBe OK

    val html = HtmlPage(res)

    html.contain("http://localhost:9542/business-rates-find/summary/10361354?uarn=156039182")
  }

  it should "reject invalid form submissions" in {
    StubSubmissionIdConnector.stubId(submissionId)

    val res = testClaimProperty.submitRelationship(positiveLong)(FakeRequest())
    status(res) mustBe BAD_REQUEST
  }

  it should "redirect to the claim relationship page on valid submissions" in {
    StubSubmissionIdConnector.stubId(submissionId)

    val res = testClaimProperty.submitRelationship(positiveLong)(
      FakeRequest().withFormUrlEncodedBody(
        "capacity" -> "OWNER"
      ))
    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(routes.ClaimPropertyOwnershipController.showOwnership.url)
  }

  it should "initialise the linking session on submission" in {
    StubSubmissionIdConnector.stubId(submissionId)

    val uarn: Long = positiveLong
    val address: String = shortString

    val res = testClaimProperty.submitRelationship(uarn)(
      FakeRequest().withFormUrlEncodedBody(
        "capacity" -> Owner.toString
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
