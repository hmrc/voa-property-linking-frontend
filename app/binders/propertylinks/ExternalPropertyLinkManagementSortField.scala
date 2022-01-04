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

import play.api.mvc.QueryStringBindable

object ExternalPropertyLinkManagementSortField extends Enumeration {
  type ExternalPropertyLinkManagementSortField = Value

  val ADDRESS = Value("ADDRESS")
  val BAREF = Value("BAREF")
  val AGENT = Value("AGENT")
  val STATUS = Value("STATUS")
  val REPRESENTATION_STATUS = Value("REPRESENTATION_STATUS")

  implicit val queryStringBindable: QueryStringBindable[ExternalPropertyLinkManagementSortField] =
    new QueryStringBindable[ExternalPropertyLinkManagementSortField] {
      override def bind(
            key: String,
            params: Map[String, Seq[String]]
      ): Option[Either[String, ExternalPropertyLinkManagementSortField]] =
        params.get(key).collect {
          case Seq(s) =>
            ExternalPropertyLinkManagementSortField.values.find(_.toString == s).toRight("invalid sort field parameter")
        }

      override def unbind(key: String, value: ExternalPropertyLinkManagementSortField): String =
        implicitly[QueryStringBindable[String]].unbind(key, value.toString)
    }
}
