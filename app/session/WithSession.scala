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

import actions.BasicAuthenticatedRequest
import javax.inject.{Inject, Named}
import models.{DetailedIndividualAccount, GroupAccount, LinkingSession}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Reads
import play.api.mvc.Results._
import play.api.mvc._
import repositories.SessionRepo
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.voa.propertylinking.errorhandler.CustomErrorHandler

import scala.concurrent.Future

case class LinkingSessionRequest[A](
                                     ses: LinkingSession,
                                     organisationId: Long,
                                     individualAccount: DetailedIndividualAccount,
                                     groupAccount: GroupAccount,
                                     request: Request[A]
                                   ) extends WrappedRequest[A](request) {
  def sessionId: String = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session)).sessionId.map(_.value).getOrElse(throw NoSessionId)
}

case object NoSessionId extends Exception

class WithLinkingSession @Inject()(
                                    errorHandler: CustomErrorHandler,
                                    @Named("propertyLinkingSession") val sessionRepository: SessionRepo
                                  ) extends ActionRefiner[BasicAuthenticatedRequest, LinkingSessionRequest] {

  implicit def hc(implicit request: BasicAuthenticatedRequest[_]): HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

  override protected def refine[A](request: BasicAuthenticatedRequest[A]): Future[Either[Result, LinkingSessionRequest[A]]] = {
      sessionRepository.get[LinkingSession](implicitly[Reads[LinkingSession]], hc(request)).map {
        case Some(s)  => Right(LinkingSessionRequest(s, request.organisationAccount.id, request.individualAccount, request.organisationAccount, request))
        case None     => Left(NotFound(errorHandler.notFoundTemplate(request)))
      }
  }
}