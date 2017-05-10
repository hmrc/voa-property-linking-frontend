/*
 * Copyright 2017 HM Revenue & Customs
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

import models.{DetailedIndividualAccount, GroupAccount, LinkingSession}
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, Result}
import repositories.SessionRepo
import session.{LinkingSessionRequest, WithLinkingSession}

import scala.concurrent.Future

class StubWithLinkingSession(sessionRepository: SessionRepo ) extends WithLinkingSession(sessionRepository) {

  private var stubbedSession: Option[(LinkingSession, DetailedIndividualAccount, GroupAccount)] = None

  override def apply(body: (LinkingSessionRequest[AnyContent]) => Future[Result])(implicit messages: Messages) = Action.async { implicit request =>
    stubbedSession.fold(throw new Exception("Linking session not stubbed")) { case (linkingSession, person, organisation) =>
      body(LinkingSessionRequest(linkingSession, person.organisationId, person, organisation, request))
    }
  }

  def stubSession(linkingSession: LinkingSession, individualAccount: DetailedIndividualAccount, groupAccount: GroupAccount) = {
    stubbedSession = Some((linkingSession, individualAccount, groupAccount))
  }

  def reset() = {
    stubbedSession = None
  }
}
