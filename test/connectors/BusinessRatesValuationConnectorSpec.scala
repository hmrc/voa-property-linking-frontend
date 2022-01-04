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

package connectors

import controllers.VoaPropertyLinkingSpec
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.when
import play.api.http.Status._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future

class BusinessRatesValuationConnectorSpec extends VoaPropertyLinkingSpec {

  implicit val hc = HeaderCarrier()

  class Setup {
    val connector = new BusinessRatesValuationConnector(servicesConfig, mockHttpClient)
  }

  "isViewable" should "return true if detailed valuation is found" in new Setup {
    when(mockHttpClient.GET[HttpResponse](any(), any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(emptyJsonHttpResponse(OK)))

    whenReady(connector.isViewable(1, 1, "PL123"))(_ shouldBe true)
  }

  "isViewable" should "return false if the detailed valuation is not found" in new Setup {
    when(mockHttpClient.GET[HttpResponse](any(), any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(emptyJsonHttpResponse(NOT_FOUND)))

    whenReady(connector.isViewable(1, 1, "PL123"))(_ shouldBe false)
  }

}
