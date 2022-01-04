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

package models.email

import models.GroupAccount.AgentGroupAccount
import models.{DetailedIndividualAccount, GroupAccount}
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

case class EmailRequest(to: List[String], templateId: String, parameters: Map[String, String])

object EmailRequest {

  implicit val format: OFormat[EmailRequest] = Json.format[EmailRequest]

  def registration(
        to: String,
        detailedIndividualAccount: DetailedIndividualAccount,
        groupAccount: Option[GroupAccount],
        affinityGroupOpt: Option[AffinityGroup] = None): EmailRequest =
    groupAccount match {
      case Some(AgentGroupAccount(groupAccount, agentCode)) =>
        EmailRequest(
          List(to),
          "cca_enrolment_confirmation_agent",
          Map(
            "agentCode" -> agentCode.toString,
            "orgName"   -> groupAccount.companyName,
            "personId"  -> detailedIndividualAccount.individualId.toString,
            "name"      -> s"${detailedIndividualAccount.details.firstName} ${detailedIndividualAccount.details.lastName}"
          )
        )

      case Some(acc) =>
        affinityGroupOpt match {
          case None => throw new IllegalStateException("No AffinityGroup for logged in user")
          case Some(affinityGroup) if affinityGroup == Individual =>
            EmailRequest(
              List(to),
              "cca_enrolment_confirmation_individual",
              Map(
                "personId" -> detailedIndividualAccount.individualId.toString,
                "name"     -> s"${detailedIndividualAccount.details.firstName} ${detailedIndividualAccount.details.lastName}"
              )
            )
          case _ =>
            EmailRequest(
              List(to),
              "cca_enrolment_confirmation",
              Map(
                "orgName"  -> acc.companyName,
                "personId" -> detailedIndividualAccount.individualId.toString,
                "name"     -> s"${detailedIndividualAccount.details.firstName} ${detailedIndividualAccount.details.lastName}"
              )
            )
        }
      case None => throw new IllegalStateException("No GroupAccount in the session")
    }

}
