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

package controllers.agentAppointment

import actions.AuthenticatedAction
import actions.agentrelationship.WithAppointAgentSessionRefiner
import businessrates.authorisation.config.FeatureSwitch
import com.google.inject.Singleton
import config.ApplicationConfig
import controllers.PropertyLinkingController
import form.Mappings.mandatoryBoolean
import models.RatingListYears
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
@Singleton
class SelectRatingListController @Inject()(
      authenticated: AuthenticatedAction,
      featureSwitch: FeatureSwitch,
      withAppointAgentSession: WithAppointAgentSessionRefiner,
      selectRatingListView: views.html.appointAgent.selectRatingList)(
      implicit executionContext: ExecutionContext,
      override val messagesApi: MessagesApi,
      override val controllerComponents: MessagesControllerComponents,
      val config: ApplicationConfig,
      val errorHandler: CustomErrorHandler
) extends PropertyLinkingController {

  def show: Action[AnyContent] = authenticated.async { implicit request =>
    if (featureSwitch.isAgentListYearsEnabled) {
      Future.successful(
        Ok(
          selectRatingListView(
            ratingListYears,
            backLink = getBackLink
          )))
    } else Future.successful(NotFound(errorHandler.notFoundErrorTemplate))
  }

  def submitRatingListYear: Action[AnyContent] = TODO

  def getBackLink = controllers.agentAppointment.routes.RatingListOptionsController.show.url

  def ratingListYears: Form[RatingListYears] =
    Form(
      mapping(
        "multipleListYears" -> mandatoryBoolean,
      )(RatingListYears.apply)(RatingListYears.unapply))

}
