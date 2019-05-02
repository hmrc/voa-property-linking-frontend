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

import auditing.AuditingService
import connectors.{Addresses, TaxEnrolmentConnector}
import models.Address
import org.mockito.ArgumentMatchers.{eq => matching, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, MustMatchers}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, NotFoundException}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EnrolmentServiceSpec extends ServiceSpec {

  val mockAddresses: Addresses = mock[Addresses]
  val mockTaxEnrolmentConnector: TaxEnrolmentConnector = mock[TaxEnrolmentConnector]
  val mockAuditing = mock[AuditingService]
  val enrolmentService: EnrolmentService = new EnrolmentService(mockTaxEnrolmentConnector, mockAddresses, mockAuditing)

  implicit val fakeRequest = FakeRequest()

  "enrol" should " return success with valid details" in {
    when(mockAddresses.findById(any())(any())).thenReturn(Future.successful(Address(Some(1), "", "", "", "", "")))
    when(mockTaxEnrolmentConnector.enrol(any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(204)))
    val result = enrolmentService.enrol(1l, 1)
    result.futureValue must be(Success)
  }

  "enrol" should " return failure when None is return for the address" in {
    when(mockAddresses.findById(any())(any())).thenReturn(Future.failed(new NotFoundException("missing address")))
    val result = enrolmentService.enrol(1l, 1)
    result.futureValue must be(Failure)
  }

  implicit private val hc: HeaderCarrier = HeaderCarrier()
}
