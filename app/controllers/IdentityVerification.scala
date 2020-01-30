/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers

import actions.registration.GgAuthenticatedAction
import config.ApplicationConfig
import javax.inject.{Inject, Named}
import models.identityVerificationProxy.IvResult
import models.registration.AdminUser
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepo
import services.iv.IdentityVerificationService
import uk.gov.hmrc.http.SessionKeys
import uk.gov.voa.propertylinking.errorhandler.CustomErrorHandler

import scala.concurrent.{ExecutionContext, Future}

class IdentityVerification @Inject()(
      override val errorHandler: CustomErrorHandler,
      ggAction: GgAuthenticatedAction,
      identityVerificationConnector: connectors.IdentityVerificationConnector,
      identityVerificationService: IdentityVerificationService,
      @Named("personSession") val personalDetailsSessionRepo: SessionRepo
)(
      implicit val executionContext: ExecutionContext,
      override val messagesApi: MessagesApi,
      override val controllerComponents: MessagesControllerComponents,
      val config: ApplicationConfig
) extends PropertyLinkingController {

  val startIv: Action[AnyContent] = ggAction.async { implicit request =>
    if (config.ivEnabled) {
      for {
        userDetails <- personalDetailsSessionRepo.get[AdminUser]
        links <- identityVerificationService.start(
                  userDetails.getOrElse(throw new Exception("details not found")).toIvDetails)
      } yield Redirect(links.getLink(config.ivBaseUrl))
    } else {
      Future.successful(
        Redirect(routes.IdentityVerification.success())
          .addingToSession("journeyId" -> java.util.UUID.randomUUID().toString))
    }
  }

  val fail: Action[AnyContent] = ggAction.async { implicit request =>
    request.getQueryString("journeyId").fold(Future.successful(Unauthorized("Unauthorised"))) { journeyId =>
      identityVerificationConnector.journeyStatus(journeyId).map {
        case IvResult.IvSuccess =>
          Redirect(controllers.routes.IdentityVerification.success())
        case ivFailureReason: IvResult.IvFailure =>
          Ok(views.html.identityVerification.failed(ivFailureReason))
      }
    }
  }

  val restoreSession: Action[AnyContent] = Action { implicit request =>
    Redirect(routes.IdentityVerification.success()).addingToSession(
      SessionKeys.authToken -> request.session.get("bearerToken").getOrElse(""),
      SessionKeys.sessionId -> request.session.get("oldSessionId").getOrElse("")
    )
  }

  val success: Action[AnyContent] = ggAction.async { implicit request =>
    request.session.get("journeyId").fold(Future.successful(Unauthorized("Unauthorised"))) { journeyId =>
      identityVerificationConnector.verifySuccess(journeyId).flatMap {
        case true =>
          identityVerificationService.continue(journeyId, request.userDetails).map {
            case Some(obj) => identityVerificationService.someCase(obj)
            case None      => identityVerificationService.noneCase
          }
        case false => Future.successful(Unauthorized("Unauthorised"))
      }
    }
  }
}
