/*
 * Copyright 2017 HM Revenue & Customs
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

import connectors.CapacityDeclaration
import controllers.ClaimProperty
import models.{CapacityType, Occupier}
import org.joda.time.LocalDate
import org.scalatest.{FlatSpec, MustMatchers}
import utils.FormBindingVerification._
import views.helpers.Errors

class CapacityDeclarationFormSpec extends FlatSpec with MustMatchers {
  import TestData._

  behavior of "Capacity declaration form"

  it should "bind when the inputs are all valid" in {
    mustBindTo(form, validData, CapacityDeclaration(Occupier, false, Some(new LocalDate(2017, 12, 24)), false, Some(new LocalDate(2018, 1, 13))))
  }

  it should "mandate a capacity" in {
    verifyMandatoryMultiChoice(form, validData, "capacity")
  }

  it should "only allow recognised capacity values" in {
    verifyMultiChoice(form, validData, "capacity", CapacityType)
  }

  it should "require a start date if the occupation/ownership started after 1st April 2017" in {
    val data = validData.updated("interestedBefore2017", "false")
    verifyMandatoryDate(form, data, "fromDate")
  }

  it should "not require the start date if the occupation/ownership started before 1st April 2017" in {
    val data = validData.updated("interestedBefore2017", "true")
    verifyOptionalDate(form, data, "fromDate")
  }

  it should "require the start date to be after 31st March 2017" in {
    val data = validData
      .updated("interestedBefore2017", "false")
      .updated("fromDate.day", "31")
      .updated("fromDate.month", "3")
      .updated("fromDate.year", "2017")
    verifyOnlyError(form, data, "fromDate", Errors.dateMustBeAfterMarch2017)
  }

  it should "require an end date if the occupation/ownership has ended" in {
    val data = validData.updated("stillInterested", "false")
    verifyMandatoryDate(form, data, "toDate")
  }

  it should "not require an end date if the occupation/ownership has not ended" in {
    val data = validData.updated("stillInterested", "true")
    verifyOptionalDate(form, data, "toDate")
  }

  it should "require the end date to be after the start date" in {
    val data = validData
      .updated("interestedBefore2017", "false")
      .updated("fromDate.day", "1")
      .updated("fromDate.month", "5")
      .updated("fromDate.year", "2017")
      .updated("stillInterested", "false")
      .updated("toDate.day", "1")
      .updated("toDate.month", "4")
      .updated("toDate.year", "2017")
    verifyOnlyError(form, data, "toDate", Errors.dateMustBeAfterOtherDate)
  }

  object TestData {
    val form = ClaimProperty.declareCapacityForm
    val validData = Map(
      "capacity" -> Occupier.name,
      "interestedBefore2017" -> "false",
      "fromDate.day" -> "24",
      "fromDate.month" -> "12",
      "fromDate.year" -> "2017",
      "stillInterested" -> "false",
      "toDate.day" -> "13",
      "toDate.month" -> "1",
      "toDate.year" -> "2018"
    )
  }
}
