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

import models.domain._
import play.api.libs.json.Json

case class AdminInExistingOrganisationAccountDetails(firstName: String,
                                                     lastName: String,
                                                     dob: LocalDate,
                                                     nino: Nino) extends AdminInExistingOrganisationUser {

  def toAdminOrganisationAccountDetails(fieldData: FieldData) = AdminOrganisationAccountDetails(
    companyName = fieldData.businessName,
    address = fieldData.businessAddress,
    email = fieldData.email,
    confirmedEmail = fieldData.email,
    phone = fieldData.businessPhoneNumber,
    isAgent = fieldData.isAgent,
    firstName = firstName,
    lastName = lastName,
    dob = dob,
    nino = nino
  )
}

object AdminInExistingOrganisationAccountDetails{
  implicit val format = Json.format[AdminInExistingOrganisationAccountDetails]
  implicit val formatGroupAccountDetails = Json.format[GroupAccountDetails]
}