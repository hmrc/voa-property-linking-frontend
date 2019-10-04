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

package controllers

import auth.VoaAction
import config.ApplicationConfig
import connectors._
import javax.inject.{Inject, Named}
import models.registration.AdminUser
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import repositories.SessionRepo
import services.iv.IdentityVerificationService
import uk.gov.hmrc.http.SessionKeys
import uk.gov.voa.propertylinking.errorhandler.CustomErrorHandler

import scala.concurrent.{ExecutionContext, Future}

class IdentityVerification @Inject()(
                                      val errorHandler: CustomErrorHandler,
                                      ggAction: VoaAction,
                                     identityVerification: connectors.IdentityVerification,
                                     addresses: Addresses,
                                     individuals: IndividualAccounts,
                                     identityVerificationService: IdentityVerificationService,
                                     groups: GroupAccounts,
                                     auth: VPLAuthConnector,
                                     @Named("personSession") val personalDetailsSessionRepo: SessionRepo)
                                    (implicit executionContext: ExecutionContext, val messagesApi: MessagesApi, val config: ApplicationConfig)
  extends PropertyLinkingController {

  def startIv: Action[AnyContent] = ggAction.async(false) { _ =>
    implicit request =>
      if (config.ivEnabled) {
        for {
          userDetails <- personalDetailsSessionRepo.get[AdminUser]
          link <- identityVerificationService.start(userDetails.getOrElse(throw new Exception("details not found")).toIvDetails)
        } yield Redirect(link.getLink(config.ivBaseUrl))
      } else {
        Future.successful(Redirect(routes.IdentityVerification.success()).addingToSession("journeyId" -> java.util.UUID.randomUUID().toString))
      }
  }

  def fail: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.identityVerification.failed())
  }

  def restoreSession: Action[AnyContent] = Action { implicit request =>
    Redirect(routes.IdentityVerification.success()).addingToSession(
      SessionKeys.authToken -> request.session.get("bearerToken").getOrElse(""),
      SessionKeys.sessionId -> request.session.get("oldSessionId").getOrElse("")
    )
  }

  def success: Action[AnyContent] = ggAction.async(false) { implicit ctx =>
    implicit request =>
      request.session.get("journeyId").fold(Future.successful(Unauthorized("Unauthorised"))) { journeyId =>
        identityVerification.verifySuccess(journeyId) flatMap {
          case true =>
            identityVerificationService.continue(journeyId)(ctx, hc, executionContext).map {
              case Some(obj) => identityVerificationService.someCase(obj)
              case None => identityVerificationService.noneCase
            }
          case false => Future.successful(Unauthorized("Unauthorised"))
        }
      }
  }
}
