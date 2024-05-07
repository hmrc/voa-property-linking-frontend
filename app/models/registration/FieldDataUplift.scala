/*
 * Copyright 2024 HM Revenue & Customs
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

package models.registration

import models.Address

case class FieldDataUplift(
      postcode: String = "",
      email: String = "",
      businessName: String = "",
      businessPhoneNumber: String = "",
      businessAddress: Address = Address.empty,
      isAgent: Boolean = false,
      mobilePhone: String = "",
      selectedAddress: Option[String] = None)

object FieldDataUplift {

  def apply(userDetails: UserDetails): FieldDataUplift = {
    val fullName = userDetails.firstName.getOrElse("").split(" ")

    new FieldDataUplift(
      postcode = userDetails.postcode.getOrElse(""),
      email = userDetails.email,
      businessName = "",
      businessPhoneNumber = "",
      businessAddress = Address(None, "", "", "", "", ""),
      isAgent = false
    )
  }

  def apply(personDetails: IndividualUserAccountDetailsUplift) =
    new FieldDataUplift(
      postcode = personDetails.address.postcode,
      email = personDetails.email,
      businessName = personDetails.tradingName.getOrElse(""),
      businessPhoneNumber = personDetails.phone,
      businessAddress = personDetails.address,
      mobilePhone = personDetails.mobilePhone,
      isAgent = false,
      selectedAddress = personDetails.selectedAddress
    )

  def apply(personDetails: AdminOrganisationAccountDetailsUplift) =
    new FieldDataUplift(
      postcode = personDetails.address.postcode,
      email = personDetails.email,
      businessName = personDetails.companyName,
      businessPhoneNumber = personDetails.phone,
      businessAddress = personDetails.address,
      isAgent = personDetails.isAgent,
      selectedAddress = personDetails.selectedAddress
    )
//TODO: Confirm Admin ?
  def apply(personDetails: AdminInExistingOrganisationAccountDetailsUplift) =
    new FieldDataUplift(
      isAgent = false
    )

  def apply(personDetails: AssistantUserAccountDetails) =
    new FieldDataUplift(
      isAgent = false
    )

}
