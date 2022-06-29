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

package utils

import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.prop.Tables.Table
import play.api.data.Form
import play.api.data.Forms.single
import tests.BaseUnitSpec
import utils.PhoneNumberValidation.validatePhoneNumber

class PhoneNumberValidationSpec extends BaseUnitSpec {

  trait Setup {
    val form = Form(single("phone" -> validatePhoneNumber))
  }

  "phone number validation" should {

    "catch invalid length error" in new Setup {
      val lengths = Table(
        ("phone number", "validity"),
        ("1", false),
        ("12", false),
        ("1234567890", false),
        ("012345678901234567890", false), // 21
        ("0123456789012345678901", false), // 22
        ("01234567890", true), // 11
        ("012345678901", true),
        ("+44 1234567890", true),
        ("+44 0808 157 0192", true),
        ("+447904567897", true),
        ("0790 305 1234", true),
        ("0790-678-8992", true),
        ("+44 790-590-5876", true)
      )

      TableDrivenPropertyChecks.forAll(lengths) { (phone, isValid) =>
        val res: Form[String] = form.bind(Map("phone" -> phone))

        if (isValid) {
          res.hasErrors shouldBe false
        } else {
          res.errors(0).message shouldBe "error.phoneNumber.invalidLength"
        }
      }
    }

    "catch invalid format error" in new Setup {
      val formats = Table(
        ("phone number", "validity"),
        ("phonenumber123456", false),
        ("phone number 1234", false),
        ("00447904098765", false),
        ("0044 4567890984", false),
        ("44 7904089899", false),
        ("0044 456 709 7653", false),
        ("+44 0567097653", false),
        ("9790 305 1234", false),
        ("0890456782a", false),
        ("+44-909-789-8973", false),
        ("#447903067897", false),
      )

      TableDrivenPropertyChecks.forAll(formats) { (phone, isValid) =>
        val res: Form[String] = form.bind(Map("phone" -> phone))
        res.errors(0).message shouldBe "error.phoneNumber.invalidFormat"
      }
    }

    "catch mandatory condition" in new Setup {
      val isNumber = Table(
        ("phone number", "validity"),
        ("01234567890", true),
        ("", false)
      )

      TableDrivenPropertyChecks.forAll(isNumber) { (phone, isValid) =>
        val res: Form[String] = form.bind(Map("phone" -> phone))

        if (isValid) {
          res.hasErrors shouldBe false
        } else {
          res.errors(0).message shouldBe "error.phoneNumber.required"
        }
      }
    }
  }

}
