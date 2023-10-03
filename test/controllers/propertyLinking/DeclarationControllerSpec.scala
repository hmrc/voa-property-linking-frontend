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

package controllers.propertyLinking

import binders.propertylinks.EvidenceChoices
import cats.data.EitherT
import cats.instances.future._
import controllers.VoaPropertyLinkingSpec
import models._
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalactic.source.Position
import org.scalatest.{Inspectors, OptionValues}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepo
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.propertylinking.exceptions.attachments._
import utils._

import java.time.LocalDate
import scala.concurrent.Future

class DeclarationControllerSpec extends VoaPropertyLinkingSpec {
  implicit val hc = HeaderCarrier()

  trait Setup {
    lazy val mockSessionRepo = {
      val f = mock[SessionRepo]
      when(f.start(any())(any(), any())).thenReturn(Future.successful(()))
      when(f.saveOrUpdate(any())(any(), any())).thenReturn(Future.successful(()))
      when(f.remove()(any())).thenReturn(Future.successful(()))
      f
    }
    lazy val envelopeId: String = shortString
    lazy val propertyRelationship: Option[CapacityType] = Some(Owner)
    lazy val evidence: UploadEvidenceData = uploadRatesBillData
    val isAgent: Boolean = true

    def testDeclarationController(earliestStartDate: LocalDate) =
      new DeclarationController(
        errorHandler = mockCustomErrorHandler,
        propertyLinkService = mockPropertyLinkingService,
        sessionRepository = mockSessionRepo,
        authenticatedAction = preAuthenticatedActionBuilders(isAgent),
        withLinkingSession = preEnrichedActionRefiner(
          evidenceData = evidence,
          relationshipCapacity = propertyRelationship,
          userIsAgent = isAgent,
          earliestStartDate = earliestStartDate
        ),
        withSubmittedLinkingSession = submittedActionRefiner(
          evidenceData = uploadRatesBillData,
          relationshipCapacity = propertyRelationship,
          userIsAgent = isAgent,
          earliestStartDate = earliestStartDate
        ),
        declarationView = declarationView,
        linkingRequestSubmittedView = linkingRequestSubmittedView
      )
  }

  "show" should "set 'fromCya' in the session" in new Setup {
    val sessionCaptor: ArgumentCaptor[LinkingSession] = ArgumentCaptor.forClass(classOf[LinkingSession])
    testDeclarationController(earliestEnglishStartDate).show()(FakeRequest()).futureValue
    verify(mockSessionRepo).saveOrUpdate[LinkingSession](sessionCaptor.capture())(any(), any())
    sessionCaptor.getValue.fromCya shouldBe Some(true)
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
    override lazy val propertyRelationship: Option[CapacityType] = Some(Occupier)

    val res: Future[Result] = testDeclarationController(earliestEnglishStartDate).show()(FakeRequest())

    status(res) shouldBe OK
    val html: HtmlPage = HtmlPage(res)

    html.verifyElementTextByAttribute("id", "still-owned-heading", "Do you still occupy the property?")
    html.verifyElementTextByAttribute("id", "end-date-heading", "Last day as occupier")
  }

  it should "display the correct summary list keys for an owner and occupier IP" in new Setup {
    override val isAgent: Boolean = false
    override lazy val propertyRelationship: Option[CapacityType] = Some(OwnerOccupier)

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
    override lazy val propertyRelationship: Option[CapacityType] = Some(Occupier)

    val res: Future[Result] = testDeclarationController(earliestEnglishStartDate).show()(FakeRequest())

    status(res) shouldBe OK
    val html: HtmlPage = HtmlPage(res)

    html.verifyElementTextByAttribute("id", "still-owned-heading", "Does your client still occupy the property?")
    html.verifyElementTextByAttribute("id", "end-date-heading", "Last day as occupier")
  }

