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

import com.google.inject.Inject
import controllers.{EnrolmentPayload, PayLoad, VoaPropertyLinkingSpec}
import play.api.http.Status._
import services.Success
import tests.AllMocks
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class TaxEnrolmentConnectorSpec @Inject()(servicesConfig: ServicesConfig) extends VoaPropertyLinkingSpec with AllMocks {

  implicit val hc = HeaderCarrier()

  class Setup {
    val connector = new TaxEnrolmentConnector(mockHttpClient, mockAuditingService, servicesConfig)
  }

  "enrol" should "enrol a user successfully" in new Setup {
    val validHttpResponse = emptyJsonHttpResponse(OK)

    mockHttpPUT[EnrolmentPayload, HttpResponse]("tst-url", validHttpResponse)
    whenReady(connector.enrol(1, "AB12 C34"))(_ shouldBe validHttpResponse)
  }

  "enrol" should "throw an exception if enrolment fails" in new Setup {
    val exception = new Exception("Failed enrolment")

    mockHttpFailedPUT[EnrolmentPayload, HttpResponse]("tst-url", exception)
    whenReady(connector.enrol(1, "AB12 C34").failed)(_ shouldBe exception)
  }

  "updatePostcode" should "update a postcode and return the result if successful" in new Setup {
    val successEnrolmentResult = Success

    mockHttpPUT[PayLoad, HttpResponse]("tst-url", emptyJsonHttpResponse(OK))
    whenReady(connector.updatePostcode(1, "NE12 W34", "OL00 D1"))(_ shouldBe successEnrolmentResult)
  }

  "updatePostcode" should "throw an exception if postcode update fails" in new Setup {
    val exception = new Exception("Failed to update postcode")

    mockHttpFailedPUT[PayLoad, HttpResponse]("tst-url", exception)
    whenReady(connector.updatePostcode(1, "NE12 W34", "OL00 D1").failed)(_ shouldBe exception)
  }

}
