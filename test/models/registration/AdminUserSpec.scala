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

package models.registration

import java.time.LocalDate

import play.api.data.Form
import tests.BaseUnitSpec
import utils.FormBindingVerification._
import views.helpers.Errors

class AdminUserSpec extends BaseUnitSpec {

  "Admin organisation user account details form" should {
    import AdminOrganisationAccountDetailsTestData._

    "bind when the inputs are all valid" in {
      shouldBindTo(form, validData, expected)
    }
    "ensure first name is mandatory" in {
      verifyNonEmptyText(form, validData, keys.firstName)
    }
    "ensure last name is mandatory" in {
      verifyNonEmptyText(form, validData, keys.lastName)
    }
    "ensure company name" when {
      "is mandatory" in {
        verifyNonEmptyText(form, validData, keys.companyName)
      }
      "has maximum length of 45" in {
        verifyCharacterLimit(form, validData, keys.companyName, 45)
      }
    }
    "ensure address validation" in {
      verifyMandatoryAddress(form, validData)
    }
    "ensure date of birth" should {
      "is mandatory" in {
        verifyMandatoryDate(form, validData, keys.dateOfBirth)
      }
      "is valid" in {
        verifyddmmyy(form, validData, keys.dateOfBirth, LocalDate.now.getYear)
      }
      "is in the past" in {
        verifyDateIsBefore(form, validData, keys.dateOfBirth, LocalDate.now.plusDays(2))
      }
    }
    "ensure nino" should {
      "is mandatory" in {
        verifyMandatory(form, validData, keys.nino)
      }
      "is valid" in {
        verifyValidNino(form, validData)
      }
    }
    "ensure phone" when {
      "is valid with +44" in {
        verifyMandatory(form, validData.updated(keys.phone, "+44 (0)7591 231 233"), keys.phone)
      }
      "is mandatory" in {
        verifyMandatory(form, validData, keys.phone)
      }
      "does not accept invalid length" in {
        val invalid = Seq("0134567", "0123456789", "012345678901234567890123456789")
        invalid.foreach { phone =>
          val data = validData.updated(keys.phone, phone)
          verifyError(form, data, keys.phone, "error.phoneNumber.invalidLength")
        }
      }
      "does not accept invalid format" in {
        val invalid = Seq("+12345788875", "+01235 456 789", "845 8888 9999")
        invalid.foreach { phone =>
          val data = validData.updated(keys.phone, phone)
          verifyError(form, data, keys.phone, "error.phoneNumber.invalidFormat")
        }
      }
    }

    "ensure email address" when {
      "is mandatory" in {
        verifyMandatory(form, validData, keys.email)
      }
      "binds when email address is valid" in {
        val valid = Seq("someone@example.com", "123.456@123.com", "abc.def@ghi.jkl.mno")
        valid.foreach { email =>
          val data = validData.updated(keys.email, email).updated(keys.confirmedBusinessEmail, email)
          shouldBind(form, data)
        }
      }
      "disallows invalid email addresses" in {
        val invalid = Seq("someone@example", "@example.com", "abc.def*abc.com", "abcdef")
        invalid.foreach { email =>
          val data = validData.updated(keys.email, email).updated(keys.confirmedBusinessEmail, email)
          verifyError(form, data, keys.email, Errors.invalidEmail)
        }
      }
      "confirmed email address and email must match" in {
        val data =
          validData.updated(keys.email, "some@example.com").updated(keys.confirmedBusinessEmail, "other@domain.com")
        verifyError(form, data, keys.confirmedBusinessEmail, Errors.emailsMustMatch)
      }
    }
    "ensure 'Is Agent'" should {
      "is 'true' or 'false'" in {
        verifyBoolean(form, validData, keys.isAgent)
      }
    }
    "optionally accepts selectedAddress field" in {
      verifyOptional(form, validData, keys.selectedAddress)
    }
  }

