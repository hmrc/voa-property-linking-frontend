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

package session

import javax.inject.{Inject, Named}

import actions.AuthenticatedAction
import config.Global
import models.{DetailedIndividualAccount, GroupAccount, LinkingSession}
import play.api.i18n.Messages
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Results._
import play.api.mvc._
import repositories.SessionRepo

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

case class LinkingSessionRequest[A](ses: LinkingSession, organisationId: Long,
                                    individualAccount: DetailedIndividualAccount,
                                    groupAccount: GroupAccount, request: Request[A]) extends WrappedRequest[A](request) {
  def sessionId: String = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session)).sessionId.map(_.value).getOrElse(throw NoSessionId)
}

case object NoSessionId extends Exception

class WithLinkingSession @Inject() (authenticated: AuthenticatedAction,
                                     @Named("propertyLinkingSession") val sessionRepository: SessionRepo) {
  implicit def hc(implicit request: Request[_]) = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

  def apply(body: LinkingSessionRequest[AnyContent] => Future[Result])(implicit messages: Messages) = authenticated { implicit request =>
    sessionRepository.get[LinkingSession] flatMap {
      case Some(s) => body(
        LinkingSessionRequest(s, request.organisationAccount.id, request.individualAccount, request.organisationAccount, request)
      )
      case None => Future.successful(NotFound(Global.notFoundTemplate))
    }
  }
}