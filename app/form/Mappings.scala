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

package form

import org.joda.time.DateTime
import play.api.data.Mapping
import play.api.data.Forms._

object Mappings {

 def dmyDate: Mapping[DateTime] = mapping (
  "day" -> number(1, 31),
  "month" -> number(1, 12),
  "year" -> number(1900, 3000)
 )((d, m, y) => new DateTime(y, m, d, 0, 0,0, 0))(d => Some(d.dayOfMonth.get, d.monthOfYear.get, d.year.get))

}
