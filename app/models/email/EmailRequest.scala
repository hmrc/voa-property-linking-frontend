/*
 * Copyright 2018 HM Revenue & Customs
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

package models.email

import models.DetailedIndividualAccount
import play.api.libs.json.{Json, OFormat}

case class EmailRequest(to: List[String], templateId: String, parameters: Map[String, String])

object EmailRequest {

  implicit val format: OFormat[EmailRequest] = Json.format[EmailRequest]

  def registration(to: String, detailedIndividualAccount: DetailedIndividualAccount): EmailRequest =
    EmailRequest(List(to), "cca_enrolment_confirmation",
      Map(
      "personId" -> detailedIndividualAccount.individualId.toString,
      "name" -> s"${detailedIndividualAccount.details.firstName} ${detailedIndividualAccount.details.lastName}"
      )
    )

}