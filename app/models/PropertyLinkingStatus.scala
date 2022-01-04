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

sealed trait PropertyLinkingStatus extends NamedEnum {
  val name: String
  val key = "propertyLinkingStatus"
  override def toString = name
}

case object PropertyLinkingApproved extends PropertyLinkingStatus {
  val name = "APPROVED"
}

case object PropertyLinkingPending extends PropertyLinkingStatus {
  val name = "PENDING"
}

case object PropertyLinkingRevoked extends PropertyLinkingStatus {
  override val name = "REVOKED"
}

case object PropertyLinkingDeclined extends PropertyLinkingStatus {
  override val name = "DECLINED"
}

case object PropertyLinkingMoreEvidenceRequired extends PropertyLinkingStatus {
  override val name = "MORE_EVIDENCE_REQUIRED"
}

object PropertyLinkingStatus extends NamedEnumSupport[PropertyLinkingStatus] {
  implicit val format = EnumFormat(PropertyLinkingStatus)

  override def all: List[PropertyLinkingStatus] =
    List(
      PropertyLinkingApproved,
      PropertyLinkingPending,
      PropertyLinkingRevoked,
      PropertyLinkingDeclined,
      PropertyLinkingMoreEvidenceRequired)
}
