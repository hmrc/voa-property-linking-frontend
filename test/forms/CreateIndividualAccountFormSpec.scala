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

import org.joda.time.LocalDate
import controllers.CreateIndividualAccount._
import models.{Address, PersonalDetails}
import org.scalatest.{FlatSpec, MustMatchers}
import uk.gov.hmrc.domain.Nino
import utils.FormBindingVerification._
import views.helpers.Errors

class CreateIndividualAccountFormSpec extends FlatSpec with MustMatchers {

  val validData = Map(
    keys.firstName -> "fname",
    keys.lastName -> "lname",
    keys.email -> "email@address.com",
    keys.confirmedEmail -> "email@address.com",
    keys.phone1 -> "01234567890",
    keys.phone2 -> "",
    keys.nino -> "AA123456B",
    "address.line1" -> "123 The Road",
    "address.postcode" -> "AA11 1AA",
    "dob.day" -> "12",
    "dob.month" -> "1",
    "dob.year" -> "1950"
  )

  "Individual account form" must "require a first name" in {
    verifyMandatory(form, validData, keys.firstName)
  }

  it must "require the first name to be no more than 100 characters" in {
    verifyCharacterLimit(form, validData, keys.firstName, 100)
  }

  it must "require the last name" in {
    verifyMandatory(form, validData, keys.lastName)
  }

  it must "require the last name to be no more than 100 characters" in {
    verifyCharacterLimit(form, validData, keys.lastName, 100)
  }

  it must "require an email address" in {
    verifyMandatory(form, validData, keys.email)
  }

  it must "require the email address to be no more than 150 characters" in {
    val `150chars` = (1 to 138 map { _ => "a" } mkString) + "@example.com"

    verifyNoErrors(form, validData.updated(keys.email, `150chars`).updated(keys.confirmedEmail, `150chars`))

    verifyError(form, validData.updated(keys.email, "a" + `150chars`).updated(keys.confirmedEmail, `150chars`), keys.email, "error.maxLength", Seq(150))
  }

  it must "require the email address to be valid" in {
    Seq("aa.aa", "aaaa", "aa@@aa", "") foreach { invalid =>
      verifyError(form, validData.updated(keys.email, invalid), keys.email, "error.email")
    }
  }

  it must "require the confirmed email to match" in {
    verifyError(form, validData.updated(keys.email, "email1@address.com").updated(keys.confirmedEmail, "email2@address.com"), keys.confirmedEmail, Errors.emailsMustMatch)
  }

  it must "require a phone number" in {
    verifyMandatory(form, validData, keys.phone1)
  }

  it must "not require an alternative phone number" in {
    verifyOptional(form, validData, keys.phone2)
  }

  it must "require a valid national insurance number" in {
    Seq("ABCD1234E", "QQ123456A", "GB998877B", "123456789", "National insurance number", "") foreach { invalid =>
      verifyError(form, validData.updated(keys.nino, invalid), keys.nino, "error.nino.invalid")
    }
  }

  it must "require a valid date in the past" in {
    verifyMandatoryDate(form, validData, "dob")
  }

  it must "require a postcode" in {
    verifyMandatory(form, validData, "address.postcode")
  }

  it must "require the first line of the address to be non-empty" in {
    verifyMandatory(form, validData, "address.line1")
  }

  it must "reject postcode longer than 8 chars" in {
    verifyCharacterLimit(form, validData, "address.postcode", 8)
  }

  it must "accept national insurance numbers with lower case characters" in {
    val withLowerCaseNino = validData.updated(keys.nino, "ab123456b")
    mustBind(form, withLowerCaseNino)
  }

  it must "strip spaces from national insurance numbers" in {
    val ninoWithSpaces = validData.updated(keys.nino, "AB 12 34 56 C")
    mustBindTo(form, ninoWithSpaces,
      PersonalDetails(
        "fname", "lname", new LocalDate(1950, 1, 12), Nino("AB123456C"),
        "email@address.com", "email@address.com", "01234567890",
        None, Address(None, "123 The Road", "", "", "", "AA11 1AA")
      )
    )
  }

  val validData2AddressLines = validData ++ Map("address.line2" -> "Test")
  val validData3AddressLines = validData2AddressLines ++ Map("address.line3" -> "Test")
  val validData4AddressLines = validData3AddressLines ++ Map("address.line4" -> "Test")

  it must "not allow more than 80 characters in line1" in {
    verifyCharacterLimit(form, validData, "address.line1", 80)
  }

  it must "not allow more than 30 characters in line2 if 2 lines are populated" in {
    verifyCharacterLimit(form, validData2AddressLines, "address.line2", 30)
  }

  it must "not allow more than 80 characters in line2 if 3 lines are populated" in {
    verifyCharacterLimit(form, validData3AddressLines, "address.line2", 80)
  }

  it must "not allow more than 30 characters in line3 if 3 lines are populated" in {
    verifyCharacterLimit(form, validData3AddressLines, "address.line3", 30)
  }

  it must "not allow more than 80 characters in line2 if 4 lines are populated" in {
    verifyCharacterLimit(form, validData4AddressLines, "address.line2", 80)
  }

  it must "not allow more than 35 characters in line3 if 4 lines are populated" in {
    verifyCharacterLimit(form, validData4AddressLines, "address.line3", 35)
  }

  it must "not allow more than 30 characters in line4 if 4 lines are populated" in {
    verifyCharacterLimit(form, validData4AddressLines, "address.line4", 30)
  }
}
