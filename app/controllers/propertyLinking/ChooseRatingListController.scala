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
import actions.propertylinking.WithLinkingSession
import com.google.inject.Singleton
import config.ApplicationConfig
import controllers.PropertyLinkingController
import form.Mappings.{dmyDate, mandatoryBoolean}
import models.{PropertyOccupancy, RatingListYears}
import play.api.data.Form
import play.api.data.Forms.{boolean, mapping, single}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfFalse
import views.helpers.Errors

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ChooseRatingListController @Inject()(
                              chooseListView: views.html.propertyLinking.chooseRatingList,
                            )(
                                           implicit executionContext: ExecutionContext,
                                           override val messagesApi: MessagesApi,
                                           override val controllerComponents: MessagesControllerComponents,
                                           val config: ApplicationConfig,
                                           authenticatedAction: AuthenticatedAction,
                                           withLinkingSession: WithLinkingSession,
                                         ) extends PropertyLinkingController {

  def show: Action[AnyContent] = Action { implicit request =>
    Ok(chooseListView(ratingListYears, currentRatingList = "2023", backLink = getBackLink, agentName = getAgentName))
  }

  def submitRatingListYears: Action[AnyContent] = Action { implicit request =>
    ratingListYears
        .bindFromRequest()
        .fold(
          errors =>
            BadRequest(chooseListView(form = errors, currentRatingList =  "2023", backLink = getBackLink, agentName = getAgentName)),
          formData =>
            if (formData.multipleListYears)
              Redirect(controllers.propertyLinking.routes.AreYouSureMultipleController.show.url)
            else Redirect(controllers.propertyLinking.routes.WhichRatingListController.show.url)
        )
    }

  val getBackLink: String = routes.DeclarationController.show.url
  val getAgentName: String = "Joeys Agent"

  def ratingListYears: Form[RatingListYears] =
    Form(
      mapping(
        "multipleListYears" -> mandatoryBoolean,
      )(RatingListYears.apply)(RatingListYears.unapply))

  override def errorHandler: FrontendErrorHandler = ???
}