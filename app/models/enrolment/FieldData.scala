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

package models.enrolment

import models.Address

case class FieldData(firstName: String = "",
                     lastName: String = "",
                     postcode: String = "",
                     email: String = "",
                     businessName: String = "",
                     businessPhoneNumber: String = "",
                     businessAddress: Address = Address(addressUnitId = None, line1 = "", line2 = "", line3 = "", line4 = "", postcode = ""),
                     isAgent: Boolean = false)

object FieldData {

  def apply(userInfo: UserInfo): FieldData =
    new FieldData(
      firstName = userInfo.firstName.getOrElse(""),
      lastName = userInfo.lastName.getOrElse(""),
      postcode = userInfo.postcode.getOrElse(""),
      email = userInfo.email,
      businessName = "",
      businessPhoneNumber = "",
      businessAddress = Address(None, "", "", "", "", ""),
      isAgent = true
    )

  def apply(map: Map[String, String]): FieldData =
    new FieldData(
      firstName = "",
      lastName = "",
      postcode = map("address.postcode"),
      email = map("email"),
      businessName = map("companyName"),
      businessPhoneNumber = map("phone"),
      businessAddress = Address(None,  map("address.line1"), map("address.line2"), map("address.line3"), map("address.line4"),  map("address.postcode")),
      isAgent = map("isAgent").toBoolean
    )
}
