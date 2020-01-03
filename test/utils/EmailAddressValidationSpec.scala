/*
 * Copyright 2020 HM Revenue & Customs
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

import org.scalatest.prop.PropertyChecks
import org.scalatest.{Matchers, WordSpec}

class EmailAddressValidationSpec extends WordSpec with Matchers with PropertyChecks {

  "Testing EmailAddress" should {

    "invalid email should return false" in {
      EmailAddressValidation.isValid("sausages") shouldBe false
      EmailAddressValidation.isValid("sausages@sausages") shouldBe false
      EmailAddressValidation.isValid("sausages@sausages.") shouldBe false
      EmailAddressValidation.isValid("@sausages.") shouldBe false
      EmailAddressValidation.isValid("@sausages.com") shouldBe false
      EmailAddressValidation.isValid("sausages.com") shouldBe false
      EmailAddressValidation.isValid("sausages.com") shouldBe false
    }

    "valid email should return false" in {
      EmailAddressValidation.isValid("sausages@gmail.com") shouldBe true
    }
  }
}
