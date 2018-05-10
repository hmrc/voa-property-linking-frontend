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

import config.VPLHttp
import controllers.ControllerSpec
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.StubServicesConfig
import play.api.http.Status._

import scala.concurrent.ExecutionContext.Implicits.global

class EnvelopeConnectorSpec extends ControllerSpec {

  implicit val hc = HeaderCarrier()
  implicit val ec = global

  override lazy val mockWSHttp = mock[VPLHttp]

  class Setup {
    val connector = new EnvelopeConnector(StubServicesConfig, mockWSHttp)(ec)
  }

  "createEnvelope" must "post envelope metadata and return the envelope ID" in new Setup {
    val envelopeMetadata = EnvelopeMetadata("SUBMISSION_ID", 1)
    val expectedResponse = Json.obj("envelopeId" -> "12345")

    mockHttpPOST[EnvelopeMetadata, JsValue]("tst-url", expectedResponse)
    whenReady(connector.createEnvelope(envelopeMetadata))(_ mustBe "12345")
  }

  "storeEnvelope" must "store an envelope and return the envelope ID" in new Setup {
    mockHttpPOST[JsValue, HttpResponse]("tst-url", HttpResponse(OK))
    whenReady(connector.storeEnvelope("12345"))(_ mustBe "12345")
  }

  "closeEnvelope" must "close an envelope and return the envelope ID" in new Setup {
    mockHttpPUT[JsValue, HttpResponse]("tst-url", HttpResponse(OK))
    whenReady(connector.closeEnvelope("12345"))(_ mustBe "12345")
  }

}
