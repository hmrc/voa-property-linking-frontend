/*
 * Copyright 2020 HM Revenue & Customs
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

package models.propertyrepresentation

import models.{EnumFormat, NamedEnum, NamedEnumSupport}

object OnePropertyOptions {

  val asWellAsCurrent = "AS_WELL_AS_CURRENT"
  val insteadOfCurrent = "INSTEAD_OF_CURRENT"
  val no = "NO"
  val yes = "YES"


  def all: List[String] = List(asWellAsCurrent, insteadOfCurrent, yes, no)
}