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

package models

sealed trait LinkBasis extends NamedEnum {
  val key = "requestFlag"
}

case object SelfCertifyFlag extends LinkBasis {
  val name = "selfCertify"
}

case object RatesBillFlag extends LinkBasis {
  val name = "ratesBill"
}

case object OtherEvidenceFlag extends LinkBasis {
  val name = "otherEvidence"
}

case object NoEvidenceFlag extends LinkBasis {
  override val name = "noEvidence"
}

object LinkBasis extends NamedEnumSupport[LinkBasis] {
  override def all: List[LinkBasis] = List(SelfCertifyFlag, RatesBillFlag, OtherEvidenceFlag, NoEvidenceFlag)
}
