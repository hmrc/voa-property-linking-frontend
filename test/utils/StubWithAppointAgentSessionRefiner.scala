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

package utils

import actions.agentrelationship.WithAppointAgentSessionRefiner
import actions.agentrelationship.request.AppointAgentSessionRequest
import actions.requests.BasicAuthenticatedRequest
import models.propertyrepresentation.AppointNewAgentSession
import models.{DetailedIndividualAccount, GroupAccount}
import org.mockito.Mockito.mock
import play.api.mvc.Result
import repositories.SessionRepo
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import scala.concurrent.{ExecutionContext, Future}

class StubWithAppointAgentSessionRefiner(sessionRepository: SessionRepo)
    extends WithAppointAgentSessionRefiner(mock(classOf[CustomErrorHandler]), sessionRepository)(
      ExecutionContext.global) {

  private var stubbedSession: Option[(AppointNewAgentSession, DetailedIndividualAccount, GroupAccount)] = None

  override def refine[A](request: BasicAuthenticatedRequest[A]): Future[Either[Result, AppointAgentSessionRequest[A]]] =
    stubbedSession.fold(throw new Exception("Appoint agent session not stubbed")) {
      case (appointAgentSession, person, organisation) =>
        Future.successful(Right(AppointAgentSessionRequest(appointAgentSession, person, organisation, request)))
    }

  def stubSession(
        appointAgentSession: AppointNewAgentSession,
        individualAccount: DetailedIndividualAccount,
        groupAccount: GroupAccount) =
    stubbedSession = Some((appointAgentSession, individualAccount, groupAccount))

  def reset() =
    stubbedSession = None
}
