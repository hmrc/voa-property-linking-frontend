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
import play.api.libs.functional.syntax._

case class Organisation(
      id: Long,
      groupId: String,
      companyName: String,
      addressId: Long,
      email: String,
      phone: String,
      isAgent: Boolean,
      agentCode: Long)

object Organisation {
  val apiFormat: Reads[Organisation] = (
    (__ \ "id").read[Long] and
      (__ \ "governmentGatewayGroupId").read[String] and
      (__ \ "organisationLatestDetail" \ "organisationName").read[String] and
      (__ \ "organisationLatestDetail" \ "addressUnitId").read[Long] and
      (__ \ "organisationLatestDetail" \ "organisationEmailAddress").read[String] and
      (__ \ "organisationLatestDetail" \ "organisationTelephoneNumber")
        .read[String] | Reads.pure[String]("not set") and
      (__ \ "organisationLatestDetail" \ "representativeFlag").read[Boolean] and
      (__ \ "representativeCode").read[Long]
  )(Organisation.apply _)

  implicit val format: OFormat[Organisation] = Json.format[Organisation]
}
