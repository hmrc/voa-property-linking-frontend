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
import auditing.AuditingService
import connectors.BusinessRatesAttachmentConnector
import models.LinkingSession
import models.attachment.InitiateAttachmentRequest
import org.mockito.ArgumentMatchers._
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import repositories.SessionRepo
import uk.gov.hmrc.http.HeaderCarrier
import utils.FakeObjects
import org.scalacheck.Arbitrary._
import resources._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import play.api.libs.json.{Json, Reads}
import play.api.test.FakeRequest
import session.LinkingSessionRequest

import scala.concurrent.ExecutionContext._

class BusinessRatesAttachmentServiceSpec extends ServiceSpec with MockitoSugar with FakeObjects {
  val businessRatesAttachmentConnector = mock[BusinessRatesAttachmentConnector]
  val mockAuditingService = mock[AuditingService]
  val mockSessionRepo = mock[SessionRepo]
  val initiateAttachmentRequest = InitiateAttachmentRequest("FILE_NAME", "img/jpeg", None)
  val linkingSessionData = arbitrary[LinkingSession].copy(uploadEvidenceData = uploadEvidenceData)
  implicit val request = BasicAuthenticatedRequest(groupAccount, detailedIndividualAccount, FakeRequest())
  implicit val linkingSessionRequest = LinkingSessionRequest(linkingSessionData, 1234l, detailedIndividualAccount, groupAccount, request)
  implicit val hc = HeaderCarrier()
  implicit val ec = global

  val businessRatesChallengeService = new BusinessRatesAttachmentService(
    businessRatesAttachmentConnector = businessRatesAttachmentConnector,
    sessionRepository = mockSessionRepo,
    auditingService = mockAuditingService)

    it should "Upload Bill Evidence initiateAttachmentUpload" in {
      when(mockSessionRepo.get[LinkingSession](any(), any())).thenReturn(Future.successful(Some(linkingSessionData)))
      when(businessRatesAttachmentConnector.initiateAttachmentUpload(any())(any[HeaderCarrier])).thenReturn(Future successful preparedUpload)
      when(mockSessionRepo.saveOrUpdate(any())(any(), any())).thenReturn(Future.successful(()))
      businessRatesChallengeService.initiateAttachmentUpload(initiateAttachmentRequest)(request, hc)
      verify(mockSessionRepo, times(1)).get[LinkingSession](any(), any())
      verify(businessRatesAttachmentConnector, times(1)).initiateAttachmentUpload(any())(any[HeaderCarrier])
    }

    it should "call to persistSessionData is success" in {
      when(mockSessionRepo.saveOrUpdate(any())(any(), any())).thenReturn(Future.successful(()))
      businessRatesChallengeService.persistSessionData(linkingSessionData,  uploadEvidenceData)
      verify(mockSessionRepo, atLeastOnce()).saveOrUpdate(any())(any(), any[HeaderCarrier])
    }

    it should "call to submit Files is success" in {
      when(businessRatesAttachmentConnector.submitFile(any(), any())(any[HeaderCarrier])).thenReturn(Future successful Some(attachment))

      businessRatesChallengeService.submitFiles(FILE_REFERENCE,  Some(Map(FILE_REFERENCE -> uploadedFileDetails)))

      verify(businessRatesAttachmentConnector, times(1)).submitFile(any(), any())(any[HeaderCarrier])
    }


}

