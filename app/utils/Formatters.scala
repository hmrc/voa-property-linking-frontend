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

package utils

import models.PropertyAddress
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat

object Formatters {

  def capitalizedAddress(address: PropertyAddress) = {
    address.copy(
      lines = address.lines.map(_.toLowerCase().trim.split(" ").map(_.capitalize).mkString(" ")),
      postcode = address.postcode.toUpperCase()
    ).toString
  }

  def capitalizedAddress(address: String): String = {
    capitalizedAddress(PropertyAddress.fromString(address))
  }

  def formatDate(fmt: String, date: LocalDate) = {
    val formatter = DateTimeFormat.forPattern(fmt)
    date.toString(formatter)
  }

  def formatDate(date: LocalDate) = {
    date.toString(DateTimeFormat.forPattern("d MMM Y"))
  }

}
