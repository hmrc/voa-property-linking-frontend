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

package services.propertylinking

import actions.propertylinking.requests.LinkingSessionRequest
import cats.data.EitherT
import cats.implicits._
import models._
import models.propertylinking.requests.PropertyLinkRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{when, _}
import play.api.test.FakeRequest
import services.ServiceSpec
import tests.AllMocks
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.propertylinking.exceptions.attachments.AttachmentException
import java.time.LocalDate

import scala.concurrent.Future
import scala.language.implicitConversions

class PropertyLinkingServiceSpec extends ServiceSpec with AllMocks {

  private lazy val testService =
    new PropertyLinkingService(mockBusinessRatesAttachmentsService, mockPropertyLinkConnector)
  val httpResponse = emptyJsonHttpResponse(200)
  val clientId = 100
  val mockPropertyLinkRequest = mock[PropertyLinkRequest]
  implicit val hc = HeaderCarrier(sessionId = Some(SessionId("1111")))

  implicit def linkingSessionRequest(clientDetails: Option[ClientDetails] = None) = LinkingSessionRequest(
    LinkingSession(
      address = "",
      uarn = 1L,
      submissionId = "PL-123456",
      personId = 1L,
      propertyRelationship = Some(PropertyRelationship(Owner)),
      propertyOwnership = Some(PropertyOwnership(interestedBefore2017 = true, fromDate = None)),
      propertyOccupancy = Some(PropertyOccupancy(stillOccupied = false, lastOccupiedDate = None)),
      hasRatesBill = Some(true),
      uploadEvidenceData = UploadEvidenceData(fileInfo = None, attachments = None),
      evidenceType = Some(RatesBillType),
      clientDetails = clientDetails,
      localAuthorityReference = "12341531531"
    ),
    organisationId = 1L,
    individualAccount = detailedIndividualAccount,
    organisationAccount = groupAccount(true),
    request = FakeRequest()
  )

  "submit with None clientId" should {
    "return Unit when successful" in {
      implicit val linkingSession = linkingSessionRequest()
      when(mockPropertyLinkConnector.createPropertyLink(any())(any()))
        .thenReturn(Future.successful(httpResponse))
      when(mockBusinessRatesAttachmentsService.submit(any(), any(), any())(any(), any()))
        .thenReturn(EitherT.rightT[Future, AttachmentException](List(attachment)))
      val res: EitherT[Future, AttachmentException, Unit] = testService.submit(mockPropertyLinkRequest, None)
      res.value.futureValue should be(Right(()))
      verify(mockPropertyLinkConnector, times(1)).createPropertyLink(any())(any())
    }
  }

  "submit with clientId" should {
    "return Unit when successful" in {
      implicit val updatedLinkingSession = linkingSessionRequest(Some(ClientDetails(100, "ABC")))
      when(mockPropertyLinkConnector.createPropertyLinkOnClientBehalf(any(), any())(any()))
        .thenReturn(Future.successful(httpResponse))
      when(mockBusinessRatesAttachmentsService.submit(any(), any(), any())(any(), any()))
        .thenReturn(EitherT.rightT[Future, AttachmentException](List(attachment)))
      val res: EitherT[Future, AttachmentException, Unit] =
        testService.submitOnClientBehalf(mockPropertyLinkRequest, clientId)
      res.value.futureValue should be(Right(()))
      verify(mockPropertyLinkConnector, times(1)).createPropertyLinkOnClientBehalf(any(), any())(any())
    }
  }

  "find earliest start date" should {
    "return valid start date" in {
      implicit val linkingSession = linkingSessionRequest()
      when(mockPropertyLinkConnector.getPropertyHistory(any())(any()))
        .thenReturn(Future.successful(propertyHistory))
      val res: Future[Option[LocalDate]] =
        testService.findEarliestStartDate(propertyHistory.uarn)
      res.futureValue should be(Some(LocalDate.now().plusYears(1)))
      verify(mockPropertyLinkConnector, times(1)).getPropertyHistory(any())(any())
    }
  }

}
