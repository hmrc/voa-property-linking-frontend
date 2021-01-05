/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.libs.json.Json
import exceptions._
case class Capacity(capacity: CapacityType, fromDate: LocalDate, toDate: Option[LocalDate])

object Capacity {
  implicit val format = Json.format[Capacity]

  lazy val defaultFromDate = LocalDate.of(2017, 4, 1)

  def apply(linkingSession: LinkingSession): Capacity =
    new Capacity(
      linkingSession.propertyRelationship.map{relationship => relationship.capacity }.getOrElse(throw new ClaimPropertyRelationshipIsEmptyException("property claim relationship should not be empty")),
      linkingSession.propertyOwnership.flatMap{ownership => ownership.fromDate}.getOrElse(defaultFromDate),
      linkingSession.propertyOwnership.flatMap{ownership => ownership.toDate})

  def fromDeclaration(declaration: CapacityDeclaration) =
    Capacity(declaration.capacity, declaration.fromDate.getOrElse(defaultFromDate), declaration.toDate)

}
