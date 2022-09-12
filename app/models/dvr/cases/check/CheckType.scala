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

package models.dvr.cases.check

sealed trait CheckType {
  def value: String
}

object CheckType {

  def of(s: String): CheckType =
    all
      .find(_.value == s)
      .getOrElse(throw new IllegalArgumentException(s"'$s' not known as a valid ${getClass.getSimpleName}"))

  case object Internal extends CheckType {
    override val value = "internal"
  }

  case object External extends CheckType {
    override val value = "external"
  }

  case object Split extends CheckType {
    override val value = "split"
  }

  case object Merge extends CheckType {
    override val value = "merge"
  }

  case object Remove extends CheckType {
    override val value = "remove"
  }

  case object LegalDecision extends CheckType {
    override val value = "legal-decision"
  }

  case object RateableValueTooHigh extends CheckType {
    val value = "rateableValueTooHigh"
  }

  val all: Seq[CheckType] = Seq(Internal, External, Split, Merge, Remove, LegalDecision, RateableValueTooHigh)
}
