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

package controllers.manageDetails

import config.ApplicationConfig
import connectors.{Addresses, VPLAuthConnector, _}
import controllers.VoaPropertyLinkingSpec
import models._
import models.messages.MessageCount
import models.registration.{UserDetails, UserInfo}
import org.mockito.ArgumentMatchers.{any, anyLong}
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary._
import org.scalatest.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
import resources._
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}
import uk.gov.hmrc.auth.core.User
import uk.gov.hmrc.http.HeaderCarrier
import utils.StubAuthentication

import scala.concurrent.Future

class ViewDetailsSpec extends VoaPropertyLinkingSpec with MockitoSugar{

  implicit val request = FakeRequest()

  val addressesConnector = mock[Addresses]
  val authConnector = mock[VPLAuthConnector]
  val mockConfig = mock[ApplicationConfig]

  object TestViewDetails extends ViewDetails(
    addressesConnector,
    StubAuthentication,
    authConnector
  )


  class TestCase {
    val controller = TestViewDetails

    val group = arbitrary[GroupAccount].sample.get.copy(groupId = "has-agent-account", isAgent = true)
    val person = arbitrary[DetailedIndividualAccount].sample.get.copy(externalId = "has-account", organisationId = group.id)
    val personalAddress = new Address(Some(1234L), "Personal address line 1", "line 2", "line 3", "line 4", "AA1 1PP")
    val messageCount = MessageCount(5, 100)

    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(group, person)))
    when(addressesConnector.findById(anyLong)(any[HeaderCarrier])).thenReturn(Future.successful(Some(personalAddress)))
    when(mockConfig.pingdomToken).thenReturn(Some("token"))
    when(mockConfig.editNameEnabled).thenReturn(true)
    when(mockConfig.loggedInUser).thenReturn(Some("loggedInUser"))
    when(mockConfig.isAgentLoggedIn).thenReturn(Some("true"))
    when(mockConfig.analyticsToken).thenReturn("token")
    when(mockConfig.analyticsHost).thenReturn("host")
    when(mockConfig.bannerContent).thenReturn(None)

  }

  "show" must "display individual page when affinityGroup is Individual" in new TestCase {


    val userInfo = UserInfo(Some("firstName"),Some("lastName"),"email@mail.com",Some("AA1 1SS"),"groupIdentifier","gatewayId", Individual, User)
    val userDetails = UserDetails("external-id", userInfo)

    when(authConnector.getUserDetails(any[HeaderCarrier])).thenReturn(Future.successful(userDetails))
    when(authConnector.getAffinityGroup(any[HeaderCarrier])).thenReturn(Future.successful(Individual))

    val res = TestViewDetails.show()(request)

    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some("http://localhost:9542/business-rates-dashboard/your-details")
  }

  "show" must "display organisation page when affinityGroup is Organisation" in new TestCase {
    val userInfo = UserInfo(Some("firstName"),Some("lastName"),"email@mail.com",Some("AA1 1SS"),"groupIdentifier","gatewayId", Organisation, User)
    val userDetails = UserDetails("external-id", userInfo)

    when(authConnector.getUserDetails(any[HeaderCarrier])).thenReturn(Future.successful(userDetails))
    when(authConnector.getAffinityGroup(any[HeaderCarrier])).thenReturn(Future.successful(Organisation))

    val res = TestViewDetails.show()(request)

    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some("http://localhost:9542/business-rates-dashboard/your-details")

  }

}