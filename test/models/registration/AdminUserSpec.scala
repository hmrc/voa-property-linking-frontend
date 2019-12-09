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

package models.registration

import java.time.LocalDate

import org.scalatest.{Matchers, WordSpec}
import play.api.data.Form
import utils.FakeObjects
import utils.FormBindingVerification._
import views.helpers.Errors

class AdminUserSpec extends WordSpec with Matchers with FakeObjects {

  "Admin organisation user account details form" should {
    import AdminOrganisationAccountDetailsTestData._

    "bind when the inputs are all valid" in {
      mustBindTo(form, validData, expected)
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
        val futureDate = LocalDate.now.plusDays(2)
        val data = validData
          .updated(s"${keys.dateOfBirth}.day", futureDate.getDayOfMonth.toString)
          .updated(s"${keys.dateOfBirth}.month", futureDate.getMonthValue.toString)
          .updated(s"${keys.dateOfBirth}.year", futureDate.getYear.toString)
        verifyError(form, data, keys.dateOfBirth, Errors.dateMustBeInPast)
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
      "has maximum length of 15" in {
        verifyCharacterLimit(form, validData, keys.phone, 15)
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
          mustBind(form, data)
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
        val data = validData.updated(keys.email, "some@example.com").updated(keys.confirmedBusinessEmail, "other@domain.com")
        verifyError(form, data, keys.confirmedBusinessEmail, Errors.emailsMustMatch)
      }
    }
    "ensure 'Is Agent'" should {
      "is optional" in {
        verifyOptional(form, validData, keys.isAgent)
      }
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
      mustBindTo(form, validData, expected)
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
        val futureDate = LocalDate.now.plusDays(2)
        val data = validData
          .updated(s"${keys.dateOfBirth}.day", futureDate.getDayOfMonth.toString)
          .updated(s"${keys.dateOfBirth}.month", futureDate.getMonthValue.toString)
          .updated(s"${keys.dateOfBirth}.year", futureDate.getYear.toString)
        verifyError(form, data, keys.dateOfBirth, Errors.dateMustBeInPast)
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
      "has maximum length of 15" in {
        verifyCharacterLimit(form, validData, keys.phone, 15)
      }
    }
    "ensure mobile phone" when {
      "is mandatory" in {
        verifyMandatory(form, validData, keys.mobilePhone)
      }
      "has maximum length of 15" in {
        verifyCharacterLimit(form, validData, keys.mobilePhone, 15)
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
          mustBind(form, data)
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

  object AdminOrganisationAccountDetailsTestData {
    val form: Form[AdminOrganisationAccountDetails] = AdminUser.organisation
    val validData = Map(
      keys.firstName -> firstName,
      keys.lastName -> lastName,
      keys.companyName -> companyName,
      s"${keys.address}.line1" -> address.line1,
      s"${keys.address}.line2" -> address.line2,
      s"${keys.address}.line3" -> address.line3,
      s"${keys.address}.line4" -> address.line4,
      s"${keys.address}.postcode" -> address.postcode,
      s"${keys.dateOfBirth}.day" -> dateOfBirth.getDayOfMonth.toString,
      s"${keys.dateOfBirth}.month" -> dateOfBirth.getMonthValue.toString,
      s"${keys.dateOfBirth}.year" -> dateOfBirth.getYear.toString,
      keys.phone -> phone,
      keys.nino -> nino.toString,
      keys.email -> email,
      keys.confirmedBusinessEmail -> email,
      keys.isAgent -> "false"
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
      isAgent = Some(false),
      selectedAddress = None
    )
  }

  object IndividualUserAccountDetailsTestData {
    val form: Form[IndividualUserAccountDetails] = AdminUser.individual
    val validData = Map(
      keys.firstName -> firstName,
      keys.lastName -> lastName,
      s"${keys.address}.line1" -> address.line1,
      s"${keys.address}.line2" -> address.line2,
      s"${keys.address}.line3" -> address.line3,
      s"${keys.address}.line4" -> address.line4,
      s"${keys.address}.postcode" -> address.postcode,
      s"${keys.dateOfBirth}.day" -> dateOfBirth.getDayOfMonth.toString,
      s"${keys.dateOfBirth}.month" -> dateOfBirth.getMonthValue.toString,
      s"${keys.dateOfBirth}.year" -> dateOfBirth.getYear.toString,
      keys.phone -> phone,
      keys.mobilePhone -> phone,
      keys.nino -> nino.toString,
      keys.email -> email,
      keys.confirmedEmail -> email,
      keys.tradingName -> companyName
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

}

