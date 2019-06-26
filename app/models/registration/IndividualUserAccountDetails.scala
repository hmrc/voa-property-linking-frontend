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

import models.{email => _, _}
import play.api.libs.json.Json
import uk.gov.hmrc.domain.Nino

case class IndividualUserAccountDetails(firstName: String,
                                        lastName: String,
                                        address: Address,
                                        dob: LocalDate,
                                        nino: Nino,
                                        phone: String,
                                        mobilePhone: String,
                                        email: String,
                                        confirmedEmail: String,
                                        tradingName: Option[String],
                                        selectedAddress: Option[String] = None
                                            ) extends AdminUser {

  override def toIvDetails = IVDetails(
    firstName = firstName,
    lastName = lastName,
    dateOfBirth = Some(dob),
    nino = Some(nino)
  )

  def toGroupDetails = GroupAccountDetails(
    companyName = tradingName.getOrElse(truncateCompanyName(s"$firstName $lastName")),
    address = address,
    email = email,
    confirmedEmail = confirmedEmail,
    phone = phone,
    isAgent = false
  )

  override def toIndividualAccountSubmission(trustId: String)(user: UserDetails)(id: Long)(organisationId: Option[Long]) = IndividualAccountSubmission(
    externalId = user.externalId,
    trustId = trustId,
    organisationId = organisationId,
    details = IndividualDetails(firstName, lastName, email, phone, Some(mobilePhone), id)
  )

  private def truncateCompanyName(companyName: String): String = {
    companyName.take(45).toString
  }
}

object IndividualUserAccountDetails {
  implicit val format = Json.format[IndividualUserAccountDetails]
}