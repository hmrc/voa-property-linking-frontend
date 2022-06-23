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

package actions.assessments

import actions.assessments.request.AssessmentsPageSessionRequest
import actions.requests.BasicAuthenticatedRequest

import javax.inject.{Inject, Named}
import models.assessments.{AssessmentsPageSession, PreviousPage}
import play.api.libs.json.Reads
import play.api.mvc._
import repositories.SessionRepo
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

class WithAssessmentsPageSessionRefiner @Inject()(
      @Named("assessmentPage") val sessionRepository: SessionRepo
)(implicit override val executionContext: ExecutionContext)
    extends ActionRefiner[BasicAuthenticatedRequest, AssessmentsPageSessionRequest] {

  implicit def hc(implicit request: BasicAuthenticatedRequest[_]): HeaderCarrier =
    HeaderCarrierConverter.fromRequestAndSession(request, request.session)

  override protected def refine[A](
        request: BasicAuthenticatedRequest[A]): Future[Either[Result, AssessmentsPageSessionRequest[A]]] =
    sessionRepository.get[AssessmentsPageSession](implicitly[Reads[AssessmentsPageSession]], hc(request)).map {
      case Some(s) => Right(AssessmentsPageSessionRequest(s, request))
      case None    => Right(AssessmentsPageSessionRequest(AssessmentsPageSession(PreviousPage.Dashboard), request))
    }
}
