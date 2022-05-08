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

package actions.propertylinking

import actions.propertylinking.requests.LinkingSessionRequest
import actions.requests.BasicAuthenticatedRequest
import models.LinkingSession
import play.api.libs.json.Reads
import play.api.mvc.Results._
import play.api.mvc._
import repositories.SessionRepo
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class WithLinkingSession @Inject()(
      errorHandler: CustomErrorHandler,
      @Named("propertyLinkingSession") val sessionRepository: SessionRepo
)(implicit override val executionContext: ExecutionContext)
    extends ActionRefiner[BasicAuthenticatedRequest, LinkingSessionRequest] {

  implicit def hc(implicit request: BasicAuthenticatedRequest[_]): HeaderCarrier =
    HeaderCarrierConverter.fromRequestAndSession(request, request.session)

  override protected def refine[A](
        request: BasicAuthenticatedRequest[A]): Future[Either[Result, LinkingSessionRequest[A]]] =
    sessionRepository.get[LinkingSession](implicitly[Reads[LinkingSession]], hc(request)).map {
      case Some(s) =>
        Right(
          LinkingSessionRequest(
            ses = s,
            organisationId = request.organisationAccount.id,
            individualAccount = request.individualAccount,
            organisationAccount = request.organisationAccount,
            request = request
          ))
      case None => Left(NotFound(errorHandler.notFoundTemplate(request)))
    }
}
