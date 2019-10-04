/*
 * Copyright 2019 HM Revenue & Customs
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

package binders.propertylinks

import binders.propertylinks.EvidenceChoices.EvidenceChoices
import binders.validation.ValidationUtils
import binders.{Params, ValidationResult}
import play.api.Logger
import play.api.mvc.QueryStringBindable
import utils.Cats
import utils.QueryParamUtils.toQueryString

case class UploadEvidenceChoiceParameters(choice: EvidenceChoices)

object UploadEvidenceChoiceParameters extends ValidationUtils {

  private val logger = Logger(this.getClass.getName)

  implicit object Binder extends QueryStringBindable[UploadEvidenceChoiceParameters] with Cats {

    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, UploadEvidenceChoiceParameters]] =
      Some(validate(params).leftMap(_.map(_.message).toList.mkString(", ")).toEither)

    override def unbind(key: String, value: UploadEvidenceChoiceParameters): String = toQueryString(value)

    private def validate(params: Params): ValidationResult[UploadEvidenceChoiceParameters] =
      validateChoice("choice", params).map(UploadEvidenceChoiceParameters.apply)


    def validateChoice(implicit key: String, params: Params): ValidationResult[EvidenceChoices] = {
      read andThen enumValue(EvidenceChoices)
    }
  }
}