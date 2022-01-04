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
import models.{Address, DetailedAddress}
import models.registration.GroupAccountDetails
import org.scalacheck.Arbitrary._
import play.api.libs.json.{JsValue, Json}
import utils._
import uk.gov.hmrc.http.HeaderCarrier

class AddressesSpec extends VoaPropertyLinkingSpec {

  implicit val hc = HeaderCarrier()

  class Setup {
    val connector = new Addresses(servicesConfig, mockHttpClient) {
      override val url: String = "tst-url"
    }
  }

  "findByPostcode" should "return a sequence of addresses based on the postcode" in new Setup {
    val validAddresses = Seq(arbitrary[DetailedAddress].sample.get)

    mockHttpGET[Seq[DetailedAddress]]("tst-url", validAddresses)
    whenReady(connector.findByPostcode("AB12 C34"))(_ shouldBe validAddresses)
  }

  "findById" should "return an address based on the ID" in new Setup {
    val validAddress = arbitrary[Address].sample.get

    mockHttpGETOption[Address]("tst-url", validAddress)
    whenReady(connector.findById(1))(_ shouldBe Some(validAddress))
  }

  "create" should "create an address and return the ID" in new Setup {
    val validAddress = arbitrary[Address].sample.get
    val addressId = Json.obj("id" -> 1)

    mockHttpPOST[Address, JsValue]("tst-url", addressId)
    whenReady(connector.create(validAddress))(_ shouldBe 1L)
  }

  "registerAddress" should "register a new address if it doesn't already exist in the group account details" in new Setup {
    val validAddress = arbitrary[Address].sample.get
    val validGroupAccountDetails = GroupAccountDetails(
      companyName = "Test Company",
      address = validAddress.copy(addressUnitId = None),
      email = "email@email.com",
      confirmedEmail = "email@email.com",
      phone = "123456789",
      isAgent = false
    )

    val addressId = Json.obj("id" -> 1)
    mockHttpPOST[Address, JsValue]("tst-url", addressId)

    whenReady(connector.registerAddress(validGroupAccountDetails))(_ shouldBe 1L)
  }

  "registerAddress" should "return the existing address ID if it already exists group account details" in new Setup {
    val validAddress = arbitrary[Address].sample.get
    val validGroupAccountDetails = GroupAccountDetails(
      companyName = "Test Company",
      address = validAddress.copy(addressUnitId = Some(1L)),
      email = "email@email.com",
      confirmedEmail = "email@email.com",
      phone = "123456789",
      isAgent = false
    )

    whenReady(connector.registerAddress(validGroupAccountDetails))(_ shouldBe 1L)
  }

}
