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

package actions.registration

import actions.registration.requests.{RegistrationSessionRequest, RequestWithUserDetails}
import actions.requests.BasicAuthenticatedRequest
import javax.inject.{Inject, Named}
import models.registration.RegistrationSession
import play.api.libs.json.Reads
import play.api.mvc.Results._
import play.api.mvc._
import repositories.SessionRepo
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import scala.concurrent.{ExecutionContext, Future}

class WithRegistrationSessionRefiner @Inject()(
                                                errorHandler: CustomErrorHandler,
                                                @Named("registrationDetails") val sessionRepository: SessionRepo
                                              )(implicit override val executionContext: ExecutionContext)
  extends ActionRefiner[RequestWithUserDetails, RegistrationSessionRequest] {

  implicit def hc(implicit request: RequestWithUserDetails[_]): HeaderCarrier =
    HeaderCarrierConverter.fromRequestAndSession(request, request.session)

  override protected def refine[A](
                                    request: RequestWithUserDetails[A]): Future[Either[Result, RegistrationSessionRequest[A]]] =
    sessionRepository.get[RegistrationSession](implicitly[Reads[RegistrationSession]], hc(request)).map {
      case Some(s) =>
        Right(RegistrationSessionRequest(sessionData = s, userDetaails = request.userDetails, request = request))
      case None => Left(NotFound(errorHandler.notFoundTemplate(request)))
    }
}