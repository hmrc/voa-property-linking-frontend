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

import controllers.{ControllerSpec, GroupAccountDetails}
import models.Address
import org.scalacheck.Arbitrary._
import play.api.libs.json.{JsNumber, JsValue, Json}
import resources._
import uk.gov.hmrc.http.HeaderCarrier
import utils.StubServicesConfig

class AddressesSpec extends ControllerSpec {

  implicit val hc = HeaderCarrier()

  class Setup {
    val connector = new Addresses(StubServicesConfig, mockWSHttp) {
      override val url: String = "tst-url"
    }
  }

  "findByPostcode" must "return a sequence of addresses based on the postcode" in new Setup {
    val validAddresses = Seq(arbitrary[Address].sample.get)

    mockHttpGET[Seq[Address]]("tst-url", validAddresses)
    whenReady(connector.findByPostcode("AB12 C34"))(_ mustBe validAddresses)
  }

  "findById" must "return an address based on the ID" in new Setup {
    val validAddress = arbitrary[Address].sample.get

    mockHttpGETOption[Address]("tst-url", validAddress)
    whenReady(connector.findById(1))(_ mustBe Some(validAddress))
  }

  "create" must "create an address and return the ID" in new Setup {
    val validAddress = arbitrary[Address].sample.get
    val addressId = Json.obj("id" -> 1)

    mockHttpPOST[Address, JsValue]("tst-url", addressId)
    whenReady(connector.create(validAddress))(_ mustBe 1L)
  }

  "registerAddress" must "register a new address if it doesn't already exist in the group account details" in new Setup {
    val validAddress = arbitrary[Address].sample.get
    val validGroupAccountDetails = GroupAccountDetails(
      companyName = "Test Company",
      address = validAddress.copy(addressUnitId = None),
      email = "email@email.com",
      confirmedEmail = "email@email.com",
      phone = "123456789",
      isAgent = false)

    val addressId = Json.obj("id" -> 1)
    mockHttpPOST[Address, JsValue]("tst-url", addressId)

    whenReady(connector.registerAddress(validGroupAccountDetails))(_ mustBe 1L)
  }

  "registerAddress" must "return the existing address ID if it already exists group account details" in new Setup {
    val validAddress = arbitrary[Address].sample.get
    val validGroupAccountDetails = GroupAccountDetails(
      companyName = "Test Company",
      address = validAddress.copy(addressUnitId = Some(1L)),
      email = "email@email.com",
      confirmedEmail = "email@email.com",
      phone = "123456789",
      isAgent = false)

    whenReady(connector.registerAddress(validGroupAccountDetails))(_ mustBe 1L)
  }

}
