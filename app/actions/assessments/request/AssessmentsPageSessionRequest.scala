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

package actions.assessments.request

import actions.requests.{BasicAuthenticatedRequest, CcaWrappedRequest}
import models.assessments.AssessmentsPageSession
import models.assessments.PreviousPage.PreviousPage
import models.{Accounts, DetailedIndividualAccount, GroupAccount}
import play.api.mvc.{Request, WrappedRequest}

case class AssessmentsPageSessionRequest[A](
      sessionData: AssessmentsPageSession,
      individualAccount: DetailedIndividualAccount,
      groupAccount: GroupAccount,
      request: Request[A]
) extends WrappedRequest[A](request) with CcaWrappedRequest {

  override def optAccounts: Option[Accounts] = Some(Accounts(organisation = groupAccount, person = individualAccount))

  def organisationId: Long = groupAccount.id

  def personId: Long = individualAccount.individualId

  def previousPage: PreviousPage = sessionData.previousPage
}

object AssessmentsPageSessionRequest {
  def apply[A](
        assessmentsPageSession: AssessmentsPageSession,
        basicAuthenticatedRequest: BasicAuthenticatedRequest[A]): AssessmentsPageSessionRequest[A] =
    AssessmentsPageSessionRequest(
      assessmentsPageSession,
      basicAuthenticatedRequest.individualAccount,
      basicAuthenticatedRequest.organisationAccount,
      basicAuthenticatedRequest)
}
