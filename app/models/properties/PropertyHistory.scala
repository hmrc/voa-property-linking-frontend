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

package models.properties

import models.properties.AllowedAction.AllowedAction
import play.api.libs.json.{Json, OFormat}

case class PropertyHistory(
      uarn: Long,
      addressFull: String,
      localAuthorityCode: String,
      localAuthorityReference: String,
      history: Seq[PropertyValuation],
      allowedActions: List[AllowedAction]
) {
  def propertyValuation(valuationId: Long): Option[PropertyValuation] =
    history.find(_.valuationId == valuationId)

  def isWelsh: Boolean = localAuthorityCode.startsWith("6")
}

object PropertyHistory {
  implicit val format: OFormat[PropertyHistory] = Json.format
}
