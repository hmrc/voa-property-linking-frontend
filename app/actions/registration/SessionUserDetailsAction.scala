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

import actions.registration.requests.{RequestWithSessionPersonDetails, RequestWithUserDetails}

import javax.inject.Inject
import models.registration._
import play.api.mvc._
import repositories.PersonalDetailsSessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

class SessionUserDetailsAction @Inject()(
      val personalDetailsSessionRepo: PersonalDetailsSessionRepository
)(implicit override val executionContext: ExecutionContext)
    extends ActionTransformer[RequestWithUserDetails, RequestWithSessionPersonDetails] {

  override protected def transform[A](
        request: RequestWithUserDetails[A]): Future[RequestWithSessionPersonDetails[A]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    personalDetailsSessionRepo.get[AdminUser].map {
      case None => new RequestWithSessionPersonDetails[A](None, request)
      case Some(details: IndividualUserAccountDetails) =>
        new RequestWithSessionPersonDetails[A](Some(details), request)
      case Some(details: AdminInExistingOrganisationAccountDetails) =>
        new RequestWithSessionPersonDetails[A](Some(details), request)
      case Some(details: AdminOrganisationAccountDetails) =>
        new RequestWithSessionPersonDetails[A](Some(details), request)
    }
  }
}
