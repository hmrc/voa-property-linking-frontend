/*
 * Copyright 2022 HM Revenue & Customs
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

import cats.data.EitherT
import cats.instances.future._
import controllers.VoaPropertyLinkingSpec
import models.{CapacityType, Occupier, Owner, OwnerOccupier}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import repositories.SessionRepo
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.propertylinking.exceptions.attachments._
import utils.{HtmlPage, _}

import java.time.LocalDate
import scala.concurrent.Future

class DeclarationControllerSpec extends VoaPropertyLinkingSpec {
  implicit val hc = HeaderCarrier()
  private val mockLinkingRequestSubmittedView = mock[views.html.linkingRequestSubmitted]

  trait Setup {
    def testDeclarationController(earliestStartDate: LocalDate) =
      new DeclarationController(
        errorHandler = mockCustomErrorHandler,
        propertyLinkService = mockPropertyLinkingService,
        sessionRepository = mockSessionRepo,
        authenticatedAction = preAuthenticatedActionBuilders(isAgent),
        withLinkingSession = preEnrichedActionRefiner(
          evidenceData = uploadEvidenceData,
          relationshipCapacity = propertyRelationship,
          userIsAgent = isAgent,
          earliestStartDate = earliestStartDate),
        declarationView = declarationView,
        linkingRequestSubmittedView = mockLinkingRequestSubmittedView
      )

    lazy val mockSessionRepo = {
      val f = mock[SessionRepo]
      when(f.start(any())(any(), any())).thenReturn(Future.successful(()))

      when(f.remove()(any())).thenReturn(Future.successful(()))
      f
    }

    lazy val envelopeId: String = shortString

    val isAgent: Boolean = true
    val propertyRelationship: CapacityType = Owner
  }

  "The declaration page with earliest start date is not in future" should "return valid page" in new Setup {
    val res = testDeclarationController(earliestEnglishStartDate).show()(FakeRequest())

    status(res) shouldBe OK
    val html = HtmlPage(res)

    html.titleShouldMatch("Check and confirm your details - Valuation Office Agency - GOV.UK")
    html.verifyElementText("page-header", "Check and confirm your details")
    html.verifyElementText("caption", "Add a property")
    html.verifyElementTextByAttribute("id", "start-date-heading", "Started")
  }

  "The declaration page with earliest start date in future" should "return valid page" in new Setup {
    val res = testDeclarationController(LocalDate.now.plusYears(1)).show()(FakeRequest())

    status(res) shouldBe OK
    val html = HtmlPage(res)

    html.titleShouldMatch("Check and confirm your details - Valuation Office Agency - GOV.UK")
    html.verifyElementText("page-header", "Check and confirm your details")
    html.verifyElementText("caption", "Add a property")
    html.shouldNotContainText("Started")
    html.shouldNotContainText("Do you still")
    html.shouldNotContainText("Does your client still")
    html.shouldNotContainText("Last day as")
  }

  "The declaration page" should "display the correct summary list keys for an owner IP" in new Setup {
    override val isAgent: Boolean = false

    val res: Future[Result] = testDeclarationController(earliestEnglishStartDate).show()(FakeRequest())

    status(res) shouldBe OK
    val html: HtmlPage = HtmlPage(res)

    html.verifyElementTextByAttribute("id", "still-owned-heading", "Do you still own the property?")
    html.verifyElementTextByAttribute("id", "end-date-heading", "Last day as owner")
  }

  it should "display the correct summary list keys for an occupier IP" in new Setup {
    override val isAgent: Boolean = false
    override val propertyRelationship: CapacityType = Occupier

    val res: Future[Result] = testDeclarationController(earliestEnglishStartDate).show()(FakeRequest())

    status(res) shouldBe OK
    val html: HtmlPage = HtmlPage(res)

    html.verifyElementTextByAttribute("id", "still-owned-heading", "Do you still occupy the property?")
    html.verifyElementTextByAttribute("id", "end-date-heading", "Last day as occupier")
  }

  it should "display the correct summary list keys for an owner and occupier IP" in new Setup {
    override val isAgent: Boolean = false
    override val propertyRelationship: CapacityType = OwnerOccupier

    val res: Future[Result] = testDeclarationController(earliestEnglishStartDate).show()(FakeRequest())

    status(res) shouldBe OK
    val html: HtmlPage = HtmlPage(res)

    html.verifyElementTextByAttribute("id", "still-owned-heading", "Do you still own and occupy the property?")
    html.verifyElementTextByAttribute("id", "end-date-heading", "Last day as owner and occupier")
  }

  it should "display the correct summary list keys for an owner agent" in new Setup {
    val res: Future[Result] = testDeclarationController(earliestEnglishStartDate).show()(FakeRequest())

    status(res) shouldBe OK
    val html: HtmlPage = HtmlPage(res)

    html.verifyElementTextByAttribute("id", "still-owned-heading", "Does your client still own the property?")
    html.verifyElementTextByAttribute("id", "end-date-heading", "Last day as owner")
  }

  it should "display the correct summary list keys for an occupier agent" in new Setup {
    override val propertyRelationship: CapacityType = Occupier

    val res: Future[Result] = testDeclarationController(earliestEnglishStartDate).show()(FakeRequest())

    status(res) shouldBe OK
    val html: HtmlPage = HtmlPage(res)

    html.verifyElementTextByAttribute("id", "still-owned-heading", "Does your client still occupy the property?")
    html.verifyElementTextByAttribute("id", "end-date-heading", "Last day as occupier")
  }

  it should "display the correct summary list keys for an owner and occupier agent" in new Setup {
    override val propertyRelationship: CapacityType = OwnerOccupier

    val res: Future[Result] = testDeclarationController(earliestEnglishStartDate).show()(FakeRequest())

    status(res) shouldBe OK
    val html: HtmlPage = HtmlPage(res)

    html
      .verifyElementTextByAttribute("id", "still-owned-heading", "Does your client still own and occupy the property?")
    html.verifyElementTextByAttribute("id", "end-date-heading", "Last day as owner and occupier")
  }

  it should "require the user to accept the declaration to continue" in new Setup {
    val res = testDeclarationController(earliestEnglishStartDate).submit()(FakeRequest())
    status(res) shouldBe BAD_REQUEST
  }

  it should "require the user to wait until evidence receipt received" in new Setup {

    when(mockPropertyLinkingService.submit(any(), any())(any(), any()))
      .thenReturn(EitherT.apply[Future, AttachmentException, Unit](Future.successful(Left(NotAllFilesReadyToUpload))))

    val res = testDeclarationController(earliestEnglishStartDate).submit()(
      FakeRequest().withFormUrlEncodedBody("declaration" -> "true"))
    status(res) shouldBe BAD_REQUEST
  }

  it should "submit the property link if the user accepts the declaration" in new Setup {

    when(mockPropertyLinkingService.submit(any(), any())(any(), any()))
      .thenReturn(EitherT.rightT[Future, AttachmentException](()))

    val res = testDeclarationController(earliestEnglishStartDate).submit()(
      FakeRequest().withFormUrlEncodedBody("declaration" -> "true"))
    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some(routes.DeclarationController.confirmation.url)

    verify(mockPropertyLinkingService).submit(any(), any())(any(), any[HeaderCarrier])
  }

  it should "submit the property link on client behalf if the user accepts the declaration" in new Setup {
    when(mockPropertyLinkingService.submit(any(), any())(any(), any()))
      .thenReturn(EitherT.rightT[Future, AttachmentException](()))

    val testDeclaration = new DeclarationController(
      errorHandler = mockCustomErrorHandler,
      propertyLinkService = mockPropertyLinkingService,
      sessionRepository = mockSessionRepo,
      authenticatedAction = preAuthenticatedActionBuilders(),
      withLinkingSession = preEnrichedActionRefiner(),
      declarationView = declarationView,
      linkingRequestSubmittedView = mockLinkingRequestSubmittedView
    )
    val res = testDeclaration.submit()(FakeRequest().withFormUrlEncodedBody("declaration" -> "true"))
    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some(routes.DeclarationController.confirmation.url)

    verify(mockPropertyLinkingService, times(1)).submit(any(), any())(any(), any[HeaderCarrier])
  }

  it should "display the normal confirmation page when the user has uploaded a rates bill" in new Setup {
    when(mockPropertyLinkingService.submit(any(), any())(any(), any()))
      .thenReturn(EitherT.rightT[Future, AttachmentException](()))

    when(mockLinkingRequestSubmittedView.apply(any())(any(), any(), any()))
      .thenReturn(Html("We’ve received your request to add the property to your business’s customer record"))

    val res = testDeclarationController(earliestEnglishStartDate).submit()(
      FakeRequest().withFormUrlEncodedBody("declaration" -> "true"))

    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some(routes.DeclarationController.confirmation.url)

    val confirmation = testDeclarationController(earliestEnglishStartDate).confirmation()(FakeRequest())

    status(confirmation) shouldBe OK
    val html = HtmlPage(confirmation)
    html.shouldContainText("We’ve received your request to add the property to your business’s customer record")
  }

  it should "display the normal confirmation page when the user has uploaded other evidence" in new Setup {
    when(mockPropertyLinkingService.submit(any(), any())(any(), any()))
      .thenReturn(EitherT.rightT[Future, AttachmentException](()))
    when(mockLinkingRequestSubmittedView.apply(any())(any(), any(), any()))
      .thenReturn(Html("We’ve received your request to add the property to your business’s customer record"))

    val res = testDeclarationController(earliestEnglishStartDate).submit()(
      FakeRequest().withFormUrlEncodedBody("declaration" -> "true"))

    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some(routes.DeclarationController.confirmation.url)

    val confirmation = testDeclarationController(earliestEnglishStartDate).confirmation()(FakeRequest())

    status(confirmation) shouldBe OK
    val html = HtmlPage(confirmation)
    html.shouldContainText("We’ve received your request to add the property to your business’s customer record")
  }

  "The confirmation page" should "display the submission ID" in new Setup {
    when(mockPropertyLinkingService.submit(any(), any())(any(), any()))
      .thenReturn(EitherT.rightT[Future, AttachmentException](()))
    when(mockLinkingRequestSubmittedView.apply(any())(any(), any(), any()))
      .thenReturn(Html("PL-123456"))

    val res = testDeclarationController(earliestEnglishStartDate).confirmation()(FakeRequest())

    status(res) shouldBe OK
    contentAsString(res) should include("PL-123456")
  }
}
