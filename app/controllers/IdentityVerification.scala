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
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepo
import services.iv.IdentityVerificationService
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

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

  val logger = Logger(this.getClass.getName)

  val startIv: Action[AnyContent] = ggAction.async { implicit request =>
    if (config.ivEnabled) {
      for {
        userDetails <- personalDetailsSessionRepo.get[AdminUser]
        _ = logger.info(s"**** DPP: $userDetails")
        links <- identityVerificationService.start(
                  userDetails.getOrElse(throw new Exception("details not found")).toIvDetails)
      } yield {
        val url = links.getLink(config.ivBaseUrl)
        logger.info(s"**** DPP: $url")
        Redirect(url)
      }
    } else {
      Future.successful(Redirect(routes.IdentityVerification.success(Some(java.util.UUID.randomUUID().toString))))
    }
  }

  def fail(journeyId: Option[String]): Action[AnyContent] = ggAction.async { implicit request =>
    journeyId.fold(Future.successful(Unauthorized(errorHandler.internalServerErrorTemplate))) { id =>
      identityVerificationConnector.journeyStatus(id).map {
        case IvResult.IvSuccess =>
          Redirect(controllers.routes.IdentityVerification.success(journeyId))
        case ivFailureReason: IvResult.IvFailure =>
          Ok(views.html.identityVerification.failed(ivFailureReason))
      }
    }

  }

  //Not sure what this is used for.
  val restoreSession: Action[AnyContent] = Action { implicit request =>
    Redirect(routes.IdentityVerification.success(Some(java.util.UUID.randomUUID().toString))).addingToSession(
      SessionKeys.authToken -> request.session.get("bearerToken").getOrElse(""),
      SessionKeys.sessionId -> request.session.get("oldSessionId").getOrElse("")
    )
  }

  def success(journeyId: Option[String]): Action[AnyContent] = ggAction.async { implicit request =>
    logger.info(s"**** DPP: SUCCESS: $journeyId")
    if (config.ivEnabled) {
      journeyId.fold(Future.successful(Unauthorized(errorHandler.internalServerErrorTemplate))) { id =>
        identityVerificationConnector.verifySuccess(id).flatMap {
          case true =>
            identityVerificationService.continue(id, request.userDetails).map {
              case Some(obj) => identityVerificationService.someCase(obj)
              case None      => identityVerificationService.noneCase
            }
          case false => Future.successful(Unauthorized(errorHandler.internalServerErrorTemplate))
        }
      }
    } else {
      identityVerificationService.continue("1", request.userDetails).map {
        case Some(obj) => identityVerificationService.someCase(obj)
        case None      => identityVerificationService.noneCase
      }
    }
  }
}
