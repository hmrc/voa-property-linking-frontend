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

import com.google.inject.Inject
import controllers.{EnrolmentPayload, PayLoad, VoaPropertyLinkingSpec}
import play.api.http.Status._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import services.Success

import scala.concurrent.ExecutionContext.global
import play.api.{Configuration, Mode, Play}

class TaxEnrolmentConnectorSpec @Inject() (configuration: Configuration) extends VoaPropertyLinkingSpec {

  implicit val hc = HeaderCarrier()
  implicit val ec = global

  class Setup {
    val connector = new TaxEnrolmentConnector(mockWSHttp, configuration)
  }

  "enrol" must "enrol a user successfully" in new Setup {
    val validHttpResponse = HttpResponse(OK)

    mockHttpPUT[EnrolmentPayload, HttpResponse]("tst-url", validHttpResponse)
    whenReady(connector.enrol(1,"AB12 C34"))(_ mustBe validHttpResponse)
  }

  "enrol" must "throw an exception if enrolment fails" in new Setup {
    val exception = new Exception("Failed enrolment")

    mockHttpFailedPUT[EnrolmentPayload, HttpResponse]("tst-url", exception)
    whenReady(connector.enrol(1,"AB12 C34").failed)(_ mustBe exception)
  }

  "updatePostcode" must "update a postcode and return the result if successful" in new Setup {
    val successEnrolmentResult = Success

    mockHttpPUT[PayLoad, HttpResponse]("tst-url", HttpResponse(OK))
    whenReady(connector.updatePostcode(1,"NE12 W34", "OL00 D1"))(_ mustBe successEnrolmentResult)
  }

  "updatePostcode" must "throw an exception if postcode update fails" in new Setup {
    val exception = new Exception("Failed to update postcode")

    mockHttpFailedPUT[PayLoad, HttpResponse]("tst-url", exception)
    whenReady(connector.updatePostcode(1,"NE12 W34", "OL00 D1").failed)(_ mustBe exception)
  }

}
