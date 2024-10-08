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

import play.api.libs.json.Format

sealed trait RepresentationResponseStatus extends NamedEnum {
  val name: String
  val key = "propReprStatus"
  override def toString = name
}

case object RepresentationResponseApproved extends RepresentationResponseStatus {
  val name = "APPROVED"
}

case object RepresentationResponseDeclined extends RepresentationResponseStatus {
  val name = "DECLINED"
}

object RepresentationResponseStatus extends NamedEnumSupport[RepresentationResponseStatus] {
  implicit val format: Format[RepresentationResponseStatus] = EnumFormat(RepresentationResponseStatus)

  override def all: List[RepresentationResponseStatus] =
    List(RepresentationResponseApproved, RepresentationResponseDeclined)
}
