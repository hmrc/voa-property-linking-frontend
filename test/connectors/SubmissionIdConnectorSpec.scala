/*
 * Copyright 2024 HM Revenue & Customs
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
import uk.gov.hmrc.http.HeaderCarrier

class SubmissionIdConnectorSpec extends VoaPropertyLinkingSpec {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  class Setup {
    val connector = new SubmissionIdConnector(servicesConfig, mockHttpClient)(ec) {
      val url: String = "tst-url"
    }
  }

  "get" should "return a submission ID" in new Setup {
    mockHttpGET[String]("tst-url", "PL12345")
    whenReady(connector.get())(_ shouldBe "PL12345")
  }

}
