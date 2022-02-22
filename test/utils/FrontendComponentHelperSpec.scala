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
import uk.gov.hmrc.govukfrontend.views.Aliases.ErrorLink
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent

class FrontendComponentHelperSpec extends VoaPropertyLinkingSpec{

  "The form with multiple date validation errors" should "return only one common error 'error.common.invalid.date' " in {
    val form = ClaimPropertyOwnership.ownershipForm
    val inValidData = Map(
      "interestedBefore2017" -> "false",
      "fromDate.day"         -> "40",
      "fromDate.month"       -> "20",
      "fromDate.year"        -> "999"
    )

    //Test errors size before merge and format date errors
    form.bind(inValidData).errors.size shouldBe 3

    //Test after merge and format date errors into only one common date error
    val formattedErrors = FrontendComponentHelper.formatErrorMessages(form.bind(inValidData), "fromDate")(messagesApi.preferred(Seq(defaultLang)))
    formattedErrors.size shouldBe 1
    formattedErrors.contains(ErrorLink(href = Some("#fromDate"), content = HtmlContent(s"On what date did you become the owner or occupier? - Enter a valid date"))) shouldBe true

  }
}
