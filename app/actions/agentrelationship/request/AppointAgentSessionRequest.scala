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

package actions.agentrelationship.request

import actions.requests.CcaWrappedRequest
import models.propertyrepresentation._
import models.{Accounts, DetailedIndividualAccount, GroupAccount}
import play.api.mvc.{Request, WrappedRequest}

case class AppointAgentSessionRequest[A](
      sessionData: AppointNewAgentSession,
      individualAccount: DetailedIndividualAccount,
      groupAccount: GroupAccount,
      request: Request[A]
) extends WrappedRequest[A](request) with CcaWrappedRequest {

  override def optAccounts = Some(Accounts(organisation = groupAccount, person = individualAccount))

  def organisationId = groupAccount.id

  def agentDetails = sessionData match {
    case SearchedAgent(_, agentOrganisationName, agentAddress, _, _) =>
      AgentDetails(agentOrganisationName, agentAddress)
    case SelectedAgent(_, agentOrganisationName, agentAddress, _, _, _) =>
      AgentDetails(agentOrganisationName, agentAddress)
    case ManagingProperty(_, agentOrganisationName, agentAddress, _, _, _, _, _) =>
      AgentDetails(agentOrganisationName, agentAddress)
  }
}
