/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers.details

import connectors.Authenticated
import controllers.{ControllerSpec, DetailsController}
import models.Accounts
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.{StubAddresses, StubAuthentication, StubGroupAccountConnector, StubIndividualAccountConnector}
import resources._

class DetailsControllerSpec extends  ControllerSpec {
  private object TestDetailsController extends DetailsController {
    override val authenticated = StubAuthentication
    override val addressesConnector = StubAddresses
    override val individualAccountConnector = StubIndividualAccountConnector
  }

  val request = FakeRequest().withSession(token)
  "Edit email page, when submitting a valid email" must "redirect to details page" in {
    stubLoggedInUser()

    val validFormData: Seq[(String, String)] = Seq(
      "email" -> "newEmail@email.com"
    )
    val res = TestDetailsController.personalEmailSubmit() (request.withFormUrlEncodedBody(validFormData:_*))
    redirectLocation(res) must be (Some(controllers.routes.DetailsController.show.url))
    status(res) must be (SEE_OTHER)
  }

  "Edit name page, when submitting a valid first and last name" must "redirect to details page" in {
    stubLoggedInUser()

    val validFormData: Seq[(String, String)] = Seq(
      "firstName" -> "firstName",
      "lastName" -> "lastName"
    )
    val res = TestDetailsController.personalNameSubmit() (request.withFormUrlEncodedBody(validFormData:_*))
    redirectLocation(res) must be (Some(controllers.routes.DetailsController.show.url))
    status(res) must be (SEE_OTHER)
  }

  "Edit personal telephone page, when submitting a valid telephone" must "redirect to details page" in {
    stubLoggedInUser()

    val validFormData: Seq[(String, String)] = Seq(
      "telephone" -> "123456789"
    )
    val res = TestDetailsController.personalTelephoneSubmit() (request.withFormUrlEncodedBody(validFormData:_*))
    redirectLocation(res) must be (Some(controllers.routes.DetailsController.show.url))
    status(res) must be (SEE_OTHER)
  }

  "Edit personal address page, when submitting a valid address" must "redirect to details page" in {
    stubLoggedInUser()

    val validFormData: Seq[(String, String)] = Seq(
      "address.addressUnitId" -> "123456789",
      "address.line1" -> "Somewhere",
      "address.line2" -> "Over the rainbow",
      "address.line3" -> "way",
      "address.line4" -> "up high",
      "address.postcode" -> "BN1 1NA"
    )
    val res = TestDetailsController.personalAddressSubmit() (request.withFormUrlEncodedBody(validFormData:_*))
    redirectLocation(res) must be (Some(controllers.routes.DetailsController.show.url))
    status(res) must be (SEE_OTHER)
  }

  private def stubLoggedInUser() = {
    val groupAccount = groupAccountGen.sample.get
    val individual = individualGen.sample.get
    StubGroupAccountConnector.stubAccount(groupAccount)
    StubIndividualAccountConnector.stubAccount(individual)
    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(groupAccount, individual)))
    (groupAccount, individual)
  }
}
