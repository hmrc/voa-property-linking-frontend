/*
 * Copyright 2019 HM Revenue & Customs
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

import actions.BasicAuthenticatedRequest
import connectors.attachments.BusinessRatesAttachmentConnector
import models.LinkingSession
import models.attachment.InitiateAttachmentPayload
import models.attachment.request.InitiateAttachmentRequest
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalacheck.Arbitrary._
import play.api.test.FakeRequest
import repositories.SessionRepo
import resources._
import session.LinkingSessionRequest
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class BusinessRatesAttachmentServiceSpec extends ServiceSpec {
  val businessRatesAttachmentConnector = mock[BusinessRatesAttachmentConnector]
  val mockSessionRepo = mock[SessionRepo]
  val initiateAttachmentRequest = InitiateAttachmentPayload(InitiateAttachmentRequest("FILE_NAME", "img/jpeg"), "http://example.com", "http://example.com/failure")
  val linkingSessionData = arbitrary[LinkingSession].copy(uploadEvidenceData = uploadEvidenceData)
  implicit val request = new BasicAuthenticatedRequest(groupAccount(agent = true), detailedIndividualAccount, userDetails(AffinityGroup.Organisation), FakeRequest())
  implicit val linkingSessionRequest = LinkingSessionRequest(linkingSessionData, 1234l, detailedIndividualAccount, groupAccount(agent = true), request)
  implicit val hc = HeaderCarrier()

  val businessRatesChallengeService = new BusinessRatesAttachmentService(
    businessRatesAttachmentConnector = businessRatesAttachmentConnector,
    sessionRepository = mockSessionRepo,
    auditingService = mockAuditingService)

    it should "Upload Bill Evidence initiateAttachmentUpload" in {
      when(mockSessionRepo.get[LinkingSession](any(), any())).thenReturn(Future.successful(Some(linkingSessionData)))
      when(businessRatesAttachmentConnector.initiateAttachmentUpload(any())(any[HeaderCarrier])).thenReturn(Future successful preparedUpload)
      when(mockSessionRepo.saveOrUpdate(any())(any(), any())).thenReturn(Future.successful(()))
      businessRatesChallengeService.initiateAttachmentUpload(initiateAttachmentRequest)(request, hc).futureValue
      verify(mockSessionRepo).get[LinkingSession](any(), any())
      verify(businessRatesAttachmentConnector).initiateAttachmentUpload(any())(any[HeaderCarrier])
    }

    it should "call to persistSessionData is success" in {
      when(mockSessionRepo.saveOrUpdate(any())(any(), any())).thenReturn(Future.successful(()))
      businessRatesChallengeService.persistSessionData(linkingSessionData,  uploadEvidenceData)
      verify(mockSessionRepo, atLeastOnce()).saveOrUpdate(any())(any(), any[HeaderCarrier])
    }

    it should "call to submit Files is success" in {
      when(businessRatesAttachmentConnector.submitFile(any(), any())(any[HeaderCarrier])).thenReturn(Future successful Some(attachment))

      businessRatesChallengeService.submitFiles(FILE_REFERENCE, Some(Map(FILE_REFERENCE -> uploadedFileDetails))).futureValue

      verify(businessRatesAttachmentConnector).submitFile(any(), any())(any[HeaderCarrier])
    }
}

