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

package binders.pagination

import binders.validation.{Params, ValidatingBinder, ValidationResult}

case class PaginationParameters(
                                 page: Int,
                                 pageSize: Int
                               ) {
  val startPoint: Int = pageSize * (page - 1) + 1
}

object PaginationParameters extends ValidatingBinder[PaginationParameters] {

  override def validate(params: Params): ValidationResult[PaginationParameters] =
    (
      validatePage("page", params),
      validatePageSize("pageSize", params)
    ).mapN(PaginationParameters.apply)

  def validatePage(implicit key: String, params: Params): ValidationResult[Int] =
    read andThen asInt andThen min(1)

  def validatePageSize(implicit key: String, params: Params): ValidationResult[Int] =
    read andThen asInt andThen min(10) andThen max(100)
}
