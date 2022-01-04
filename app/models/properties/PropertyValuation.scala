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

import models.ListType.ListType
import models.properties.AllowedAction.AllowedAction
import models.properties.ValuationStatus.ValuationStatus
import models.referencedata.ReferenceData
import play.api.libs.json.{Json, OFormat}

import java.time.LocalDate

case class PropertyValuation(
      valuationId: Long,
      valuationStatus: ValuationStatus,
      rateableValue: Option[BigDecimal],
      scatCode: Option[String],
      effectiveDate: LocalDate,
      currentFromDate: LocalDate,
      currentToDate: Option[LocalDate],
      listYear: String,
      primaryDescription: ReferenceData,
      allowedActions: List[AllowedAction],
      listType: ListType,
      propertyLinkEarliestStartDate: Option[LocalDate]
)

object PropertyValuation {
  implicit val format: OFormat[PropertyValuation] = Json.format
}
