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

package binders.pagination

import binders.validation.ValidationUtils
import binders.{Params, ValidationResult}
import play.api.Logger
import play.api.mvc.QueryStringBindable
import utils.Cats
import utils.QueryParamUtils.toQueryString

case class PaginationParameters(
      page: Int = 1,
      pageSize: Int = 15
) {

  def startPoint: Int = ((page - 1) * pageSize) + 1

  def previousPage: PaginationParameters = this.copy(page = page - 1)

  def nextPage: PaginationParameters = copy(page = page + 1)

}

object PaginationParameters extends ValidationUtils {

  private val logger = Logger(this.getClass.getName)

  implicit object Binder extends QueryStringBindable[PaginationParameters] with Cats {

    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, PaginationParameters]] =
      Some(validate(params).leftMap(_.map(_.message).toList.mkString(", ")).toEither)

    override def unbind(key: String, value: PaginationParameters): String = toQueryString(value)

    private def validate(params: Params): ValidationResult[PaginationParameters] =
      (
        validatePage("page", params),
        validatePageSize("pageSize", params)
      ).mapN {
        logger.debug("validation of get pagination parameters")
        PaginationParameters.apply _
      }

    def validatePage(implicit key: String, params: Params): ValidationResult[Int] =
      readWithDefault("1")(key, params) andThen asInt

    def validatePageSize(implicit key: String, params: Params): ValidationResult[Int] =
      readWithDefault("15")(key, params) andThen asInt
  }
}
