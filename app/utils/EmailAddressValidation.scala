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

package utils

import play.api.data.Forms.text
import play.api.data.validation.{Constraint, Invalid, Valid}

object EmailAddressValidation {
  final private val validEmail = """^([a-zA-Z0-9.!#$%&’'*+/=?^_`{|}~-]+)@([a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)+)$""".r

  def isValid(email: String) =
    email match {
      case validEmail(_, _) => true
      case invalidEmail     => false
    }

  def isValidEmail =
    text.verifying(Constraint[String]("email") { txt =>
      if (txt.trim.length > 150)
        Invalid("error.emailLength")
      else if (txt.trim.isEmpty || !EmailAddressValidation.isValid(txt))
        Invalid("error.invalidEmail")
      else
        Valid
    })
}
