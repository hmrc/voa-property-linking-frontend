/*
 * Copyright 2020 HM Revenue & Customs
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
import models.propertyrepresentation.AppointNewAgentSession
import models.{Accounts, DetailedIndividualAccount, GroupAccount, LinkingSession}
import play.api.mvc.{Request, WrappedRequest}
import uk.gov.hmrc.play.HeaderCarrierConverter

case class AppointAgentSessionRequest[A](
      sessionData: AppointNewAgentSession,
      organisationId: Long,
      individualAccount: DetailedIndividualAccount,
      groupAccount: GroupAccount,
      request: Request[A]
) extends WrappedRequest[A](request) with CcaWrappedRequest {
  override def isLoggedIn = true

  override def optAccounts = Some(Accounts(organisation = groupAccount, person = individualAccount))

  override def yourDetailsName = getYourDetailsName(optAccounts)

  def sessionId: String =
    HeaderCarrierConverter
      .fromHeadersAndSession(request.headers, Some(request.session))
      .sessionId
      .map(_.value)
      .getOrElse(throw NoSessionId)
}

case object NoSessionId extends Exception
