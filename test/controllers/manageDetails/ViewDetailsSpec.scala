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

package controllers.manageDetails

import config.ApplicationConfig
import connectors._
import controllers.VoaPropertyLinkingSpec
import models._
import models.messages.MessageCount
import org.mockito.ArgumentMatchers.{any, anyLong}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class ViewDetailsSpec extends VoaPropertyLinkingSpec with MockitoSugar {

  implicit val request = FakeRequest()

  val addressesConnector = mock[Addresses]
  val mockConfig = mock[ApplicationConfig]

  object TestViewDetails
      extends ViewDetails(
        mockCustomErrorHandler,
        preAuthenticatedActionBuilders()
      )

  class TestCase {
    val controller = TestViewDetails

    val personalAddress = new Address(Some(1234L), "Personal address line 1", "line 2", "line 3", "line 4", "AA1 1PP")
    val messageCount = MessageCount(5, 100)

    when(addressesConnector.findById(anyLong)(any[HeaderCarrier])).thenReturn(Future.successful(Some(personalAddress)))
    when(mockConfig.pingdomToken).thenReturn(Some("token"))
    when(mockConfig.bannerContent).thenReturn(None)
  }

  "show" should "display individual page when affinityGroup is Individual" in new TestCase {
    val res = TestViewDetails.show()(request)

    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some("http://localhost:9542/business-rates-dashboard/your-details")
  }

  "show" should "display organisation page when affinityGroup is Organisation" in new TestCase {
    val res = TestViewDetails.show()(request)

    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some("http://localhost:9542/business-rates-dashboard/your-details")

  }

}
