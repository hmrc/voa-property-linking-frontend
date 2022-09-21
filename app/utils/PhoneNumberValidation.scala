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

import play.api.data.Forms.text

object PhoneNumberValidation {

  // Valid formats: 1234567890 | +44 1234567890 | +44 0123 456 789 | +441234567890 | 0123 456 7890 | 0123-456-7890 | +44 123-456-7890

  val phoneNumberRegex =
    "((\\+44\\s?\\(0\\)\\s?\\d{2,4})|(\\+44\\s?(01|02|03|07|08)\\d{2,3})|(\\+44\\s?(1|2|3|7|8)\\d{2,3})|(\\(\\+44\\)\\s?\\d{3,4})|(\\(\\d{5}\\))|((01|02|03|07|08)\\d{2,3})|(\\d{5}))(\\s|-|.)(((\\d{3,4})(\\s|-)(\\d{3,4}))|((\\d{6,7})))"

  def validatePhoneNumber = {

    def validPhoneNumberLength(num: String) = num.length >= 11 && num.length <= 20

    text
      .verifying("error.phoneNumber.required", num => num.nonEmpty)
      .verifying("error.phoneNumber.invalidLength", num => if (num.nonEmpty) validPhoneNumberLength(num) else true)
      .verifying(
        "error.phoneNumber.invalidFormat",
        num => if (num.nonEmpty && validPhoneNumberLength(num)) num.matches(phoneNumberRegex) else true)
  }
}
