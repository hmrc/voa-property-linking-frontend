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

package actions

import auth.GovernmentGatewayProvider
import config.ApplicationConfig
import connectors.VPLAuthConnector
import javax.inject.Inject
import models.Accounts
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Request, Result}
import services.email.EmailService
import services.{EnrolmentResult, EnrolmentService, Failure, Success}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

sealed trait Auth {
  def success(
               accounts: Accounts,
               body: BasicAuthenticatedRequest[AnyContent] => Future[Result]
             )(implicit request: Request[AnyContent], hc: HeaderCarrier, ec: ExecutionContext): Future[Result]

  def noVoaRecord: Future[Result]
}

class VoaAuth @Inject()(provider: GovernmentGatewayProvider,
                        enrolmentService: EnrolmentService,
                        emailService: EmailService,
                        val authConnector: AuthConnector,
                        auth: VPLAuthConnector
                       )(implicit val messagesApi: MessagesApi, config: ApplicationConfig) extends AuthorisedFunctions with Auth with I18nSupport {

  override def success(
                        accounts: Accounts,
                        body: BasicAuthenticatedRequest[AnyContent] => Future[Result])
                      (implicit request: Request[AnyContent], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
    def handleError: PartialFunction[Throwable, Future[Result]] = {
      case _: NoActiveSession =>
        provider.redirectToLogin
      case otherException =>
        Logger.debug(s"Exception thrown on authorisation with message: ${otherException.getMessage}")
        throw otherException
    }

    val retrieval = allEnrolments and credentialRole

    authorised(AuthProviders(AuthProvider.GovernmentGateway)).retrieve(retrieval) {
      case enrolments ~ role => {
        val action = body(BasicAuthenticatedRequest(accounts.organisation, accounts.person, request))
        isAssistant(role) match {
          case false => {
            if (config.stubEnrolment) {
              Logger.info("Enrolment stubbed")
            } else {
              enrolments.getEnrolment("HMRC-VOA-CCA") match {
                case Some(enrolment) =>
                case None => enrolmentService
                  .enrol(accounts.person.individualId, accounts.organisation.addressId).flatMap(existingUserEnrolmentResult(accounts, body))
              }
            }
            action
          }
          case true => action
        }
      }
    }.recoverWith(handleError)
  }

  private def isAssistant(credentialRole: Option[CredentialRole])(implicit request: Request[_]): Boolean = {
    credentialRole match {
      case Some(Assistant) => true
      case _ => false
    }
  }

  override def noVoaRecord: Future[Result] =
    Future.successful(Redirect(controllers.registration.routes.RegistrationController.show()))

  private def existingUserEnrolmentResult(
                                           accounts: Accounts,
                                           body: BasicAuthenticatedRequest[AnyContent] => Future[Result]
                                         )(implicit request: Request[AnyContent], hc: HeaderCarrier, ec: ExecutionContext): PartialFunction[EnrolmentResult, Future[Result]] = {
    case Success =>
      Logger.info("Existing VOA user successfully enrolled")
      body(BasicAuthenticatedRequest(accounts.organisation, accounts.person, request))
    case Failure =>
      Logger.warn("Failed to enrol existing VOA user")
      body(BasicAuthenticatedRequest(accounts.organisation, accounts.person, request))
  }
}