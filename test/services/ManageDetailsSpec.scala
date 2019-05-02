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

import config.ApplicationConfig
import connectors.{Addresses, TaxEnrolmentConnector, VPLAuthConnector}
import models.Address
import models.registration.{UserDetails, UserInfo}
import org.mockito.ArgumentMatchers.{any, anyLong, eq => matches}
import org.mockito.Mockito.{never, times, verify, when}
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.auth.core.User
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class ManageDetailsSpec extends ServiceSpec {

  implicit val request = FakeRequest()

  "updatePostcode" should "upsert known facts if predicate matches" in {
    updatePostcode(1, 1, 2, true)
    verify(mockTaxEnrolments, once).updatePostcode(matches(1L), any(), matches(mockAddress.postcode))(any(), any())
  }

  "updatePostcode" should "not upsert known facts if predicate does not match" in {
    updatePostcode(3, 3, 4, false)
    verify(mockTaxEnrolments, never()).updatePostcode(any(), any(), any())(any(), any())
  }

  def updatePostcode(personId: Int, addressId: Long, currentAddressId: Long, predicate: Boolean): Unit = {
    Await.result(manageDetails.updatePostcode(personId, currentAddressId, addressId)(_ => predicate)(hc, request), 1 seconds)
  }

  override def beforeEach(): Unit = {
    mockTaxEnrolments = mock[TaxEnrolmentConnector]
    mockVPLAuthConnector = mock[VPLAuthConnector]
    mockAddresses = mock[Addresses]

    when(mockVPLAuthConnector.getUserDetails(any())).thenReturn(Future.successful(mockUserDetails))
    when(mockAddresses.findById(anyLong)(any[HeaderCarrier])).thenReturn(Future.successful(mockAddress))
    when(mockTaxEnrolments.updatePostcode(any(), any(), any())(any(), any())).thenReturn(Future.successful(Success))
  }

  private var mockTaxEnrolments: TaxEnrolmentConnector = _
  private var mockAddresses: Addresses = _
  private var mockVPLAuthConnector: VPLAuthConnector = _

  private val once = times(1)
  private val mockAddress = Address(Some(1L), "1, The Place", "", "", "", "AA11 1AA")
  private val mockUserDetails = UserDetails("123456", UserInfo(None, None, "", Some("ABC"), "", "654321", Individual, User))

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()
  private lazy val manageDetails = new ManageVoaDetails(taxEnrolments = mockTaxEnrolments, addresses = mockAddresses, vPLAuthConnector = mockVPLAuthConnector, config = mock[ApplicationConfig])
}
