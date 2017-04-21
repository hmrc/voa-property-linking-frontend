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

import config.ApplicationConfig
import controllers.ClaimProperty
import models.{CapacityDeclaration, CapacityType, Occupier}
import org.joda.time.LocalDate
import org.scalatest.{FlatSpec, MustMatchers}
import utils.FormBindingVerification._
import views.helpers.Errors

class CapacityDeclarationFormSpec extends FlatSpec with MustMatchers {

  import TestData._

  /*
  Important note on testing this form binding - for the purposes of testing the 1st April Date is overridden in the
  application configuration parameter 'propertyLinkingDateThreshold' to be 1st MARCH 2017 and all the test cases used accordingly
  however for clarity the test descriptions remain 1st April so that it doesn't get confusint as to the desired behavior.

  After 1st April this could be reversed, but then the test cases should also be changed
   */
  behavior of "Capacity declaration form"

  it should "bind when the inputs are all valid" in {
    mustBindTo(form, validData, CapacityDeclaration(Occupier, false, Some(new LocalDate(2017, 3, 20)), false, Some(new LocalDate(2017, 3, 23))))
  }

  it should "mandate a capacity" in {
    verifyMandatoryMultiChoice(form, validData, "capacity")
  }

  it should "only allow recognised capacity values" in {
    verifyMultiChoice(form, validData, "capacity", CapacityType)
  }

  it should "require a start date if the occupation/ownership started after 1st April 2017" in {
    val data = validData.updated("interestedBefore2017", "false") - "fromDate.day" - "fromDate.month" - "fromDate.year"
    verifyMandatoryDate(form, data, "fromDate", false)
    verifyError(form, data, "toDate", Errors.dateMustBeAfterOtherDate)
  }

  it should "ignore a start date and require an end date after 1st April 2017 if the occupation/ownership started before 1st April 2017" in {
    val data = validData.updated("interestedBefore2017", "true")
      .updated("toDate.day", "28")
      .updated("toDate.month", "2")
      .updated("toDate.year", "2017")
    verifyError(form, data, "toDate", Errors.dateMustBeAfter1stApril2017)
  }

  it should "ignore a start date if the occupation/ownership started before 1st April 2017" in {
    val data = validData
      .updated("interestedBefore2017", "true")
      .updated("toDate.day", "19")
      .updated("toDate.month", "3")
      .updated("toDate.year", "2017")
    mustBindTo(form, data, CapacityDeclaration(Occupier, true, None, false, Some(new LocalDate(2017, 3, 19))))
  }

  it should "require end date after 1st April 2017 if the occupation/ownership started before 1st April 2017" in {
    val data = validData
      .updated("interestedBefore2017", "true")
      .updated("toDate.day", "28")
      .updated("toDate.month", "2")
      .updated("toDate.year", "2017")
    verifyError(form, data, "toDate", Errors.dateMustBeAfter1stApril2017)
  }


  it should "not require the start date if the occupation/ownership started before 1st April 2017" in {
    val data = validData.updated("interestedBefore2017", "true")
    verifyOptionalDate(form, data, "fromDate")

  }

  it should s"require the start date to be after ${ApplicationConfig.propertyLinkingDateThreshold}" in {
    val data = validData
      .updated("interestedBefore2017", "false")
      .updated("fromDate.day", "1")
      .updated("fromDate.month", "3")
      .updated("fromDate.year", "2017")
    verifyOnlyError(form, data, "fromDate", Errors.dateMustBeAfter1stApril2017)
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
      .updated("fromDate.day", "10")
      .updated("fromDate.month", "3")
      .updated("fromDate.year", "2017")
      .updated("stillInterested", "false")
      .updated("toDate.day", "3")
      .updated("toDate.month", "3")
      .updated("toDate.year", "2017")
    verifyOnlyError(form, data, "toDate", Errors.dateMustBeAfterOtherDate)
  }

  it should "require the end date to be after 1st April if start before 1st April but start date still set" in {
    val data = validData
      .updated("interestedBefore2017", "true")
      .updated("fromDate.day", "10")
      .updated("fromDate.month", "3")
      .updated("fromDate.year", "2017")
      .updated("stillInterested", "false")
      .updated("toDate.day", "1")
      .updated("toDate.month", "3")
      .updated("toDate.year", "2017")
    verifyOnlyError(form, data, "toDate", Errors.dateMustBeAfter1stApril2017)
  }


  object TestData {
    val form = ClaimProperty.declareCapacityForm
    val validData = Map(
      "capacity" -> Occupier.name,
      "interestedBefore2017" -> "false",
      "fromDate.day" -> "20",
      "fromDate.month" -> "3",
      "fromDate.year" -> "2017",
      "stillInterested" -> "false",
      "toDate.day" -> "23",
      "toDate.month" -> "3",
      "toDate.year" -> "2017"
    )
  }

}