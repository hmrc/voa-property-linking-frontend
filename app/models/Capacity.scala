/*
 * Copyright 2024 HM Revenue & Customs
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

package models

import java.time.LocalDate
import play.api.libs.json.{Json, OFormat}
import exceptions._

case class Capacity(capacity: CapacityType, fromDate: LocalDate, toDate: Option[LocalDate])

object Capacity {
  implicit val format: OFormat[Capacity] = Json.format[Capacity]

  def apply(linkingSession: LinkingSession): Capacity =
    Capacity(
      capacity = linkingSession.propertyRelationship
        .fold(throw PropertyRelationshipException("property claim relationship should not be empty"))(_.capacity),
      fromDate = linkingSession.propertyOwnership.fold(linkingSession.earliestStartDate) { propertyOwnership =>
        if (propertyOwnership.fromDate.isBefore(linkingSession.earliestStartDate))
          linkingSession.earliestStartDate
        else propertyOwnership.fromDate
      },
      toDate = linkingSession.propertyOccupancy.flatMap(_.lastOccupiedDate)
    )
}
