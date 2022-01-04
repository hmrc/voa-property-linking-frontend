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

import connectors.authorisation.AuthorisationResult._
import connectors.authorisation.BusinessRatesAuthorisationConnector
import connectors.authorisation.errorhandler.exceptions.AuthorisationFailure
import controllers.VoaPropertyLinkingSpec
import models.{Accounts, DetailedIndividualAccount, GroupAccount}
import org.scalacheck.Arbitrary._
import uk.gov.hmrc.http.HeaderCarrier
import utils._

class BusinessRatesAuthorisationConnectorSpec extends VoaPropertyLinkingSpec {

  implicit val hc = HeaderCarrier()

  class Setup {
    val connector = new BusinessRatesAuthorisationConnector(servicesConfig, mockHttpClient) {
      override val url: String = "tst-url"
    }
  }

  "authenticate" should "return a successful authentication result if authentication was successful" in new Setup {
    val validGroupAccount = arbitrary[GroupAccount].sample.get
    val validIndividualAccount = arbitrary[DetailedIndividualAccount].sample.get
    val validAccounts = Accounts(validGroupAccount, validIndividualAccount)
    val authenticationSuccessResult = Authenticated(validAccounts)

    mockHttpGET[Accounts]("tst-url", validAccounts)
    whenReady(connector.authenticate)(_ shouldBe authenticationSuccessResult)
  }

  "authenticate" should "return a failed authentication result and be handled if authentication was unsuccessful" in new Setup {
    mockHttpFailedGET[Accounts]("tst-url", AuthorisationFailure("NO_CUSTOMER_RECORD"))
    whenReady(connector.authenticate)(_ shouldBe NoVOARecord)
  }
}
