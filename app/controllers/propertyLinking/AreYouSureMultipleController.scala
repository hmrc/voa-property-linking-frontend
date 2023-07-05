/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.propertyLinking

import actions.AuthenticatedAction
import businessrates.authorisation.config.FeatureSwitch
import com.google.inject.Singleton
import config.ApplicationConfig
import controllers.PropertyLinkingController
import models.propertyrepresentation.{AgentAppointmentChangeRequest, AgentSummary}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.ManageAgentSessionRepository
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import connectors.propertyLinking.PropertyLinkConnector

@Singleton
class AreYouSureMultipleController @Inject()(
      areYouSureMultipleView: views.html.propertyLinking.areYouSureMultipleYears,
      manageAgentSessionRepository: ManageAgentSessionRepository,
      authenticated: AuthenticatedAction,
      featureSwitch: FeatureSwitch,
      propertyLinkConnector: PropertyLinkConnector
)(
      implicit executionContext: ExecutionContext,
      override val messagesApi: MessagesApi,
      override val controllerComponents: MessagesControllerComponents,
      val config: ApplicationConfig,
      val errorHandler: CustomErrorHandler
) extends PropertyLinkingController {

  def show: Action[AnyContent] = authenticated.async { implicit request =>
    if (featureSwitch.isAgentListYearsEnabled) {
      manageAgentSessionRepository.get[AgentSummary].map {
        case Some(AgentSummary(_, representativeCode, agentName, _, _, _)) =>
          Ok(areYouSureMultipleView(agentName = agentName, backLink = getBackLink, agentCode = representativeCode))
        case _ => NotFound(errorHandler.notFoundErrorTemplate)
      }
    } else Future.successful(NotFound(errorHandler.notFoundErrorTemplate))
  }

  def submitRatingListYears: Action[AnyContent] = authenticated.async { implicit request =>
    if (featureSwitch.isAgentListYearsEnabled) {
      manageAgentSessionRepository.get[AgentSummary].map {
        case Some(agentSummary) =>
          propertyLinkConnector.agentAppointmentChange(
            AgentAppointmentChangeRequest(
              agentRepresentativeCode = agentSummary.representativeCode,
              scope = "APPOINT",
              action = "LIST_YEAR",
              propertyLinkIds = None,
              listYears = Some(List("2017", "2023"))
            ))

          Redirect(controllers.propertyLinking.routes.RatingListConfirmedController.show.url)
        case _ => NotFound(errorHandler.notFoundErrorTemplate)
      }
    } else Future.successful(NotFound(errorHandler.notFoundErrorTemplate))
  }
  def getBackLink: String = controllers.propertyLinking.routes.ChooseRatingListController.show.url
}
