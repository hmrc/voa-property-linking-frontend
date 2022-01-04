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

package services

import auditing.AuditingService
import connectors.TaxEnrolmentConnector
import models.Address
import org.mockito.ArgumentMatchers.{any, eq => mEq}
import org.mockito.Mockito._
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class EnrolmentServiceSpec extends ServiceSpec {

  val mockTaxEnrolmentConnector: TaxEnrolmentConnector = mock[TaxEnrolmentConnector]
  val mockAuditing = mock[AuditingService]
  val enrolmentService: EnrolmentService = new EnrolmentService(mockTaxEnrolmentConnector, mockAddresses, mockAuditing)

  implicit val fakeRequest = FakeRequest()

  "enrol" should {
    "return success with valid details" in {
      when(mockAddresses.findById(any())(any()))
        .thenReturn(Future.successful(Some(Address(Some(1), "l1", "l2", "", "", "BN1 2CD"))))
      when(mockTaxEnrolmentConnector.enrol(any(), any())(any(), any()))
        .thenReturn(Future.successful(emptyJsonHttpResponse(204)))

      enrolmentService.enrol(1L, 1).futureValue shouldBe Success

      verify(mockTaxEnrolmentConnector).enrol(mEq(1L), mEq("BN1 2CD"))(any(), any())
    }

    "return failure when None is returned for the address" in {
      when(mockAddresses.findById(any())(any())).thenReturn(Future.successful(None))
      enrolmentService.enrol(1L, 1).futureValue shouldBe Failure
    }

    "skip enrolling a user that has a blank postcode" in {
      reset(mockTaxEnrolmentConnector)
      when(mockAddresses.findById(any())(any()))
        .thenReturn(Future.successful(Some(Address(Some(1), "l1", "l2", "", "", "   "))))

      enrolmentService.enrol(1L, 1).futureValue shouldBe Success

      verify(mockTaxEnrolmentConnector, never()).enrol(any(), any())(any(), any())
    }

    "skip enrolling a user that has an invalid postcode (does not match our postcode regex)" in {
      reset(mockTaxEnrolmentConnector)
      when(mockAddresses.findById(any())(any()))
        .thenReturn(Future.successful(Some(Address(Some(1), "l1", "l2", "", "", "HAPPY POSTCODE"))))

      enrolmentService.enrol(1L, 1).futureValue shouldBe Success

      verify(mockTaxEnrolmentConnector, never()).enrol(any(), any())(any(), any())
    }
  }

  implicit private val hc: HeaderCarrier = HeaderCarrier()

}
