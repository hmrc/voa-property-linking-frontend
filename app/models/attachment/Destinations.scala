/*
 * Copyright 2019 HM Revenue & Customs
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

package models.attachment

<<<<<<< HEAD:app/models/attachment/HistoryItem.scala
import java.time.Instant

import play.api.libs.json.{Json, OFormat}

case class HistoryItem(state: AttachmentState, timeStamp: Instant, details: Option[String] = None)

object HistoryItem {
  implicit val format: OFormat[HistoryItem] = Json.format
=======
import play.api.libs.json.Format
import utils.JsonUtils

object Destinations extends Enumeration {
  type Destinations = Value

  val PROPERTY_LINK_EVIDENCE_DFE = Value("PROPERTY_LINK_EVIDENCE_DFE")

  implicit val format: Format[Destinations] = JsonUtils.enumFormat(Destinations)

>>>>>>> VTCCA-2975 switching property linking to upscan v2 with needed clean up in certain areas to allow passing tests and to use libraries that simplify upscan v2 integration, the javascript now loads the response from the initiate into the page and submits the html form which redirects to the same page:app/models/attachment/Destinations.scala
}
