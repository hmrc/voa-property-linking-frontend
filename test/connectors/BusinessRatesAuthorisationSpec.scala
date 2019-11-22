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

import config.AuthorisationFailed
import connectors.authorisation.AuthorisationResult._
import connectors.authorisation.BusinessRatesAuthorisation
import controllers.VoaPropertyLinkingSpec
import models.{Accounts, DetailedIndividualAccount, GroupAccount}
import org.scalacheck.Arbitrary._
import resources._
import uk.gov.hmrc.http.{HeaderCarrier, Upstream4xxResponse}

class BusinessRatesAuthorisationSpec extends VoaPropertyLinkingSpec {

  implicit val hc = HeaderCarrier()

  class Setup {
    val connector = new BusinessRatesAuthorisation(servicesConfig, mockWSHttp) {
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

  "authorise" must "return a successful authentication result if the user is authorised to view an assessment" in new Setup {
    val validGroupAccount = arbitrary[GroupAccount].sample.get
    val validIndividualAccount = arbitrary[DetailedIndividualAccount].sample.get
    val validAccounts = Accounts(validGroupAccount, validIndividualAccount)
    val authenticationSuccessResult = Authenticated(validAccounts)

    mockHttpGET[Accounts]("tst-url", validAccounts)
    whenReady(connector.authorise(1,1))(_ mustBe authenticationSuccessResult)
  }

  "authorise" must "return a forbidden response if the user is not authorised to view an assessment" in new Setup {
    val forbiddenResponse = Upstream4xxResponse("FORBIDDEN", 403, 403, Map.empty)
    mockHttpFailedGET[Accounts]("tst-url", forbiddenResponse)
    whenReady(connector.authorise(1,1).failed)(_ mustBe forbiddenResponse)
  }

  "authorise" must "return a successful authentication result if the user is authorised to view assessments for a property link" in new Setup {
    val validGroupAccount = arbitrary[GroupAccount].sample.get
    val validIndividualAccount = arbitrary[DetailedIndividualAccount].sample.get
    val validAccounts = Accounts(validGroupAccount, validIndividualAccount)
    val authenticationSuccessResult = Authenticated(validAccounts)

    mockHttpGET[Accounts]("tst-url", validAccounts)
    whenReady(connector.authorise(1))(_ mustBe authenticationSuccessResult)
  }

  "authorise" must "return a forbidden response if the user is not authorised to view assessments for a property link" in new Setup {
    val forbiddenResponse = Upstream4xxResponse("FORBIDDEN", 403, 403, Map.empty)
    mockHttpFailedGET[Accounts]("tst-url", forbiddenResponse)
    whenReady(connector.authorise(1))(_ mustBe ForbiddenResponse)
  }

}
