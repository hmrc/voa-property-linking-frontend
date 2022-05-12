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

import play.api.data.{Form, FormError}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.ErrorLink
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent

object FrontendComponentHelper {

  def formatErrorMessages(form: Form[_], fieldName: String, messagesKeySuffix: String = "")(
        implicit messages: Messages): Seq[ErrorLink] = {

    def isDateFieldErrorsExists(error: FormError, fieldName: String) =
      Seq(fieldName, s"$fieldName.day", s"$fieldName.month", s"$fieldName.year").contains(error.key)

    //Merge date mapper individual filed errors(date, month and year) into one common date error to avoid duplicate messages
    form.errors
      .map { error =>
        {
          if (isDateFieldErrorsExists(error, fieldName))
            FormError(s"$fieldName", Seq("error.common.invalid.date"))
          else
            error
        }
      }
      .toSet
      .map { error: FormError =>
        ErrorLink(
          href = if (isDateFieldErrorsExists(error, fieldName)) Some(s"#${error.key}-day") else Some(s"#${error.key}"),
          content = HtmlContent(
            s"${messages(s"label.${error.key}$messagesKeySuffix")} - ${messages(error.message, error.args.map(_.toString): _*)}")
        )
      }
  }.toSeq

  def valueWithId(value: String, id: String): HtmlContent =
    HtmlContent(s"""<span id="$id">$value</span>""")
}
