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

import controllers.VoaPropertyLinkingSpec
import controllers.propertyLinking.ClaimPropertyOwnership
import play.api.i18n.Lang.defaultLang
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.{ErrorLink, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent

import java.time.LocalDate

class FrontendComponentHelperSpec extends VoaPropertyLinkingSpec {

  "The form with multiple date validation errors" should "return only one common error 'error.common.invalid.date' " in {
    val form = ClaimPropertyOwnership.ownershipForm(earliestEnglishStartDate, endDate = None)
    val inValidData = Map(
      "interestedOnOrBefore" -> "false",
      "fromDate.day"         -> "40",
      "fromDate.month"       -> "20",
      "fromDate.year"        -> "999"
    )

    //Test errors size before merge and format date errors
    form.bind(inValidData).errors.size shouldBe 3

    //Test after merge and format date errors into only one common date error
    val formattedErrors = FrontendComponentHelper.formatErrorMessages(form.bind(inValidData), "fromDate")(
      messagesApi.preferred(Seq(defaultLang)))

    formattedErrors.size shouldBe 1
    formattedErrors should contain(
      ErrorLink(
        href = Some("#fromDate-day"),
        content = Text(s"On what date did you become the owner or occupier? - Enter a valid date")))
  }

  it should "be able to handle non-generic date errors while removing duplicates" in {
    implicit val messages: Messages = messagesApi.preferred(Seq(defaultLang))
    val form = ClaimPropertyOwnership
      .ownershipForm(earliestEnglishStartDate, endDate = Some(LocalDate.of(2017, 4, 2)))
    val invalidData = Map(
      "interestedOnOrBefore" -> "false",
      "fromDate.day"         -> "2",
      "fromDate.month"       -> "4",
      "fromDate.year"        -> "2017"
    )

    val formattedErrors =
      FrontendComponentHelper.formatErrorMessages(form.bind(invalidData), "fromDate", manualDateErrorHandler = {
        case e @ "interestedOnOrBefore.error.startDateMustBeBeforeEnd" => e.replace(".", "?")
      })

    formattedErrors.size shouldBe 1
    formattedErrors should contain(
      ErrorLink(
        href = Some("#fromDate-day"),
        content = Text("interestedOnOrBefore?error?startDateMustBeBeforeEnd")
      ))
  }

  "valueWithId" should "return value wrapped in a span" in {
    FrontendComponentHelper.valueWithId("this is a value", "value-id") shouldBe HtmlContent(
      """<span id="value-id">this is a value</span>"""
    )
  }
}
