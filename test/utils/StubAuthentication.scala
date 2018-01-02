/*
 * Copyright 2018 HM Revenue & Customs
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

import actions.AuthenticatedAction
import connectors.{AuthorisationResult, BusinessRatesAuthorisation, InvalidGGSession}

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

object StubAuthentication extends AuthenticatedAction(null, StubBusinessRatesAuthorisation) {
  def stubAuthenticationResult(result: AuthorisationResult) = {
    StubBusinessRatesAuthorisation.authorisationResult = result
  }

  def reset() = {
    StubBusinessRatesAuthorisation.authorisationResult = InvalidGGSession
  }

}

object StubBusinessRatesAuthorisation extends BusinessRatesAuthorisation(StubServicesConfig, StubHttp) {
  var authorisationResult: AuthorisationResult = InvalidGGSession

  override def authenticate(implicit hc: HeaderCarrier) = Future.successful(authorisationResult)
  override def authorise(authorisationId: Long, assessmentRef: Long)(implicit hc: HeaderCarrier): Future[AuthorisationResult] = Future.successful(authorisationResult)
  override def authorise(authorisationId: Long)(implicit hc: HeaderCarrier): Future[AuthorisationResult] = Future.successful(authorisationResult)
}