  it should "display the correct summary list keys for an owner and occupier agent" in new Setup {
    override lazy val propertyRelationship: Option[CapacityType] = Some(OwnerOccupier)

    val res: Future[Result] = testDeclarationController(earliestEnglishStartDate).show()(FakeRequest())

    status(res) shouldBe OK
    val html: HtmlPage = HtmlPage(res)

    html
      .verifyElementTextByAttribute("id", "still-owned-heading", "Does your client still own and occupy the property?")
    html.verifyElementTextByAttribute("id", "end-date-heading", "Last day as owner and occupier")
  }

  it should "have a link to change the property connection" in new Setup {
    val doc: Document =
      Jsoup.parse(contentAsString(testDeclarationController(earliestEnglishStartDate).show()(FakeRequest())))
    val changeLink: Element = doc.getElementById("relationship-change")
    changeLink.attr("href") shouldBe routes.ClaimPropertyRelationshipController.back.url
  }

  it should "have a link to change the property connection start date" in new Setup {
    val doc: Document =
      Jsoup.parse(contentAsString(testDeclarationController(earliestEnglishStartDate).show()(FakeRequest())))
    val changeLink: Element = doc.getElementById("start-date-change")
    changeLink.attr("href") shouldBe routes.ClaimPropertyOwnershipController.showOwnership.url
  }

  it should "have a link to change the property occupancy" in new Setup {
    val doc: Document =
      Jsoup.parse(contentAsString(testDeclarationController(earliestEnglishStartDate).show()(FakeRequest())))
    val changeLink: Element = doc.getElementById("still-owned-change")
    changeLink.attr("href") shouldBe routes.ClaimPropertyOccupancyController.showOccupancy.url
  }

  it should "have a link to change the property occupancy last day" in new Setup {
    val doc: Document =
      Jsoup.parse(contentAsString(testDeclarationController(earliestEnglishStartDate).show()(FakeRequest())))
    val changeLink: Element = doc.getElementById("end-date-change")
    changeLink.attr("href") shouldBe routes.ClaimPropertyOccupancyController.showOccupancy.url
  }

  it should "have a link to change uploaded evidence" in new Setup {
    val doc: Document =
      Jsoup.parse(contentAsString(testDeclarationController(earliestEnglishStartDate).show()(FakeRequest())))
    val changeLink: Element = doc.getElementById("evidence-change")
    changeLink.attr("href") shouldBe routes.ChooseEvidenceController.show.url
  }

  it should "have a back link to the back endpoint" in new Setup {
    val doc: Document =
      Jsoup.parse(contentAsString(testDeclarationController(earliestEnglishStartDate).show(FakeRequest())))
    val backLink: Element = doc.getElementById("back-link")
    backLink.attr("href") shouldBe routes.DeclarationController.back.url
  }

  it should "require the user to accept the declaration to continue" in new Setup {
    val res = testDeclarationController(earliestEnglishStartDate).submit(FakeRequest())
    status(res) shouldBe BAD_REQUEST
  }

  it should "require the user to wait until evidence receipt received" in new Setup {

    when(mockPropertyLinkingService.submit(any(), any())(any(), any()))
      .thenReturn(EitherT.apply[Future, AttachmentException, Unit](Future.successful(Left(NotAllFilesReadyToUpload))))

    val res = testDeclarationController(earliestEnglishStartDate).submit(
      FakeRequest().withFormUrlEncodedBody("declaration" -> "true"))
    status(res) shouldBe BAD_REQUEST
  }

  it should "submit the property link if the user accepts the declaration" in new Setup {

    when(mockPropertyLinkingService.submit(any(), any())(any(), any()))
      .thenReturn(EitherT.rightT[Future, AttachmentException](()))

    val res = testDeclarationController(earliestEnglishStartDate).submit(
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
      withSubmittedLinkingSession = submittedActionRefiner(evidenceData = uploadRatesBillData),
      declarationView = declarationView,
      linkingRequestSubmittedView = linkingRequestSubmittedView
    )
    val res = testDeclaration.submit(FakeRequest().withFormUrlEncodedBody("declaration" -> "true"))
    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some(routes.DeclarationController.confirmation.url)

    verify(mockPropertyLinkingService, times(1)).submit(any(), any())(any(), any[HeaderCarrier])
  }

