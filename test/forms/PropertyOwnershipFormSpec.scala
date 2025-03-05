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

package forms

import controllers.VoaPropertyLinkingSpec
import controllers.propertyLinking.ClaimPropertyOwnership
import models.PropertyOwnership
import play.api.data.Form
import utils.FormBindingVerification._

import java.time.LocalDate

class PropertyOwnershipFormSpec extends VoaPropertyLinkingSpec {

  import TestData._

  behavior of "Property ownership form"

  it should "bind when the inputs are all valid" in
    shouldBindTo(form(), validData, PropertyOwnership(LocalDate.of(2017, 4, 20)))

  it should "require a start date if the occupation/ownership started after 1st April 2017" in {
    val data = validData - "interestedStartDate.day" - "interestedStartDate.month" - "interestedStartDate.year"
    verifyMandatoryDate(form(), data, "interestedStartDate", exclusive = false)
  }

  it should "require the start date to be before the end date" in
    verifyOnlyError(
      form(endDate = Some(LocalDate.of(2017, 4, 19))),
      validData,
      field = "interestedStartDate",
      error = "interestedStartDate.error.startDateMustBeBeforeEnd"
    )

  it should "require the start date to be in the past" in {
    val tomorrow = LocalDate.now().plusDays(1)
    verifyOnlyError(
      form(),
      validData
        .updated("interestedStartDate.day", tomorrow.getDayOfMonth.toString)
        .updated("interestedStartDate.month", tomorrow.getMonthValue.toString)
        .updated("interestedStartDate.year", tomorrow.getYear.toString),
      field = "interestedStartDate",
      error = "interestedStartDate.error.dateInFuture"
    )
  }

  object TestData {
    def form(endDate: Option[LocalDate] = None): Form[PropertyOwnership] =
      ClaimPropertyOwnership.ownershipForm(endDate)
    val validData = Map(
      "interestedStartDate.day"   -> "20",
      "interestedStartDate.month" -> "4",
      "interestedStartDate.year"  -> "2017"
    )
  }

}
