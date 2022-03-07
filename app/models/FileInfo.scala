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

package models

import play.api.libs.json._

sealed trait FileInfo {
  val evidenceType: EvidenceType
}

case class PartialFileInfo(evidenceType: EvidenceType) extends FileInfo

object PartialFileInfo {
  implicit val format = Json.format[PartialFileInfo]
}

case class CompleteFileInfo(name: String, evidenceType: EvidenceType) extends FileInfo

object CompleteFileInfo {
  implicit val format = Json.format[CompleteFileInfo]
}

object FileInfo {
  implicit val format = Json.format[FileInfo]
}
