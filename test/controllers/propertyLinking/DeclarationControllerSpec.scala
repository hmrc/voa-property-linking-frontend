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

  it should "require the user to wait until evidence receipt received" in new Setup {

    when(mockPropertyLinkingService.submit(any(), any())(any(), any()))
      .thenReturn(EitherT.apply[Future, AttachmentException, Unit](Future.successful(Left(NotAllFilesReadyToUpload))))

    val res = testDeclarationController(earliestEnglishStartDate).submit(
      FakeRequest().withFormUrlEncodedBody("declaration" -> "true"))
    status(res) shouldBe BAD_REQUEST
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
