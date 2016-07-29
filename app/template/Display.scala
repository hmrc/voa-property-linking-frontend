/*
 * Copyright 2016 HM Revenue & Customs
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

package template


import models.Address
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

object Display {
  private val pattern = DateTimeFormat.forPattern("dd/MM/yyyy")
  def date(d: DateTime): String = d.toString(pattern)

  def address(a: Address): String = a.lines.mkString(", ") + ", " + a.postcode
}
