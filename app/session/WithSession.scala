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

package session

import javax.inject.Inject

import config.Wiring
import models.{DetailedIndividualAccount, GroupAccount, LinkingSession}
import play.api.i18n.Messages
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Results.Unauthorized
import play.api.mvc._
import repositories.{SessionRepo, SessionRepository}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

case class LinkingSessionRequest[A](ses: LinkingSession, organisationId: Int,
                                    individualAccount: DetailedIndividualAccount,
                                    groupAccount: GroupAccount, request: Request[A]) extends WrappedRequest[A](request) {
  def sessionId: String = HeaderCarrier.fromHeadersAndSession(request.headers, Some(request.session)).sessionId.map(_.value).getOrElse(throw NoSessionId)
}

case object NoSessionId extends Exception

class WithLinkingSession @Inject() (val sessionRepository: SessionRepo) {
  implicit def hc(implicit request: Request[_]) = HeaderCarrier.fromHeadersAndSession(request.headers, Some(request.session))
  val individualAccountConnector = Wiring().individualAccountConnector
  val groupAccountConnector = Wiring().groupAccountConnector
  val auth = Wiring().authConnector
  val ggAction = Wiring().ggAction
  val authenticated = Wiring().authenticated

  def apply(body: LinkingSessionRequest[AnyContent] => Future[Result])(implicit messages: Messages) = authenticated { implicit request =>
    sessionRepository.get[LinkingSession] flatMap {
      case Some(s) => body(
        LinkingSessionRequest(s, request.organisationAccount.id, request.individualAccount, request.organisationAccount, request)
      )
      case None => Future.successful(Unauthorized("No linking session"))
    }
  }
}