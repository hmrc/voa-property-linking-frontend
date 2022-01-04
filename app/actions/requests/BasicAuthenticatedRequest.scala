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

package actions.requests

import models.{Accounts, DetailedIndividualAccount, GroupAccount}
import play.api.mvc.Request
import uk.gov.hmrc.auth.core.AffinityGroup

//need to decouple to authentication away from the group account and individual account.
case class BasicAuthenticatedRequestWithAffinityGroup[A](
      organisationAccount: GroupAccount,
      individualAccount: DetailedIndividualAccount,
      affinityGroup: AffinityGroup,
      request: Request[A]
) extends AuthenticatedRequest[A](request)

case class BasicAuthenticatedRequest[A](
      organisationAccount: GroupAccount,
      individualAccount: DetailedIndividualAccount,
      request: Request[A]
) extends AuthenticatedRequest[A](request) with CcaWrappedRequest {
  override def optAccounts: Option[Accounts] =
    Some(Accounts(organisationAccount, individualAccount))
}

case class AgentRequest[A](
      organisationAccount: GroupAccount,
      individualAccount: DetailedIndividualAccount,
      agentCode: Long,
      request: Request[A]
) extends AuthenticatedRequest[A](request)