  "Individual user account details form" should {
    import IndividualUserAccountDetailsTestData._

    "bind when the inputs are all valid" in {
      shouldBindTo(form, validData, expected)
    }
    "ensure first name is mandatory" in {
      verifyNonEmptyText(form, validData, keys.firstName)
    }
    "ensure last name is mandatory" in {
      verifyNonEmptyText(form, validData, keys.lastName)
    }
    "ensure address validation" in {
      verifyMandatoryAddress(form, validData)
    }
    "ensure date of birth" should {
      "is mandatory" in {
        verifyMandatoryDate(form, validData, keys.dateOfBirth)
      }
      "is valid" in {
        verifyddmmyy(form, validData, keys.dateOfBirth, LocalDate.now.getYear)
      }
      "is in the past" in {
        verifyDateIsBefore(form, validData, keys.dateOfBirth, LocalDate.now.plusDays(2))
      }
    }
    "ensure nino" should {
      "is mandatory" in {
        verifyMandatory(form, validData, keys.nino)
      }
      "is valid" in {
        verifyValidNino(form, validData)
      }
    }
    "ensure phone" when {
      "is mandatory" in {
        verifyMandatory(form, validData, keys.phone)
      }
      "is valid with +44" in {
        verifyMandatory(form, validData.updated(keys.phone, "+44 07591 231 233"), keys.phone)
      }
      "does not accept invalid length" in {
        val invalid = Seq("0134567", "0123456789", "012345678901234567890123456789")
        invalid.foreach { phone =>
          val data = validData.updated(keys.phone, phone)
          verifyError(form, data, keys.phone, "error.phoneNumber.invalidLength")
        }
      }
    }
    "ensure mobile phone" when {
      "is mandatory" in {
        verifyMandatory(form, validData, keys.mobilePhone)
      }
      "does not accept invalid length" in {
        val invalid = Seq("0134567", "0123456789", "012345678901234567890123456789")
        invalid.foreach { phone =>
          val data = validData.updated(keys.phone, phone)
          verifyError(form, data, keys.phone, "error.phoneNumber.invalidLength")
        }
      }
    }
    "ensure email address" when {
      "is mandatory" in {
        verifyMandatory(form, validData, keys.email)
      }
      "binds when email address is valid" in {
        val valid = Seq("someone@example.com", "123.456@123.com", "abc.def@ghi.jkl.mno")
        valid.foreach { email =>
          val data = validData.updated(keys.email, email).updated(keys.confirmedEmail, email)
          shouldBind(form, data)
        }
      }
      "disallows invalid email addresses" in {
        val invalid = Seq("someone@example", "@example.com", "abc.def*abc.com", "abcdef")
        invalid.foreach { email =>
          val data = validData.updated(keys.email, email).updated(keys.confirmedEmail, email)
          verifyError(form, data, keys.email, Errors.invalidEmail)
        }
      }
      "confirmed email address and email must match" in {
        val data = validData.updated(keys.email, "some@example.com").updated(keys.confirmedEmail, "other@domain.com")
        verifyError(form, data, keys.confirmedEmail, Errors.emailsMustMatch)
      }
    }
    "ensure trading name" when {
      "has maximum length of 45" in {
        verifyCharacterLimit(form, validData, keys.tradingName, 45)
      }
    }
    "optionally accepts selectedAddress field" in {
      verifyOptional(form, validData, keys.selectedAddress)
    }
  }

  "Admin in existing organisation user account details form" should {
    import AdminInExistingOrganisationUserTestData._

    "bind when the inputs are all valid" in {
      shouldBindTo(form, validData, expected)
    }
    "ensure first name is mandatory" in {
      verifyNonEmptyText(form, validData, keys.firstName)
    }
    "ensure last name is mandatory" in {
      verifyNonEmptyText(form, validData, keys.lastName)
    }
    "ensure date of birth" should {
      "is mandatory" in {
        verifyMandatoryDate(form, validData, keys.dateOfBirth)
      }
      "is valid" in {
        verifyddmmyy(form, validData, keys.dateOfBirth, LocalDate.now.getYear)
      }
      "is in the past" in {
        verifyDateIsBefore(form, validData, keys.dateOfBirth, LocalDate.now.plusDays(2))
      }
    }
    "ensure nino" should {
      "is mandatory" in {
        verifyMandatory(form, validData, keys.nino)
      }
      "is valid" in {
        verifyValidNino(form, validData)
      }
    }
  }

