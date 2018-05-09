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

import config.AuthorisationFailed
import controllers.ControllerSpec
import models.{Accounts, DetailedIndividualAccount, GroupAccount}
import org.scalacheck.Arbitrary._
import resources._
import uk.gov.hmrc.http.HeaderCarrier
import utils.StubServicesConfig
class BusinessRatesAuthorisationSpec extends ControllerSpec {

  implicit val hc = HeaderCarrier()

  class Setup {
    val connector = new BusinessRatesAuthorisation(StubServicesConfig, mockWSHttp) {
      override val url: String = "tst-url"
    }
  }

  "authenticate" must "return a successful authentication result if authentication was successful" in new Setup {
    val validGroupAccount = arbitrary[GroupAccount].sample.get
    val validIndividualAccount = arbitrary[DetailedIndividualAccount].sample.get
    val validAccounts = Accounts(validGroupAccount, validIndividualAccount)
    val authenticationSuccessResult = Authenticated(validAccounts)

    mockHttpGET[Accounts]("tst-url", validAccounts)
    whenReady(connector.authenticate)(_ mustBe authenticationSuccessResult)
  }

  "authenticate" must "return a failed authentication result and be handled if authentication was unsuccessful" in new Setup {
    mockHttpFailedGET[Accounts]("tst-url", AuthorisationFailed("NO_CUSTOMER_RECORD"))
    whenReady(connector.authenticate)(_ mustBe NoVOARecord)
  }

}
