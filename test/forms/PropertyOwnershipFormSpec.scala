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

package forms

import controllers.VoaPropertyLinkingSpec
import controllers.propertyLinking.ClaimPropertyOwnership
import models.PropertyOwnership
import play.api.data.Form
import utils.FormBindingVerification._
import views.helpers.Errors

import java.time.LocalDate

class PropertyOwnershipFormSpec extends VoaPropertyLinkingSpec {

  import TestData._

  behavior of "Property ownership form"

  it should "bind when the inputs are all valid" in {
    shouldBindTo(form(), validData, PropertyOwnership(false, Some(LocalDate.of(2017, 4, 20))))
  }

  it should "require a start date if the occupation/ownership started after 1st April 2017" in {
    val data = validData.updated("interestedOnOrBefore", "false") - "fromDate.day" - "fromDate.month" - "fromDate.year"
    verifyMandatoryDate(form(), data, "fromDate", false)
  }

  it should "not require the start date if the occupation/ownership started before 1st April 2017" in {
    val data = validData.updated("interestedOnOrBefore", "true")
    verifyOptionalDate(form(), data, "fromDate")
  }

  it should "require the start date to be after 1 April 2017" in {
    val data = validData
      .updated("interestedOnOrBefore", "false")
      .updated("fromDate.day", "1")
      .updated("fromDate.month", "3")
      .updated("fromDate.year", "2017")
    verifyOnlyError(form(), data, "fromDate", Errors.dateMustBeAfter)
  }

  it should "require the start date to be before the end date" in {
    verifyOnlyError(
      form(endDate = Some(LocalDate.of(2017, 4, 19))),
      validData,
      field = "fromDate",
      error = "interestedOnOrBefore.error.startDateMustBeBeforeEnd")
  }

  object TestData {
    def form(endDate: Option[LocalDate] = None): Form[PropertyOwnership] =
      ClaimPropertyOwnership.ownershipForm(earliestEnglishStartDate, endDate)
    val validData = Map(
      "interestedOnOrBefore" -> "false",
      "fromDate.day"         -> "20",
      "fromDate.month"       -> "4",
      "fromDate.year"        -> "2017"
    )
  }

}
