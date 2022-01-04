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

package models

import java.time.LocalDate

import models.domain._
import play.api.libs.json.{Json, OFormat}

case class PersonalDetails(
      firstName: String,
      lastName: String,
      dateOfBirth: LocalDate,
      nino: Nino,
      email: String,
      confirmedEmail: String,
      phone1: String,
      phone2: Option[String],
      address: Address) {

  def individualDetails: IndividualDetails =
    IndividualDetails(
      firstName,
      lastName,
      email,
      phone1,
      phone2,
      address.addressUnitId.getOrElse(throw new Exception("Address ID not set")))

  def withAddressId(addressId: Long): PersonalDetails = copy(address = address.copy(addressUnitId = Some(addressId)))

}

object PersonalDetails {
  implicit val format: OFormat[PersonalDetails] = Json.format
}
