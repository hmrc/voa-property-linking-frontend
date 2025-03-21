/*
 * Copyright 2024 HM Revenue & Customs
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

package services

import actions.propertylinking.requests.LinkingSessionRequest
import actions.requests.BasicAuthenticatedRequest
import connectors.attachments.BusinessRatesAttachmentsConnector
import models.{LinkingSession, RatesBillType}
import models.attachment._
import models.attachment.request.InitiateAttachmentRequest
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalacheck.Arbitrary._
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import repositories.SessionRepo
import utils._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.propertylinking.exceptions.attachments.AttachmentException

import scala.concurrent.Future

class BusinessRatesAttachmentsServiceSpec extends ServiceSpec {
  val businessRatesAttachmentConnector = mock[BusinessRatesAttachmentsConnector]
  val mockSessionRepo = mock[SessionRepo]
  val initiateAttachmentRequest = InitiateAttachmentPayload(
    InitiateAttachmentRequest("FILE_NAME", "img/jpeg", RatesBillType),
    "http://example.com",
    "http://example.com/failure"
  )
  val linkingSessionData = arbitrary[LinkingSession].copy(uploadEvidenceData = uploadRatesBillData)
  implicit val request: BasicAuthenticatedRequest[AnyContentAsEmpty.type] =
    BasicAuthenticatedRequest(groupAccount(agent = true), detailedIndividualAccount, FakeRequest())
  implicit val linkingSessionRequest: LinkingSessionRequest[AnyContentAsEmpty.type] =
    LinkingSessionRequest(linkingSessionData, 1234L, detailedIndividualAccount, groupAccount(agent = true), request)
  implicit val hc: HeaderCarrier = HeaderCarrier()

  val businessRatesChallengeService = new BusinessRatesAttachmentsService(
    businessRatesAttachmentsConnector = businessRatesAttachmentConnector,
    sessionRepository = mockSessionRepo,
    auditingService = mockAuditingService
  )

  "BusinessRatesAttachmentsService" should {
    "Upload Bill Evidence initiateAttachmentUpload" in {
      when(mockSessionRepo.get[LinkingSession](any(), any())).thenReturn(Future.successful(Some(linkingSessionData)))
      when(businessRatesAttachmentConnector.initiateAttachmentUpload(any())(any[HeaderCarrier]))
        .thenReturn(Future successful preparedUpload)
      when(mockSessionRepo.saveOrUpdate(any())(any(), any())).thenReturn(Future.successful(()))

      businessRatesChallengeService
        .initiateAttachmentUpload(initiateAttachmentRequest, RatesBillType)(linkingSessionRequest, hc)
        .futureValue

      verify(businessRatesAttachmentConnector, times(1)).initiateAttachmentUpload(any())(any[HeaderCarrier])
    }

    "call to persistSessionData is success" in {
      when(mockSessionRepo.saveOrUpdate(any())(any(), any())).thenReturn(Future.successful(()))
      businessRatesChallengeService.persistSessionData(linkingSessionData, uploadRatesBillData)
      verify(mockSessionRepo, atLeastOnce()).saveOrUpdate(any())(any(), any[HeaderCarrier])
    }
  }

  "submit" should {
    "call BRATT and validate property link evidence is in MetadataPending state" in {
      when(businessRatesAttachmentConnector.submitFile(any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(attachment))
      when(businessRatesAttachmentConnector.getAttachment(any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(attachment.copy(state = MetadataPending)))

      val result: Either[AttachmentException, List[Attachment]] =
        businessRatesChallengeService.submit("PL-123", List(FILE_REFERENCE)).value.futureValue

      result shouldBe Symbol("right")

      verify(businessRatesAttachmentConnector).submitFile(any(), any())(any[HeaderCarrier])
    }

    "call BRATT and validate property link evidence is in UploadPending state" in {
      val att = attachment.copy(state = UploadPending)
      when(businessRatesAttachmentConnector.submitFile(any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(att))
      when(businessRatesAttachmentConnector.getAttachment(any())(any[HeaderCarrier])).thenReturn(Future.successful(att))

      val result: Either[AttachmentException, List[Attachment]] =
        businessRatesChallengeService.submit("PL-123", List(FILE_REFERENCE)).value.futureValue

      result shouldBe Right(List(att))
    }

    "call to submit Files is success" in {
      when(businessRatesAttachmentConnector.submitFile(any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(attachment))

      businessRatesChallengeService.patchMetadata(FILE_REFERENCE, FILE_REFERENCE).futureValue

      verify(businessRatesAttachmentConnector, times(2)).submitFile(any(), any())(any[HeaderCarrier])
    }
  }

}
