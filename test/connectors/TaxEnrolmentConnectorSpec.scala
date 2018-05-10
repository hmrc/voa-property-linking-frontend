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

import controllers.{ControllerSpec, EnrolmentPayload}
import play.api.http.Status._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.global

class TaxEnrolmentConnectorSpec extends ControllerSpec {

  implicit val hc = HeaderCarrier()
  implicit val ec = global

  class Setup {
    val connector = new TaxEnrolmentConnector(mockWSHttp)
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

}
