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

import form.EnumMapping
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Format

sealed trait EvidenceType extends NamedEnum {
  val name: String
  val key = "evidenceType"
  override def toString = name
}

case object Lease extends EvidenceType {
  val name = "lease"
}

case object License extends EvidenceType {
  val name = "license"
}

case object ServiceCharge extends EvidenceType {
  val name = "serviceCharge"
}

case object StampDutyLandTaxForm extends EvidenceType {
  val name = "stampDutyLandTaxForm"
}

case object WaterRateDemand extends EvidenceType {
  val name = "waterRateDemand"
}

case object OtherUtilityBill extends EvidenceType {
  val name = "otherUtilityBill"
}

case object RatesBillType extends EvidenceType {
  val name = "ratesBill"
}

case object LandRegistryTitle extends EvidenceType {
  val name = "landRegistryTitle"
}

case object UnableToProvide extends EvidenceType {
  val name = "unableToProvide"
}

object EvidenceType extends NamedEnumSupport[EvidenceType] {
  override def all: List[EvidenceType] =
    List(
      Lease,
      License,
      ServiceCharge,
      StampDutyLandTaxForm,
      WaterRateDemand,
      OtherUtilityBill,
      RatesBillType,
      LandRegistryTitle,
      UnableToProvide)

  implicit val format: Format[EvidenceType] = EnumFormat(EvidenceType)
  lazy val form: Form[EvidenceType] = Form(single("evidenceType" -> EnumMapping(EvidenceType)))
}
