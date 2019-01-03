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

package connectors

import controllers.VoaPropertyLinkingSpec
import play.api.http.Status._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, NotFoundException}
import utils.StubServicesConfig

class BusinessRatesValuationConnectorSpec extends VoaPropertyLinkingSpec {

  implicit val hc = HeaderCarrier()

  class Setup {
    val connector = new BusinessRatesValuationConnector(StubServicesConfig, mockWSHttp) {
      override val url: String = "tst-url"
    }
  }

  "isViewable" must "return true if detailed valuation is found" in new Setup {
    mockHttpGET[HttpResponse]("tst-url", HttpResponse(OK))
    whenReady(connector.isViewable(1, 1))(_ mustBe true)
  }

  "isViewable" must "return false if the detailed valuation is not found" in new Setup {
    mockHttpFailedGET[HttpResponse]("tst-url", new NotFoundException("Detailed valuation not found"))
    whenReady(connector.isViewable(1, 1))(_ mustBe false)
  }

}
