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

package binders.propertylinks

import binders.propertylinks.ExternalPropertyLinkManagementSortField._
import binders.propertylinks.ExternalPropertyLinkManagementSortOrder._
import binders.validation.ValidationUtils
import binders.{Params, ValidationResult}
import play.api.mvc.QueryStringBindable
import utils.Cats
import utils.QueryParamUtils.toQueryString

case class GetPropertyLinksParameters(
      address: Option[String] = None,
      baref: Option[String] = None,
      agent: Option[String] = None,
      status: Option[String] = None,
      sortfield: ExternalPropertyLinkManagementSortField = ADDRESS,
      sortorder: ExternalPropertyLinkManagementSortOrder = ASC
) {

  def reverseSorting: GetPropertyLinksParameters =
    this.copy(sortorder = sortorder match {
      case ASC  => DESC
      case DESC => ASC
    })
}

object GetPropertyLinksParameters extends ValidationUtils {

  implicit object Binder extends QueryStringBindable[GetPropertyLinksParameters] with Cats {

    override def bind(
          key: String,
          params: Map[String, Seq[String]]): Option[Either[String, GetPropertyLinksParameters]] =
      Some(validate(params).leftMap(_.map(_.message).toList.mkString(", ")).toEither)

    override def unbind(key: String, value: GetPropertyLinksParameters): String = toQueryString(value)

    private def validate(params: Params): ValidationResult[GetPropertyLinksParameters] =
      (
        validateString("address", params),
        validateString("baref", params),
        validateString("agent", params),
        validateString("status", params),
        validateSortField("sortfield", params),
        validateSortOrder("sortorder", params)
      ).mapN(GetPropertyLinksParameters.apply)

    def validateSortOrder(
          implicit key: String,
          params: Params): ValidationResult[ExternalPropertyLinkManagementSortOrder] =
      readWithDefault("ASC")(key, params) andThen enumValue(ExternalPropertyLinkManagementSortOrder)

    def validateSortField(
          implicit key: String,
          params: Params): ValidationResult[ExternalPropertyLinkManagementSortField] =
      readWithDefault("ADDRESS")(key, params) andThen enumValue(ExternalPropertyLinkManagementSortField)

    def validateString(implicit key: String, params: Params): ValidationResult[Option[String]] =
      readOption(key, params) ifPresent maxLength(1000)

  }
}
