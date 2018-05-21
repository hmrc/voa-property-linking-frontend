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

import java.time.LocalDate

import controllers.GroupAccountDetails
import models.{Address, IVDetails}
import play.api.libs.json.Json
import uk.gov.hmrc.domain.Nino

case class EnrolmentOrganisationAccountDetails(firstName: String,
                                               lastName: String,
                                               companyName: String,
                                               address: Address,
                                               dob: LocalDate,
                                               nino: Nino,
                                               phone: String,
                                               email: String,
                                               confirmedEmail: String,
                                               isAgent: Boolean) extends EnrolmentUser {
  override def ivDetails = IVDetails(
    firstName = firstName,
    lastName = lastName,
    dateOfBirth = Some(dob),
    nino = Some(nino)
  )

  def toGroupDetails = GroupAccountDetails(
    companyName = companyName,
    address = address,
    email = email,
    confirmedEmail = confirmedEmail,
    phone = phone,
    isAgent = isAgent
  )
}

object EnrolmentOrganisationAccountDetails {
  implicit val format = Json.format[EnrolmentOrganisationAccountDetails]
}


