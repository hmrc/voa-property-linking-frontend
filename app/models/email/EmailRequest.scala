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

package models.email

import models.{DetailedIndividualAccount, GroupAccount}
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}

case class EmailRequest(to: List[String], templateId: String, parameters: Map[String, String])

object EmailRequest {

  implicit val format: OFormat[EmailRequest] = Json.format[EmailRequest]

  def registration(
                    to: String,
                    detailedIndividualAccount: DetailedIndividualAccount,
                    groupAccount: GroupAccount,
                    affinityGroupOpt: AffinityGroup): EmailRequest = {
    if (groupAccount.isAgent) {
      EmailRequest(List(to), "cca_enrolment_confirmation_agent",
        Map(
          "agentCode" -> groupAccount.agentCode.toString,
          "orgName" -> groupAccount.companyName,
          "personId" -> detailedIndividualAccount.individualId.toString,
          "name" -> s"${detailedIndividualAccount.details.firstName} ${detailedIndividualAccount.details.lastName}"
        )
      )
    } else {
      affinityGroupOpt match {
        case Individual  =>
          EmailRequest(List(to), "cca_enrolment_confirmation_individual",
            Map(
              "personId" -> detailedIndividualAccount.individualId.toString,
              "name" -> s"${detailedIndividualAccount.details.firstName} ${detailedIndividualAccount.details.lastName}"
            )
          )
        case Organisation =>
          EmailRequest(List(to), "cca_enrolment_confirmation",
            Map(
              "orgName" -> groupAccount.companyName,
              "personId" -> detailedIndividualAccount.individualId.toString,
              "name" -> s"${detailedIndividualAccount.details.firstName} ${detailedIndividualAccount.details.lastName}"
            )
          )
      }
    }
  }


}