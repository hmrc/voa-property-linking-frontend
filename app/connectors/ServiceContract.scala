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

package connectors

import models.{CapacityType, NamedEnum, NamedEnumSupport}
import org.joda.time.DateTime

case class CapacityDeclaration(capacity: CapacityType, fromDate: DateTime, toDate: Option[DateTime] = None)

case class PropertyLink(uarn: String, userId: String, capacityDeclaration: CapacityDeclaration,
                        linkedDate: DateTime, assessmentYears: Seq[Int], pending: Boolean, requestFlag: RequestFlag)

case class LinkedProperties(added: Seq[PropertyLink], pending: Seq[PropertyLink])

case class PropertyRepresentation(representationId: String, agentId: String, userId: String, uarn: String,
                                  canCheck: Boolean, canChallenge: Boolean, pending: Boolean)


sealed trait RequestFlag extends NamedEnum {
  val key = "requestFlag"
}

case object SelfCertifyFlag extends RequestFlag {
  val name = "selfCertify"
}

case object RatesBillFlag extends RequestFlag {
  val name = "ratesBill"
}

case object OtherEvidenceFlag extends RequestFlag {
  val name = "otherEvidence"
}

case object NoEvidenceFlag extends RequestFlag {
  override val name = "noEvidence"
}

object RequestFlag extends NamedEnumSupport[RequestFlag] {
  def unapply(s: String) = s match {
    case SelfCertifyFlag.name => Some(SelfCertifyFlag)
    case RatesBillFlag.name => Some(RatesBillFlag)
    case OtherEvidenceFlag.name => Some(OtherEvidenceFlag)
    case NoEvidenceFlag.name => Some(NoEvidenceFlag)
    case _ => None
  }

  override def all: List[RequestFlag] = List(SelfCertifyFlag, RatesBillFlag, OtherEvidenceFlag, NoEvidenceFlag)
}


