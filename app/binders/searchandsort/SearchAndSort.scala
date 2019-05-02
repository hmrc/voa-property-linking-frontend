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

package binders.searchandsort

import binders.validation.{Params, ValidatingBinder, ValidationResult}

case class SearchAndSort(
                                              sortfield: Option[String] = None,
                                              sortorder: String = "ASC",
                                              status: Option[String] = None,
                                              address: Option[String] = None,
                                              baref: Option[String] = None,
                                              agent: Option[String] = None,
                                              client: Option[String] = None,
                                              totalResults: Long = 0
                                            )

object SearchAndSort extends ValidatingBinder[SearchAndSort] {

  override def validate(params: Params): ValidationResult[SearchAndSort] =
    (
      validateSortField("sortField", params),
      validateSortOrder("sortorder", params),
      validateStatus("status", params),
      validateAddress("address", params),
      validateBaref("baref", params),
      validateAgent("agent", params),
      validateClient("client", params),
      validateTotalResults("totalResults", params)
    ).mapN(SearchAndSort.apply)

  private def validateSortField(implicit key: String, params: Params): ValidationResult[Option[String]]=
    readOption

  private def validateSortOrder(implicit key: String, params: Params): ValidationResult[String]=
    readWithDefault("ASC")

  private def validateStatus(implicit key: String, params: Params): ValidationResult[Option[String]]=
    readOption

  private def validateAddress(implicit key: String, params: Params): ValidationResult[Option[String]]=
    readOption

  private def validateBaref(implicit key: String, params: Params): ValidationResult[Option[String]]=
    readOption

  private def validateAgent(implicit key: String, params: Params): ValidationResult[Option[String]]=
    readOption

  private def validateClient(implicit key: String, params: Params): ValidationResult[Option[String]]=
    readOption

  private def validateTotalResults(implicit key: String, params: Params): ValidationResult[Long] =
    read andThen asLong
}
