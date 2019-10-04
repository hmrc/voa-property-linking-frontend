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
import javax.inject.Inject
import models.registration.UserDetails
import play.api.Logger
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter.fromHeadersAndSession

import scala.concurrent.{ExecutionContext, Future}

class GgAuthenticatedAction @Inject()(
                                       provider: GovernmentGatewayProvider,
                                       override val authConnector: AuthConnector
                                     )(implicit executionContext: ExecutionContext)
  extends ActionBuilder[RequestWithUserDetails] with AuthorisedFunctions {

  val logger = Logger(this.getClass.getName)

  override def invokeBlock[A](request: Request[A], block: RequestWithUserDetails[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier = fromHeadersAndSession(request.headers, Some(request.session))
    logger.debug("the request called invoke block")
    success(block)(request, hc)
  }

  def success[A](body: RequestWithUserDetails[A] => Future[Result])
                (implicit request: Request[A], hc: HeaderCarrier): Future[Result] = {
    def handleError: PartialFunction[Throwable, Future[Result]] = {
      case _: NoActiveSession =>
        provider.redirectToLogin
      case otherException: Throwable =>
        Logger.debug(s"Exception thrown on authorisation with message:", otherException)
        throw otherException
    }

    val retrieval = name and email and postCode and groupIdentifier and externalId and affinityGroup and credentialRole
    authorised(AuthProviders(GovernmentGateway)).retrieve(retrieval) {
      case name ~ optEmail ~ optPostCode ~ Some(groupIdentifier) ~ Some(externalId) ~ Some(affinityGroup) ~ Some(role) =>
        body(new RequestWithUserDetails(UserDetails.fromRetrieval(name, optEmail, optPostCode, groupIdentifier, externalId, affinityGroup, role), request))
    }.recoverWith(handleError)
  }

  def noVoaRecord: Future[Result] =
    Future.successful(Redirect(controllers.registration.routes.RegistrationController.show()))

}
