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

import controllers.VoaPropertyLinkingSpec
import controllers.propertyLinking.ClaimPropertyOwnership
import play.api.i18n.Lang.defaultLang
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.{ErrorLink, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent

import java.time.LocalDate

class FrontendComponentHelperSpec extends VoaPropertyLinkingSpec {

  "The form with multiple date validation errors" should "return only one common error 'error.common.invalid.date' " in {
    val form = ClaimPropertyOwnership.ownershipForm(endDate = None)
    val inValidData = Map(
      "interestedStartDate.day"   -> "40",
      "interestedStartDate.month" -> "20",
      "interestedStartDate.year"  -> "999"
    )

    //Test errors size before merge and format date errors
    form.bind(inValidData).errors.size shouldBe 3

    //Test after merge and format date errors into only one common date error
    val formattedErrors = FrontendComponentHelper.formatErrorMessages(form.bind(inValidData), "interestedStartDate")(
      messagesApi.preferred(Seq(defaultLang)))

    formattedErrors.size shouldBe 1
    formattedErrors should contain(
      ErrorLink(
        href = Some("#interestedStartDate-day"),
        content = Text(s"On what date did you become the owner or occupier? - Enter a valid date")))
  }

  it should "be able to handle non-generic date errors while removing duplicates" in {
    implicit val messages: Messages = messagesApi.preferred(Seq(defaultLang))
    val form = ClaimPropertyOwnership
      .ownershipForm(endDate = Some(LocalDate.of(2017, 4, 2)))
    val invalidData = Map(
      "interestedStartDate.day"   -> "2",
      "interestedStartDate.month" -> "4",
      "interestedStartDate.year"  -> "2017"
    )

    val formattedErrors =
      FrontendComponentHelper
        .formatErrorMessages(form.bind(invalidData), "interestedStartDate", manualDateErrorHandler = {
          case e @ "interestedStartDate.error.startDateMustBeBeforeEnd" => e.replace(".", "?")
        })

    formattedErrors.size shouldBe 1
    formattedErrors should contain(
      ErrorLink(
        href = Some("#interestedStartDate-day"),
        content = Text("interestedStartDate?error?startDateMustBeBeforeEnd")
      ))
  }

  "valueWithId" should "return value wrapped in a span" in {
    FrontendComponentHelper.valueWithId("this is a value", "value-id") shouldBe HtmlContent(
      """<span id="value-id">this is a value</span>"""
    )
  }

  "summaryListMultiValues" should "return value wrapped in a paragraph with values" in {
    FrontendComponentHelper.summaryListMultiValues(id = "id", values = List("value-1", "value-2")) shouldBe HtmlContent(
      s"""<p class="govuk-body" id="id">value-1<br>value-2</p>""")
  }
}
