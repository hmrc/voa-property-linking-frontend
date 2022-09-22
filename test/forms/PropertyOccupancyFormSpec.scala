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

import java.time.LocalDate
import controllers.VoaPropertyLinkingSpec
import controllers.propertyLinking.ClaimPropertyOccupancy
import models.PropertyOccupancy
import play.api.i18n.Lang.defaultLang
import utils.FormBindingVerification._

class PropertyOccupancyFormSpec extends VoaPropertyLinkingSpec {

  import TestData._

  val errorLastDateAfterStartDate = "error.date.mustBeAfterStartDate"

  behavior of "Property occupancy form"

  it should "bind when the inputs are all valid" in {
    shouldBindTo(form, validData, PropertyOccupancy(false, Some(LocalDate.of(2017, 4, 23))))
  }

  it should "require last occupied date if the occupation/ownership" in {
    val data = validData
      .updated("interestedOnOrBefore", "true")
      .updated("lastOccupiedDate.day", "28")
      .updated("lastOccupiedDate.month", "2")
      .updated("lastOccupiedDate.year", "2017")
    verifyError(form, data, "lastOccupiedDate", errorLastDateAfterStartDate)
  }

  it should "require last occupied if the occupation/ownership has ended" in {
    val data = validData.updated("stillOccupied", "false")
    verifyMandatoryDate(form, data, "lastOccupiedDate")
  }

  it should "not require last occupied date if the occupation/ownership has not ended" in {
    val data = validData.updated("stillOccupied", "true")
    verifyOptionalDate(form, data, "lastOccupiedDate")
  }

  it should "require the last occupied date to be after the start date" in {
    val data = validData
      .updated("stillOccupied", "false")
      .updated("lastOccupiedDate.day", "1")
      .updated("lastOccupiedDate.month", "3")
      .updated("lastOccupiedDate.year", "2017")
    verifyOnlyError(form, data, "lastOccupiedDate", errorLastDateAfterStartDate)
  }

  object TestData {
    val form =
      ClaimPropertyOccupancy.occupancyForm(earliestEnglishStartDate)
    val validData = Map(
      "stillOccupied"          -> "false",
      "lastOccupiedDate.day"   -> "23",
      "lastOccupiedDate.month" -> "4",
      "lastOccupiedDate.year"  -> "2017"
    )
  }

}
