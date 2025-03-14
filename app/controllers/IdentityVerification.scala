/*
 * Copyright 2024 HM Revenue & Customs
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
import models.identityVerificationProxy.IvResult
import models.registration._
import play.api.Logging
import play.api.i18n.MessagesApi
import play.api.mvc._
import repositories.SessionRepo
import services.iv.IdentityVerificationService
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class IdentityVerification @Inject() (
      override val errorHandler: CustomErrorHandler,
      ggAction: GgAuthenticatedAction,
      identityVerificationConnector: connectors.IdentityVerificationConnector,
      identityVerificationService: IdentityVerificationService,
      ivFailedView: views.html.ivFailed,
      @Named("personSession") val personalDetailsSessionRepo: SessionRepo
)(implicit
      val executionContext: ExecutionContext,
      override val messagesApi: MessagesApi,
      override val controllerComponents: MessagesControllerComponents,
      val config: ApplicationConfig
) extends PropertyLinkingController with Logging {

  val startIv: Action[AnyContent] = ggAction.async { implicit request =>
    if (config.ivEnabled)
      for {
        userDetails <- personalDetailsSessionRepo.get[AdminUser]
        link <-
          identityVerificationService.start(userDetails.getOrElse(throw new Exception("details not found")).toIvDetails)
      } yield Redirect(link.getLink(config.ivBaseUrl))
    else
      Future.successful(Redirect(routes.IdentityVerification.success(Some(java.util.UUID.randomUUID().toString))))
  }

  def upliftIv: Action[AnyContent] =
    ggAction.async { implicit request =>
      Future.successful(
        Redirect(
          config.identityVerificationUrl,
          Map(
            "origin"          -> Seq(config.appName),
            "confidenceLevel" -> Seq(ConfidenceLevel.L200.toString),
            "completionURL"   -> Seq(s"${config.upliftCompletionUrl}"),
            "failureURL"      -> Seq(s"${config.upliftFailureUrl}")
          )
        )
      )
    }

  def fail(journeyId: Option[String]): Action[AnyContent] =
    ggAction.async { implicit request =>
      journeyId.fold(errorHandler.internalServerErrorTemplate.map(html => Unauthorized(html))) { id =>
        identityVerificationConnector.journeyStatus(id).map {
          case IvResult.IvSuccess =>
            Redirect(controllers.routes.IdentityVerification.success(journeyId))
          case ivFailureReason: IvResult.IvFailure =>
            // TODO: Need to clear cached user answers on failure
            Ok(ivFailedView(ivFailureReason))
        }
      }
    }

  def success(journeyId: Option[String]): Action[AnyContent] =
    ggAction.async { implicit request =>
      journeyId.fold(errorHandler.internalServerErrorTemplate.map(html => Unauthorized(html))) { id =>
        identityVerificationConnector.verifySuccess(id).flatMap {
          case true =>
            identityVerificationService.continue(journeyId, request.userDetails).flatMap {
              case Some(RegistrationSuccess(personId)) =>
                if (config.newRegistrationJourneyEnabled)
                  Future.successful(Redirect(registration.routes.RegistrationController.confirmation(personId)))
                else Future.successful(Redirect(registration.routes.RegistrationController.success(personId)))
              case _ => errorHandler.internalServerErrorTemplate.map(html => InternalServerError(html))
            }
          case false => errorHandler.internalServerErrorTemplate.map(html => Unauthorized(html))
        }
      }
    }

  def upliftSuccess(journeyId: Option[String]): Action[AnyContent] =
    ggAction.async { implicit request =>
      journeyId.fold(errorHandler.internalServerErrorTemplate.map(html => Unauthorized(html))) { id =>
        identityVerificationConnector.verifySuccess(id).flatMap {
          case true => Future.successful(Redirect(registration.routes.RegistrationController.show))
          case _    =>
            // TODO: should this go to iv failure screen?
            // TODO: Need to clear cached user answers on failure
            errorHandler.internalServerErrorTemplate.map(html => Unauthorized(html))
        }
      }
    }

  def upliftFail(journeyId: Option[String]): Action[AnyContent] =
    ggAction.async { implicit request =>
      journeyId.fold(errorHandler.internalServerErrorTemplate.map(html => Unauthorized(html))) { id =>
        identityVerificationConnector.journeyStatus(id).map {
          case IvResult.IvSuccess =>
            Redirect(registration.routes.RegistrationController.show)
          case ivFailureReason: IvResult.IvFailure =>
            // TODO: Need to clear cached user answers on failure
            Ok(ivFailedView(ivFailureReason))
        }
      }
    }
}