  object AdminOrganisationAccountDetailsTestData {
    val form: Form[AdminOrganisationAccountDetails] = AdminUser.organisation
    val validData = Map(
      keys.firstName               -> firstName,
      keys.lastName                -> lastName,
      keys.companyName             -> companyName,
      s"${keys.address}.line1"     -> address.line1,
      s"${keys.address}.line2"     -> address.line2,
      s"${keys.address}.line3"     -> address.line3,
      s"${keys.address}.line4"     -> address.line4,
      s"${keys.address}.postcode"  -> address.postcode,
      s"${keys.dateOfBirth}.day"   -> dateOfBirth.getDayOfMonth.toString,
      s"${keys.dateOfBirth}.month" -> dateOfBirth.getMonthValue.toString,
      s"${keys.dateOfBirth}.year"  -> dateOfBirth.getYear.toString,
      keys.phone                   -> phone,
      keys.nino                    -> nino.toString,
      keys.email                   -> email,
      keys.confirmedBusinessEmail  -> email,
      keys.isAgent                 -> "false"
    )
    val expected = AdminOrganisationAccountDetails(
      firstName = firstName,
      lastName = lastName,
      companyName = companyName,
      address = address.copy(addressUnitId = None),
      dob = dateOfBirth,
      nino = nino,
      phone = phone,
      email = email,
      confirmedEmail = email,
      isAgent = false,
      selectedAddress = None
    )
  }

  object IndividualUserAccountDetailsTestData {
    val form: Form[IndividualUserAccountDetails] = AdminUser.individual
    val validData = Map(
      keys.firstName               -> firstName,
      keys.lastName                -> lastName,
      s"${keys.address}.line1"     -> address.line1,
      s"${keys.address}.line2"     -> address.line2,
      s"${keys.address}.line3"     -> address.line3,
      s"${keys.address}.line4"     -> address.line4,
      s"${keys.address}.postcode"  -> address.postcode,
      s"${keys.dateOfBirth}.day"   -> dateOfBirth.getDayOfMonth.toString,
      s"${keys.dateOfBirth}.month" -> dateOfBirth.getMonthValue.toString,
      s"${keys.dateOfBirth}.year"  -> dateOfBirth.getYear.toString,
      keys.phone                   -> phone,
      keys.mobilePhone             -> phone,
      keys.nino                    -> nino.toString,
      keys.email                   -> email,
      keys.confirmedEmail          -> email,
      keys.tradingName             -> companyName
    )
    val expected = IndividualUserAccountDetails(
      firstName = firstName,
      lastName = lastName,
      address = address.copy(addressUnitId = None),
      dob = dateOfBirth,
      nino = nino,
      phone = phone,
      mobilePhone = phone,
      email = email,
      confirmedEmail = email,
      tradingName = Some(companyName),
      selectedAddress = None
    )
  }

  object AdminInExistingOrganisationUserTestData {
    val form: Form[AdminInExistingOrganisationAccountDetails] = AdminInExistingOrganisationUser.organisation
    val validData = Map(
      keys.firstName               -> firstName,
      keys.lastName                -> lastName,
      s"${keys.dateOfBirth}.day"   -> dateOfBirth.getDayOfMonth.toString,
      s"${keys.dateOfBirth}.month" -> dateOfBirth.getMonthValue.toString,
      s"${keys.dateOfBirth}.year"  -> dateOfBirth.getYear.toString,
      keys.nino                    -> nino.toString
    )
    val expected = AdminInExistingOrganisationAccountDetails(
      firstName = firstName,
      lastName = lastName,
      dob = dateOfBirth,
      nino = nino
    )
  }
}