  it should "display the normal confirmation page when the user has uploaded a rates bill" in new Setup {
    when(mockPropertyLinkingService.submit(any(), any())(any(), any()))
      .thenReturn(EitherT.rightT[Future, AttachmentException](()))

    val res = testDeclarationController(earliestEnglishStartDate).submit(
      FakeRequest().withFormUrlEncodedBody("declaration" -> "true"))

    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some(routes.DeclarationController.confirmation.url)

    val confirmation = testDeclarationController(earliestEnglishStartDate).confirmation(FakeRequest())

    status(confirmation) shouldBe OK
    val html = HtmlPage(confirmation)
    html.titleShouldMatch("Property claim submitted - Valuation Office Agency - GOV.UK")
    html.shouldContainText("Your submission number PL-123456")
  }

  it should "display the normal confirmation page when the user has uploaded other evidence" in new Setup {
    when(mockPropertyLinkingService.submit(any(), any())(any(), any()))
      .thenReturn(EitherT.rightT[Future, AttachmentException](()))

    val res = testDeclarationController(earliestEnglishStartDate).submit()(
      FakeRequest().withFormUrlEncodedBody("declaration" -> "true"))

    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some(routes.DeclarationController.confirmation.url)

    val confirmation = testDeclarationController(earliestEnglishStartDate).confirmation(FakeRequest())

    status(confirmation) shouldBe OK
    val html = HtmlPage(confirmation)
    html.titleShouldMatch("Property claim submitted - Valuation Office Agency - GOV.UK")
    html.shouldContainText("Make a note of your reference number as you’ll need to provide it if you contact us")
  }

  "The confirmation page" should "display the submission ID" in new Setup {
    when(mockPropertyLinkingService.submit(any(), any())(any(), any()))
      .thenReturn(EitherT.rightT[Future, AttachmentException](()))

    val res = testDeclarationController(earliestEnglishStartDate).confirmation(FakeRequest())

    status(res) shouldBe OK
    val html = HtmlPage(res)
    html.titleShouldMatch("Property claim submitted - Valuation Office Agency - GOV.UK")
    html.shouldContainText("Your submission number PL-123456")
  }

  "back" should "set 'fromCya' in the session" in new Setup {
    val sessionCaptor: ArgumentCaptor[LinkingSession] = ArgumentCaptor.forClass(classOf[LinkingSession])
    testDeclarationController(earliestEnglishStartDate).back(FakeRequest()).futureValue
    verify(mockSessionRepo).saveOrUpdate[LinkingSession](sessionCaptor.capture())(any(), any())
    sessionCaptor.getValue.fromCya shouldBe Some(false)
  }

  abstract class BackLinkTest(implicit pos: Position) extends Setup with OptionValues {
    lazy val controller: DeclarationController = testDeclarationController(earliestEnglishStartDate)
    lazy val result: Future[Result] = controller.back(FakeRequest())
    val expectedRedirect: String

    status(result) shouldBe SEE_OTHER
    redirectLocation(result).value shouldBe expectedRedirect
  }

