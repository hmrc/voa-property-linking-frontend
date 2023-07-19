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
import connectors.propertyLinking.PropertyLinkConnector
import controllers.PropertyLinkingController
import models.propertyrepresentation.{AgentAppointmentChangeRequest, AgentAppointmentChangesResponse, AgentSummary, AppointmentAction, AppointmentScope}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.ManageAgentSessionRepository
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AreYouSureController @Inject()(
      areYouSureView: views.html.propertyLinking.areYouSure,
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

  def show(chosenListYear: String): Action[AnyContent] = authenticated.async { implicit request =>
    if (featureSwitch.isAgentListYearsEnabled) {
      if (chosenListYear == "2017" || chosenListYear == "2023") {
        manageAgentSessionRepository.get[AgentSummary].map {
          case Some(AgentSummary(_, representativeCode, agentName, _, _, _)) =>
            Ok(
              areYouSureView(
                agentName = agentName,
                chosenListYear = chosenListYear,
                backLink = getBackLink,
                agentCode = representativeCode))
          case _ => NotFound(errorHandler.notFoundErrorTemplate)
        }
      } else Future.successful(NotFound(errorHandler.notFoundErrorTemplate))
    } else Future.successful(NotFound(errorHandler.notFoundErrorTemplate))
  }

  def submitRatingListYears(chosenListYear: String): Action[AnyContent] = authenticated.async { implicit request =>
    if (featureSwitch.isAgentListYearsEnabled) {
      manageAgentSessionRepository.get[AgentSummary].flatMap {
        case Some(agentSummary) =>
          //This should never happen, shouldn't fall into this flow if the feature switch is enabled
          val currentListYears = agentSummary.listYears.getOrElse(throw new Exception("No list years"))
          for {
            _ <- if (!currentListYears.contains(chosenListYear)) {
                  propertyLinkConnector.agentAppointmentChange(
                    AgentAppointmentChangeRequest(
                      agentRepresentativeCode = agentSummary.representativeCode,
                      scope = AppointmentScope.LIST_YEAR,
                      action = AppointmentAction.APPOINT,
                      propertyLinkIds = None,
                      listYears = Some(List(chosenListYear))
                    )
                  )
                } else Future.successful(AgentAppointmentChangesResponse("No appointment needed"))
            listYearsToRevoke = currentListYears.filterNot(_ == chosenListYear)
            _ <- if (listYearsToRevoke.nonEmpty) {
                  propertyLinkConnector.agentAppointmentChange(
                    AgentAppointmentChangeRequest(
                      agentRepresentativeCode = agentSummary.representativeCode,
                      scope = AppointmentScope.LIST_YEAR,
                      action = AppointmentAction.REVOKE,
                      propertyLinkIds = None,
                      listYears = Some(listYearsToRevoke.toList)
                    )
                  )
                } else Future.successful(AgentAppointmentChangesResponse("No revoke needed"))
            _ <- manageAgentSessionRepository.saveOrUpdate[AgentSummary](
                  agentSummary.copy(listYears = Some(List(chosenListYear)))
                )
          } yield Redirect(controllers.propertyLinking.routes.RatingListConfirmedController.show.url)
        case _ => Future.successful(NotFound(errorHandler.notFoundErrorTemplate))
      }
    } else Future.successful(NotFound(errorHandler.notFoundErrorTemplate))
  }

  def getBackLink: String = controllers.propertyLinking.routes.WhichRatingListController.show.url
}
