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
import models.identityVerificationProxy.IvResult.{IvFailure, IvSuccess}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HeaderCarrier

class IdentityVerificationConnectorSpec extends VoaPropertyLinkingSpec {

  implicit val hc = HeaderCarrier()

  class Setup {
    val connector = new IdentityVerificationConnector(servicesConfig, applicationConfig, mockHttpClient)
  }

  "verifySuccess" should "return true if IV was successful" in new Setup {
    val ivResult = Json.obj("result" -> "Success")

    mockHttpGET[JsValue]("tst-url", ivResult)
    whenReady(connector.verifySuccess("JOURNEY_ID"))(_ shouldBe true)
  }

  "verifySuccess" should "return false for any other response" in new Setup {
    val ivResult = Json.obj("result" -> "Something else")

    mockHttpGET[JsValue]("tst-url", ivResult)
    whenReady(connector.verifySuccess("JOURNEY_ID"))(_ shouldBe false)
  }

  "journeyStatus" should "return the appropriate IvResult" in new Setup {
    val ivResult = Json.obj("result" -> "Success")

    mockHttpGET[JsValue]("tst-url", ivResult)
    whenReady(connector.journeyStatus("JOURNEY_ID"))(_ == IvSuccess)
  }

  "journeyStatus" should "return TechnicalIssue IvFailure for anything else" in new Setup {
    val ivResult = Json.obj("result" -> "Something else")

    mockHttpGET[JsValue]("tst-url", ivResult)
    whenReady(connector.journeyStatus("JOURNEY_ID"))(_ shouldBe IvFailure.TechnicalIssue)
  }

}
