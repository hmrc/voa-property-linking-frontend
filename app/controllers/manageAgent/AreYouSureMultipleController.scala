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

package controllers.manageAgent

import actions.AuthenticatedAction
import com.google.inject.Singleton
import config.ApplicationConfig
import controllers.PropertyLinkingController
import models.propertyrepresentation._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.ManageAgentSessionRepository
import services.propertylinking.PropertyLinkingService
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AreYouSureMultipleController @Inject() (
      areYouSureMultipleView: views.html.manageAgent.areYouSureMultipleYears,
      manageAgentSessionRepository: ManageAgentSessionRepository,
      authenticated: AuthenticatedAction,
      propertyLinkingService: PropertyLinkingService
)(implicit
      executionContext: ExecutionContext,
      override val messagesApi: MessagesApi,
      override val controllerComponents: MessagesControllerComponents,
      val config: ApplicationConfig,
      val errorHandler: CustomErrorHandler
) extends PropertyLinkingController {

  def show: Action[AnyContent] =
    authenticated.async { implicit request =>
      manageAgentSessionRepository.get[AgentSummary].map {
        case Some(AgentSummary(_, representativeCode, agentName, _, _, _, Some(proposedListYears))) =>
          Ok(
            areYouSureMultipleView(
              agentName = agentName,
              backLink = getBackLink,
              agentCode = representativeCode,
              listYears = proposedListYears
            )
          )
        case _ => NotFound(errorHandler.notFoundErrorTemplate)
      }
    }

  def submitRatingListYears: Action[AnyContent] =
    authenticated.async { implicit request =>
      manageAgentSessionRepository.get[AgentSummary].flatMap {
        case Some(agentSummary) =>
          manageAgentSessionRepository.saveOrUpdate[AgentSummary](
            agentSummary
              .copy(proposedListYears = None)
          )
          agentSummary.proposedListYears match {
            case Some(proposedListYears) =>
              propertyLinkingService.appointAndOrRevokeListYears(agentSummary, proposedListYears.toList)
            case _ => Future.successful(NotFound(errorHandler.notFoundErrorTemplate))
          }
        case _ => Future.successful(NotFound(errorHandler.notFoundErrorTemplate))
      }
    }

  def getBackLink: String = if (config.agentJourney2026)
    controllers.manageAgent.routes.WhichRatingListController.showRevalEnabled.url
  else
    controllers.manageAgent.routes.ChooseRatingListController.show.url
}
