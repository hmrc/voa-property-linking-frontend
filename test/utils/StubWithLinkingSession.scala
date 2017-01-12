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

import connectors.CapacityDeclaration
import models.{DetailedIndividualAccount, GroupAccount, Property}
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, Result}
import session.{LinkingSession, LinkingSessionRequest, WithLinkingSession}

import scala.concurrent.Future

class StubWithLinkingSession(p: Property, decl: CapacityDeclaration, ind: DetailedIndividualAccount, group: GroupAccount) extends WithLinkingSession {
  private val stubSession = LinkingSession(p, "envelopId", "submissionId", Some(decl), None)

  override def apply(body: (LinkingSessionRequest[AnyContent]) => Future[Result])(implicit messages: Messages) = Action.async { implicit request =>
    body(LinkingSessionRequest(stubSession, ind.organisationId, ind, group, request))
  }
}
