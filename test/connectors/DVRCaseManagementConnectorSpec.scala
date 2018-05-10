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

package connectors

import controllers.VoaPropertyLinkingSpec
import models.DetailedValuationRequest
import play.api.http.Status._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.StubServicesConfig

class DVRCaseManagementConnectorSpec extends VoaPropertyLinkingSpec {

  implicit val hc = HeaderCarrier()

  class Setup {
    val connector = new DVRCaseManagementConnector(StubServicesConfig, mockWSHttp) {
      override val url: String = "tst-url"
    }
  }

  "requestDetailedValuation" must "successfully post a detailed valuation request" in new Setup {
    val detailedValuationRequest = mock[DetailedValuationRequest]

    mockHttpPOST[DetailedValuationRequest, HttpResponse]("tst-url", HttpResponse(OK))
    whenReady(connector.requestDetailedValuation(detailedValuationRequest))(_ mustBe ())
  }

}