  Inspectors.forAll(Seq(Owner, OwnerOccupier)) { relationshipType =>
    s"back, when the user is an ${relationshipType.name}" should "redirect to the upload rates bill page" in new BackLinkTest {
      override lazy val propertyRelationship: Option[CapacityType] = Some(relationshipType)
      override lazy val evidence: UploadEvidenceData = uploadRatesBillData
      lazy val expectedRedirect: String = routes.UploadController.show(EvidenceChoices.RATES_BILL).url
    }

    Inspectors.forAll(Seq(uploadLeaseData)) { upload =>
      val uploadType = upload.fileInfo.getOrElse(fail("could not get upload info")).evidenceType.name

      it should s"redirect to the choose evidence page when a $uploadType is uploaded" in new BackLinkTest {
        override lazy val propertyRelationship: Option[CapacityType] = Some(relationshipType)
        override lazy val evidence: UploadEvidenceData = upload
        lazy val expectedRedirect: String = routes.UploadController.show(EvidenceChoices.LEASE).url
      }
    }

    Inspectors.forAll(Seq(uploadLicenseData)) { upload =>
      val uploadType = upload.fileInfo.getOrElse(fail("could not get upload info")).evidenceType.name

      it should s"redirect to the choose evidence page when a $uploadType is uploaded" in new BackLinkTest {
        override lazy val propertyRelationship: Option[CapacityType] = Some(relationshipType)
        override lazy val evidence: UploadEvidenceData = upload
        lazy val expectedRedirect: String = routes.UploadController.show(EvidenceChoices.LICENSE).url
      }
    }

    Inspectors.forAll(Seq(uploadServiceChargeData)) { upload =>
      val uploadType = upload.fileInfo.getOrElse(fail("could not get upload info")).evidenceType.name

      it should s"redirect to the choose evidence page when a $uploadType is uploaded" in new BackLinkTest {
        override lazy val propertyRelationship: Option[CapacityType] = Some(relationshipType)
        override lazy val evidence: UploadEvidenceData = upload
        lazy val expectedRedirect: String = routes.UploadController.show(EvidenceChoices.SERVICE_CHARGE).url
      }
    }
  }

  "back, when the user is an OCCUPIER" should "redirect to the upload lease page" in new BackLinkTest {
    override lazy val propertyRelationship: Option[CapacityType] = Some(Occupier)
    override lazy val evidence: UploadEvidenceData = uploadLeaseData
    lazy val expectedRedirect: String = routes.UploadController.show(EvidenceChoices.LEASE).url
  }

  it should "redirect to the upload licence to occupy page" in new BackLinkTest {
    override lazy val propertyRelationship: Option[CapacityType] = Some(Occupier)
    override lazy val evidence: UploadEvidenceData = uploadLicenseData
    lazy val expectedRedirect: String = routes.UploadController.show(EvidenceChoices.LICENSE).url
  }

  Inspectors.forAll(Seq(uploadRatesBillData, uploadServiceChargeData)) { upload =>
    val uploadType = upload.fileInfo.getOrElse(fail("could not get upload info")).evidenceType.name

    it should s"redirect to the choose evidence page when a $uploadType is uploaded" in new BackLinkTest {
      override lazy val propertyRelationship: Option[CapacityType] = Some(Occupier)
      override lazy val evidence: UploadEvidenceData = upload
      lazy val expectedRedirect: String = routes.UploadController.show(EvidenceChoices.NO_LEASE_OR_LICENSE).url
    }
  }

  Inspectors.forAll(Seq(Owner, Occupier, OwnerOccupier)) { relationshipType =>
    s"back, when the user is a ${relationshipType.name} and the session is missing uploaded evidence" should "redirect to the initial choose evidence page" in new BackLinkTest {
      override lazy val propertyRelationship: Option[CapacityType] = Some(relationshipType)
      override lazy val evidence: UploadEvidenceData = UploadEvidenceData.empty
      lazy val expectedRedirect: String = routes.ChooseEvidenceController.show.url
    }
  }

  s"back, when the session is missing a relationship capacity" should "redirect to the claim relationship page" in new BackLinkTest {
    override lazy val propertyRelationship: Option[CapacityType] = None
    override lazy val evidence: UploadEvidenceData = uploadRatesBillData
    lazy val expectedRedirect: String = routes.ClaimPropertyRelationshipController.back.url
  }
}
