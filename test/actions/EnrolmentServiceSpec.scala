/*
 * Copyright 2018 HM Revenue & Customs
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

package actions

import connectors.{Addresses, VPLAuthConnector}
import models.Address
import org.mockito.ArgumentMatchers.{eq => matching, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, MustMatchers}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future

class EnrolmentServiceSpec extends FlatSpec with MustMatchers with MockitoSugar with ScalaFutures {

  val mockAddresses = mock[Addresses]
  val mockAuthConnector = mock[VPLAuthConnector]
  val mockTaxEnrolmentConnector = mock[TaxEnrolmentConnector]
  val enrolmentService = new EnrolmentService(mockTaxEnrolmentConnector, mockAuthConnector, mockAddresses)

  implicit val hc = HeaderCarrier()

  "Enrolment Service " should " return success" in {
    when(mockAuthConnector.getUserId(any())).thenReturn(Future.successful(""))
    when(mockAddresses.findById(any())(any())).thenReturn(Future.successful(Some(Address(Some(1), "", "", "", "", ""))))
    when(mockTaxEnrolmentConnector.enrol(1l, "", "")).thenReturn(Future.successful(HttpResponse(204)))
    val result = enrolmentService.enrol(1l, 1)
    result.futureValue must be(Success)
  }

  "Enrolment Service" should " return failure" in {
    when(mockAuthConnector.getUserId(any())).thenReturn(Future.successful(""))
    when(mockAddresses.findById(any())(any())).thenReturn(Future.successful(None))
    val result = enrolmentService.enrol(1l, 1)
    result.futureValue must be(Failure)
  }
}
