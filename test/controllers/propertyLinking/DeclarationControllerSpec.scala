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
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import repositories.SessionRepo
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.propertylinking.exceptions.attachments.{AttachmentException, NotAllFilesReadyToUpload}
import utils.{HtmlPage, _}

import scala.concurrent.Future

class DeclarationControllerSpec extends VoaPropertyLinkingSpec {
  implicit val hc = HeaderCarrier()
  private val mockDeclarationView = mock[views.html.propertyLinking.declaration]
  private val mockLinkingRequestSubmittedView = mock[views.html.linkingRequestSubmitted]

  trait Setup {
    object TestDeclaration
        extends DeclarationController(
          mockCustomErrorHandler,
          mockPropertyLinkingService,
          mockSessionRepo,
          preAuthenticatedActionBuilders(),
          preEnrichedActionRefiner(),
          mockDeclarationView,
          mockLinkingRequestSubmittedView
        )

    lazy val mockSessionRepo = {
      val f = mock[SessionRepo]
      when(f.start(any())(any(), any())).thenReturn(Future.successful(()))

      when(f.remove()(any())).thenReturn(Future.successful(()))
      f
    }

    lazy val envelopeId: String = shortString
  }

  "The declaration page" should "return valid page" in new Setup {

    when(mockDeclarationView.apply(any(), any(), any())(any(), any(), any()))
      .thenReturn(Html("The declaration page"))
    val res = TestDeclaration.show()(FakeRequest())
    status(res) shouldBe OK
    val html = HtmlPage(res)
    html.shouldContainText("The declaration page")
  }

  it should "require the user to accept the declaration to continue" in new Setup {

    val res = TestDeclaration.submit()(FakeRequest())
    status(res) shouldBe BAD_REQUEST
  }

  it should "require the user to wait until evidence receipt received" in new Setup {

    when(mockPropertyLinkingService.submit(any(), any())(any(), any()))
      .thenReturn(EitherT.apply[Future, AttachmentException, Unit](Future.successful(Left(NotAllFilesReadyToUpload))))
    when(mockDeclarationView.apply(any(), any(), any())(any(), any(), any()))
      .thenReturn(Html("please try again in a moment"))
    val res = TestDeclaration.submit()(FakeRequest().withFormUrlEncodedBody("declaration" -> "true"))
    status(res) shouldBe BAD_REQUEST

    val html = HtmlPage(res)
    html.shouldContainText("please try again in a moment")
  }

  it should "submit the property link if the user accepts the declaration" in new Setup {
    when(mockPropertyLinkingService.submit(any(), any())(any(), any()))
      .thenReturn(EitherT.rightT[Future, AttachmentException](()))

    val res = TestDeclaration.submit()(FakeRequest().withFormUrlEncodedBody("declaration" -> "true"))
    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some(routes.DeclarationController.confirmation.url)

    verify(mockPropertyLinkingService, times(1)).submit(any(), any())(any(), any[HeaderCarrier])
  }

  it should "submit the property link on client behalf if the user accepts the declaration" in new Setup {
    when(mockPropertyLinkingService.submit(any(), any())(any(), any()))
      .thenReturn(EitherT.rightT[Future, AttachmentException](()))

    val testDeclaration = new DeclarationController(
      mockCustomErrorHandler,
      mockPropertyLinkingService,
      mockSessionRepo,
      preAuthenticatedActionBuilders(),
      preEnrichedActionRefiner(),
      mockDeclarationView,
      mockLinkingRequestSubmittedView
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

    val res = TestDeclaration.submit()(FakeRequest().withFormUrlEncodedBody("declaration" -> "true"))
    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some(routes.DeclarationController.confirmation.url)

    val confirmation = TestDeclaration.confirmation()(FakeRequest())
    status(confirmation) shouldBe OK
    val html = HtmlPage(confirmation)
    html.shouldContainText("We’ve received your request to add the property to your business’s customer record")
  }

  it should "display the normal confirmation page when the user has uploaded other evidence" in new Setup {
    when(mockPropertyLinkingService.submit(any(), any())(any(), any()))
      .thenReturn(EitherT.rightT[Future, AttachmentException](()))
    when(mockLinkingRequestSubmittedView.apply(any())(any(), any(), any()))
      .thenReturn(Html("We’ve received your request to add the property to your business’s customer record"))

    val res = TestDeclaration.submit()(FakeRequest().withFormUrlEncodedBody("declaration" -> "true"))
    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some(routes.DeclarationController.confirmation.url)

    val confirmation = TestDeclaration.confirmation()(FakeRequest())
    status(confirmation) shouldBe OK
    val html = HtmlPage(confirmation)
    html.shouldContainText("We’ve received your request to add the property to your business’s customer record")
  }

  "The confirmation page" should "display the submission ID" in new Setup {

    when(mockPropertyLinkingService.submit(any(), any())(any(), any()))
      .thenReturn(EitherT.rightT[Future, AttachmentException](()))
    when(mockLinkingRequestSubmittedView.apply(any())(any(), any(), any()))
      .thenReturn(Html("PL-123456"))

    val res = TestDeclaration.confirmation()(FakeRequest())
    status(res) shouldBe OK

    contentAsString(res) should include("PL-123456")
  }

}
