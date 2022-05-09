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
import org.mockito.Mockito
import org.mockito.Mockito.{when, _}
import play.api.test.FakeRequest
import services.ServiceSpec
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.propertylinking.exceptions.attachments.AttachmentException

import java.time.LocalDate
import scala.concurrent.Future
import scala.language.implicitConversions

class PropertyLinkingServiceSpec extends ServiceSpec {

  private lazy val testService =
    new PropertyLinkingService(mockBusinessRatesAttachmentsService, mockPropertyLinkConnector, mockApplicationConfig)

  val httpResponse = emptyJsonHttpResponse(200)
  val clientId = 100
  val mockPropertyLinkRequest = mock[PropertyLinkRequest]
  implicit val hc = HeaderCarrier(sessionId = Some(SessionId("1111")))

  implicit def linkingSessionRequest(clientDetails: Option[ClientDetails] = None) = LinkingSessionRequest(
    LinkingSession(
      address = "some address",
      uarn = 1L,
      submissionId = "PL-123456",
      personId = 1L,
      earliestStartDate = earliestEnglishStartDate,
      propertyRelationship = Some(PropertyRelationship(Owner)),
      propertyOwnership = Some(PropertyOwnership(interestedOnOrBefore = true, fromDate = None)),
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
      verify(mockPropertyLinkConnector).createPropertyLink(any())(any())
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
      verify(mockPropertyLinkConnector).createPropertyLinkOnClientBehalf(any(), any())(any())
    }
  }

  trait Setup {
    implicit val linkingSession = linkingSessionRequest()

    when(mockApplicationConfig.earliestEnglishStartDate).thenReturn(earliestEnglishStartDate)
    when(mockApplicationConfig.earliestWelshStartDate).thenReturn(earliestWelshStartDate)
  }

  "find earliest start date" when {
    "property is english" should {
      "return earliest start date from property history" in new Setup {

        val earliestStartDateOfCurrentList = LocalDate.of(2019, 5, 1)
        val currentPropertyValuations = Seq(
          propertyValuation1.copy(listType = ListType.CURRENT, propertyLinkEarliestStartDate = None),
          propertyValuation1
            .copy(
              listType = ListType.CURRENT,
              propertyLinkEarliestStartDate = Some(earliestStartDateOfCurrentList.plusDays(1))),
          propertyValuation1
            .copy(listType = ListType.DRAFT, propertyLinkEarliestStartDate = Some(earliestStartDateOfCurrentList)),
          propertyValuation1
            .copy(
              listType = ListType.DRAFT,
              propertyLinkEarliestStartDate = Some(earliestStartDateOfCurrentList.plusDays(2))),
          propertyValuation1
            .copy(
              listType = ListType.CURRENT,
              propertyLinkEarliestStartDate = Some(earliestStartDateOfCurrentList.plusDays(3)))
        )
        val history = propertyHistory.copy(history = currentPropertyValuations, localAuthorityCode = "1English")

        when(mockPropertyLinkConnector.getPropertyHistory(any())(any())).thenReturn(Future.successful(history))

        val res: Future[LocalDate] = testService.findEarliestStartDate(propertyHistory.uarn)

        res.futureValue shouldBe earliestStartDateOfCurrentList
        verify(mockPropertyLinkConnector).getPropertyHistory(any())(any())
      }

      "return default earliest english start date from config" in new Setup {

        val currentPropertyValuations = Seq(
          propertyValuation1.copy(listType = ListType.DRAFT, propertyLinkEarliestStartDate = None),
          propertyValuation1.copy(listType = ListType.CURRENT, propertyLinkEarliestStartDate = None),
          propertyValuation1.copy(listType = ListType.PREVIOUS, propertyLinkEarliestStartDate = None)
        )
        val history = propertyHistory.copy(history = currentPropertyValuations, localAuthorityCode = "1English")

        when(mockPropertyLinkConnector.getPropertyHistory(any())(any())).thenReturn(Future.successful(history))

        val res: Future[LocalDate] = testService.findEarliestStartDate(propertyHistory.uarn)

        res.futureValue shouldBe earliestEnglishStartDate
        verify(mockPropertyLinkConnector).getPropertyHistory(any())(any())
      }
    }

    "property is welsh" should {
      "return earliest start date from property history" in new Setup {

        val earliestStartDateOfCurrentList = LocalDate.of(2019, 5, 1)
        val currentPropertyValuations = Seq(
          propertyValuation1.copy(listType = ListType.CURRENT, propertyLinkEarliestStartDate = None),
          propertyValuation1
            .copy(
              listType = ListType.CURRENT,
              propertyLinkEarliestStartDate = Some(earliestStartDateOfCurrentList.plusDays(1))),
          propertyValuation1
            .copy(listType = ListType.DRAFT, propertyLinkEarliestStartDate = Some(earliestStartDateOfCurrentList)),
          propertyValuation1
            .copy(
              listType = ListType.DRAFT,
              propertyLinkEarliestStartDate = Some(earliestStartDateOfCurrentList.plusDays(2))),
          propertyValuation1
            .copy(
              listType = ListType.CURRENT,
              propertyLinkEarliestStartDate = Some(earliestStartDateOfCurrentList.plusDays(3)))
        )
        val history = propertyHistory.copy(history = currentPropertyValuations, localAuthorityCode = "6Welsh")

        when(mockPropertyLinkConnector.getPropertyHistory(any())(any())).thenReturn(Future.successful(history))

        val res: Future[LocalDate] = testService.findEarliestStartDate(propertyHistory.uarn)

        res.futureValue shouldBe earliestStartDateOfCurrentList
        verify(mockPropertyLinkConnector).getPropertyHistory(any())(any())
      }

      "return default earliest welsh start date from config" in new Setup {

        val currentPropertyValuations = Seq(
          propertyValuation1.copy(listType = ListType.DRAFT, propertyLinkEarliestStartDate = None),
          propertyValuation1.copy(listType = ListType.CURRENT, propertyLinkEarliestStartDate = None),
          propertyValuation1.copy(listType = ListType.PREVIOUS, propertyLinkEarliestStartDate = None)
        )
        val history = propertyHistory.copy(history = currentPropertyValuations, localAuthorityCode = "6Welsh")

        when(mockPropertyLinkConnector.getPropertyHistory(any())(any())).thenReturn(Future.successful(history))

        val res: Future[LocalDate] = testService.findEarliestStartDate(propertyHistory.uarn)

        res.futureValue shouldBe earliestWelshStartDate
        verify(mockPropertyLinkConnector).getPropertyHistory(any())(any())
      }
    }
  }
}
