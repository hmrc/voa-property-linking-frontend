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

package utils

import actions.{AuthenticatedAction, VoaAuth}
import connectors.{AuthorisationResult, BusinessRatesAuthorisation, InvalidGGSession}
import models.DetailedIndividualAccount
import services.email.EmailService
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.auth.core.{Assistant, AuthConnector, CredentialRole, Enrolments}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

object StubAuthentication extends AuthenticatedAction(null, StubBusinessRatesAuthorisation, StubAuth, null, null)(null, null) { //TODO mock these things
  def stubAuthenticationResult(result: AuthorisationResult) = {
    StubBusinessRatesAuthorisation.authorisationResult = result
  }

  def reset() = {
    StubBusinessRatesAuthorisation.authorisationResult = InvalidGGSession
  }

}

object StubEmailService extends EmailService(null) {

  override def sendNewRegistrationSuccess(to: String, detailedIndividualAccount: DetailedIndividualAccount)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Unit] = Future.successful(())

}

object StubAuthConnector extends AuthConnector {
  def success: ~[Enrolments, Option[CredentialRole]] = new ~(Enrolments(Set()), Some(Assistant))

  def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext): Future[A] =
    Future.successful(success.asInstanceOf[A])
}

object StubAuth extends VoaAuth(null, null, StubEmailService, StubAuthConnector, StubVplAuthConnector)(null, null)

object StubBusinessRatesAuthorisation extends BusinessRatesAuthorisation(StubServicesConfig, StubHttp) {
  var authorisationResult: AuthorisationResult = InvalidGGSession

  override def authenticate(implicit hc: HeaderCarrier) = Future.successful(authorisationResult)

  override def authorise(authorisationId: Long, assessmentRef: Long)(implicit hc: HeaderCarrier): Future[AuthorisationResult] = Future.successful(authorisationResult)

  override def authorise(authorisationId: Long)(implicit hc: HeaderCarrier): Future[AuthorisationResult] = Future.successful(authorisationResult)

  override def isAgentOwnProperty(authorisationId: Long)(implicit hc: HeaderCarrier): Future[Boolean] = Future.successful(true)
}
