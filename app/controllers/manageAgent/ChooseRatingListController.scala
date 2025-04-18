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
import form.Mappings.mandatoryBoolean
import models.RatingListYears
import models.propertyrepresentation.AgentSummary
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.ManageAgentSessionRepository
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ChooseRatingListController @Inject() (
      chooseListView: views.html.manageAgent.chooseRatingList,
      manageAgentSessionRepository: ManageAgentSessionRepository,
      authenticated: AuthenticatedAction
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
        case Some(AgentSummary(_, _, agentName, _, _, Some(listYears), _)) =>
          Ok(
            chooseListView(
              ratingListYears,
              currentRatingList = listYears.toList,
              backLink = getBackLink,
              agentName = agentName
            )
          )
        case _ => NotFound(errorHandler.notFoundErrorTemplate)
      }
    }

  def submitRatingListYears: Action[AnyContent] =
    authenticated.async { implicit request =>
      manageAgentSessionRepository.get[AgentSummary].map {
        case Some(agentSummary @ AgentSummary(_, _, agentName, _, _, Some(listYears), _)) =>
          ratingListYears
            .bindFromRequest()
            .fold(
              errors =>
                BadRequest(
                  chooseListView(
                    form = errors,
                    currentRatingList = listYears.toList,
                    backLink = getBackLink,
                    agentName = agentName
                  )
                ),
              formData =>
                if (formData.multipleListYears) {
                  manageAgentSessionRepository.saveOrUpdate[AgentSummary](
                    agentSummary
                      .copy(proposedListYears = Some(Seq("2017", "2023")))
                  )
                  Redirect(controllers.manageAgent.routes.AreYouSureMultipleController.show.url)
                } else Redirect(controllers.manageAgent.routes.WhichRatingListController.show.url)
            )
        case _ => NotFound(errorHandler.notFoundErrorTemplate)
      }
    }

  def getBackLink: String = controllers.agent.routes.ManageAgentController.showManageAgent.url

  def ratingListYears: Form[RatingListYears] =
    Form(
      mapping(
        "multipleListYears" -> mandatoryBoolean
      )(RatingListYears.apply)(RatingListYears.unapply)
    )

}
