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

import play.api.libs.json.Format
import play.api.mvc.QueryStringBindable
import utils.JsonUtils

object ClaimPropertyReturnToPage extends Enumeration {
  type ClaimPropertyReturnToPage = Value

  val FMBR = Value("fmbr")
  val SummaryValuation = Value("summary-valuation")
  val SummaryValuationHelp = Value("summary-valuation-help")

  implicit val queryStringBindable: QueryStringBindable[ClaimPropertyReturnToPage] =
    new QueryStringBindable[ClaimPropertyReturnToPage] {
      override def bind(
            key: String,
            params: Map[String, Seq[String]]
      ): Option[Either[String, ClaimPropertyReturnToPage]] =
        params.get(key).collect {
          case Seq(s) =>
            ClaimPropertyReturnToPage.values.find(_.toString == s).toRight("invalid value")
        }

      override def unbind(key: String, value: ClaimPropertyReturnToPage): String = s"rtp=${value.toString}"
    }

  implicit val format: Format[ClaimPropertyReturnToPage] = JsonUtils.enumFormat(ClaimPropertyReturnToPage)
}
