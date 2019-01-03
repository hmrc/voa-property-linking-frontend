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
import models.registration.UserInfo
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HeaderCarrier
import utils.StubServicesConfig

import scala.concurrent.Future

class VPLAuthConnectorSpec extends VoaPropertyLinkingSpec {

  implicit val userInfoFormat = Json.format[UserInfo]
  implicit val userDetailsLinkFormat = Json.format[UserDetailsLink]
  implicit val externalIdFormat = Json.format[ExternalId]

  implicit val hc = HeaderCarrier()

  class Setup {
    val connector = new VPLAuthConnector(StubServicesConfig, mockWSHttp) {
      override val serviceUrl: String = "tst-url/"
    }
  }

  "getUserId" must "returns cred id" in new Setup {
    mockHttpGET[JsValue]("tst-url/auth/authority", Future.successful(Json.parse("""{"credId": "cred-id"}""")))

    whenReady(connector.getUserId)(_ mustBe "cred-id")
  }

}