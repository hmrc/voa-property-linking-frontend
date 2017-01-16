/*
 * Copyright 2017 HM Revenue & Customs
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

import connectors.CapacityDeclaration
import org.joda.time.LocalDate
import play.api.libs.json.Json
import serialization.JsonFormats._

case class Capacity(capacity: CapacityType, fromDate: LocalDate, toDate: Option[LocalDate])

object Capacity {
  implicit val format = Json.format[Capacity]

  lazy val defaultFromDate = new LocalDate(2017, 4, 1)

  def fromDeclaration(declaration: CapacityDeclaration) = {
    Capacity(declaration.capacity, declaration.fromDate.getOrElse(defaultFromDate), declaration.toDate)
  }
}
