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

package models.registration

import java.time.LocalDate

import models.Address

case class FieldData(firstName: String = "",
                     lastName: String = "",
                     postcode: String = "",
                     email: String = "",
                     businessName: String = "",
                     businessPhoneNumber: String = "",
                     businessAddress: Address = Address.empty,
                     isAgent: Boolean = false,
                     nino: String = "",
                     dob: Option[LocalDate] = None,
                     mobilePhone: String = "")

object FieldData {

  def apply(userDetails: UserDetails): FieldData =
    new FieldData(
      firstName = userDetails.firstName.getOrElse(""),
      lastName = userDetails.lastName.getOrElse(""),
      postcode = userDetails.postcode.getOrElse(""),
      email = userDetails.email,
      businessName = "",
      businessPhoneNumber = "",
      businessAddress = Address(None, "", "", "", "", ""),
      isAgent = true
    )

  def apply(personDetails: IndividualUserAccountDetails) =
    new FieldData(
      firstName = personDetails.firstName,
      lastName = personDetails.lastName,
      postcode = personDetails.address.postcode,
      email = personDetails.email,
      businessName = personDetails.tradingName.getOrElse(""),
      businessPhoneNumber = personDetails.phone,
      businessAddress = personDetails.address,
      nino = personDetails.nino.nino,
      dob = Some(personDetails.dob),
      mobilePhone = personDetails.mobilePhone,
      isAgent = false
    )

  def apply(personDetails: AdminOrganisationAccountDetails) =
    new FieldData(
      firstName = personDetails.firstName,
      lastName = personDetails.lastName,
      postcode = personDetails.address.postcode,
      email = personDetails.email,
      businessName = personDetails.companyName,
      businessPhoneNumber = personDetails.phone,
      businessAddress = personDetails.address,
      nino = personDetails.nino.nino,
      dob = Some(personDetails.dob),
      isAgent = personDetails.isAgent
    )

  def apply(personDetails: AdminInExistingOrganisationAccountDetails) =
    new FieldData(
      firstName = personDetails.firstName,
      lastName = personDetails.lastName,
      nino = personDetails.nino.nino,
      dob = Some(personDetails.dob)
    )

  def apply(personDetails: AssistantUserAccountDetails) =
    new FieldData(
      firstName = personDetails.firstName,
      lastName = personDetails.lastName
    )

}
