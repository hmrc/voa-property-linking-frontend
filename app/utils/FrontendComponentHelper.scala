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

import play.api.data.{Form, FormError}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.ErrorLink
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, SummaryListRow, Value}

object FrontendComponentHelper {

  def formatErrorMessages(
        form: Form[_],
        fieldName: String,
        messagesKeySuffix: String = "",
        ownershipStatusSuffix: String = "",
        manualDateErrorHandler: PartialFunction[String, String] = PartialFunction.empty
  )(implicit messages: Messages): Seq[ErrorLink] = {

    def isDateFieldErrorsExists(error: FormError, fieldName: String) =
      Seq(fieldName, s"$fieldName.day", s"$fieldName.month", s"$fieldName.year").contains(error.key)

    // Merge date mapper individual filed errors(date, month and year) into one common date error to avoid duplicate messages
    form.errors
      .map { error =>
        (isDateFieldErrorsExists(error, fieldName), manualDateErrorHandler.isDefinedAt(error.message)) match {
          case (true, false) => FormError(fieldName, "error.common.invalid.date")
          case (true, true)  => FormError(fieldName, error.message)
          case _             => error
        }
      }
      .distinct
      .map { error: FormError =>
        ErrorLink(
          href = if (isDateFieldErrorsExists(error, fieldName)) Some(s"#${error.key}-day") else Some(s"#${error.key}"),
          content = Text(
            manualDateErrorHandler
              .applyOrElse(
                error.message,
                (_: String) =>
                  if (ownershipStatusSuffix.isEmpty)
                    s"${messages(s"label.${error.key}$messagesKeySuffix")} - ${messages(error.message, error.args.map(_.toString): _*)}"
                  else
                    s"${messages(s"label.${error.key}$messagesKeySuffix.$ownershipStatusSuffix")} - ${messages(error.message, error.args.map(_.toString): _*)}"
              )
          )
        )
      }
  }

  def valueWithId(value: String, id: String): HtmlContent =
    HtmlContent(s"""<span id="$id">$value</span>""")

  def summaryListMultiValues(id: String, values: List[String]): HtmlContent =
    HtmlContent(s"""<p class="govuk-body" id="$id">${values.zipWithIndex.map { case (value, index) =>
        if (index == 0) value else s"<br>$value"
      }.mkString}</p>""")

  def summaryListRow(keyId: String, key: String, valueId: String, value: String)(implicit messages: Messages) =
    SummaryListRow(
      key = Key(
        content = valueWithId(messages(key), keyId)
      ),
      value = Value(
        content = valueWithId(value, valueId)
      )
    )
}
