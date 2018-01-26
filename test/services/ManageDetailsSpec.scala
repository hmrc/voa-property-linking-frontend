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

package services

import org.scalatest._
import connectors.{Addresses, TaxEnrolmentConnector, VPLAuthConnector}
import models.Address
import models.enrolment.{UserDetails, UserInfo}
import org.mockito.ArgumentMatchers.{any, anyInt, eq => matches}
import org.mockito.Mockito.{times, verify, when, never}
import org.scalatest.{FlatSpec, MustMatchers}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class ManageDetailsSpec extends FlatSpec with MustMatchers with MockitoSugar with ScalaFutures with BeforeAndAfterEach {


  "updatePostcode" should "upsert known facts if predicate matches" in {
    updatePostcode(1, 1, 2,true)
    verify(mockTaxEnrolments, once).updatePostcode(matches(1L),any(),matches(mockAddress.postcode))(any(),any())
  }

  "updatePostcode" should "not upsert known facts if predicate does not match" in {
    updatePostcode(3, 3, 4, false)
    verify(mockTaxEnrolments, never()).updatePostcode(any(),any(),any())(any(),any())
  }

  def updatePostcode(personId:Int, addressId:Int, currentAddressId: Int, predicate:Boolean): Unit = {
    Await.result(manageDetails.updatePostcode(personId, currentAddressId, addressId)(_ => predicate)(hc),1 seconds)
  }

  override def beforeEach(): Unit = {
    mockTaxEnrolments = mock[TaxEnrolmentConnector]
    mockVPLAuthConnector = mock[VPLAuthConnector]
    mockAddresses = mock[Addresses]

    when(mockVPLAuthConnector.getUserDetails(any())).thenReturn(Future.successful(mockUserDetails))
    when(mockAddresses.findById(anyInt)(any[HeaderCarrier])).thenReturn(Future.successful(Some(mockAddress)))
    when(mockTaxEnrolments.updatePostcode(any(),any(),any())(any(),any())).thenReturn(Future.successful(Success))
  }

  private var mockTaxEnrolments:TaxEnrolmentConnector = _
  private var mockAddresses:Addresses = _
  private var mockVPLAuthConnector:VPLAuthConnector = _

  private val once = times(1)
  private val mockAddress = Address(None, "1, The Place", "", "", "", "AA11 1AA")
  private val mockUserDetails = UserDetails("123456", UserInfo(None, None, "", Some("ABC"), "", Individual))

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()
  private lazy val manageDetails = new ManageDetailsWithEnrolments(taxEnrolments = mockTaxEnrolments, addresses = mockAddresses, vPLAuthConnector = mockVPLAuthConnector)
}
