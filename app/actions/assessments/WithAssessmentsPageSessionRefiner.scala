/*
 * Copyright 2021 HM Revenue & Customs
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

package actions.assessments

import actions.assessments.request.AssessmentsPageSessionRequest
import actions.requests.BasicAuthenticatedRequest
import javax.inject.{Inject, Named}
import models.assessments.AssessmentsPageSession
import play.api.libs.json.Reads
import play.api.mvc.Results._
import play.api.mvc._
import repositories.SessionRepo
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import scala.concurrent.{ExecutionContext, Future}

class WithAssessmentsPageSessionRefiner @Inject()(
      errorHandler: CustomErrorHandler,
      @Named("assessmentPage") val sessionRepository: SessionRepo
)(implicit override val executionContext: ExecutionContext)
    extends ActionRefiner[BasicAuthenticatedRequest, AssessmentsPageSessionRequest] {

  implicit def hc(implicit request: BasicAuthenticatedRequest[_]): HeaderCarrier =
    HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

  override protected def refine[A](
        request: BasicAuthenticatedRequest[A]): Future[Either[Result, AssessmentsPageSessionRequest[A]]] =
    sessionRepository.get[AssessmentsPageSession](implicitly[Reads[AssessmentsPageSession]], hc(request)).map {
      case Some(s) =>
        Right(AssessmentsPageSessionRequest(s, request.individualAccount, request.organisationAccount, request))
      case None => Left(NotFound(errorHandler.notFoundTemplate(request)))
    }
}
