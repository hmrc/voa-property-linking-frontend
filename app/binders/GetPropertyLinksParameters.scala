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

package binders

import binders.validation.ValidationUtils
import play.api.mvc.QueryStringBindable
import utils.Cats
import utils.QueryParamUtils.toQueryString

case class GetPropertyLinksParameters(address: Option[String] = None, baref: Option[String] = None, agent: Option[String] = None,
                                      status: Option[String] = None, sortfield: Option[String] = None, sortorder: Option[String] = None)

object GetPropertyLinksParameters extends ValidationUtils {

  implicit object Binder extends QueryStringBindable[GetPropertyLinksParameters] with Cats {

    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, GetPropertyLinksParameters]] =
      Some(validate(params).leftMap(_.map(_.show).toList.mkString(", ")).toEither)

    override def unbind(key: String, value: GetPropertyLinksParameters): String = toQueryString(value)

    private def validate(params: Params): ValidationResult[GetPropertyLinksParameters] =
      (validateString("address", params), validateString("baref", params), validateString("agent", params), validateString("status", params), validateString("sortfield", params), validateString("sortorder", params))
        .mapN(GetPropertyLinksParameters.apply)

    def validateString(implicit key: String, params: Params): ValidationResult[Option[String]] =
      readOption(key, params) ifPresent maxLength(1000)

  }
}
