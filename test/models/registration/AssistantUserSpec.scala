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

import play.api.data.Form
import tests.BaseUnitSpec
import utils.FormBindingVerification.{shouldBindTo, verifyNonEmptyText}

class AssistantUserSpec extends BaseUnitSpec {

  "Assistant user account details form" should {
    import AssistantUserAccountDetailsTestData._

    "bind when the inputs are all valid" in {
      shouldBindTo(form, validData, expected)
    }
    "ensure first name is mandatory" in {
      verifyNonEmptyText(form, validData, keys.firstName)
    }
    "ensure last name is mandatory" in {
      verifyNonEmptyText(form, validData, keys.lastName)
    }
  }

  object AssistantUserAccountDetailsTestData {
    val form: Form[AssistantUserAccountDetails] = AssistantUser.assistant
    val validData = Map(
      keys.firstName -> firstName,
      keys.lastName  -> lastName
    )
    val expected = AssistantUserAccountDetails(
      firstName = firstName,
      lastName = lastName
    )
  }
}
