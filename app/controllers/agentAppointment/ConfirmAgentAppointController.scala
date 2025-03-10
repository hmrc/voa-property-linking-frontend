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

package controllers.agentAppointment

import actions.AuthenticatedAction
import actions.agentrelationship.WithAppointAgentSessionRefiner
import businessrates.authorisation.config.FeatureSwitch
import config.ApplicationConfig
import controllers.PropertyLinkingController
import models.propertyrepresentation._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepo
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import javax.inject.{Inject, Named}
import scala.concurrent.ExecutionContext

class ConfirmAgentAppointController @Inject() (
      val errorHandler: CustomErrorHandler,
      authenticated: AuthenticatedAction,
      withAppointAgentSession: WithAppointAgentSessionRefiner,
      @Named("appointNewAgentSession") val appointNewAgentSession: SessionRepo,
      confirmationView: views.html.propertyrepresentation.appoint.confirmation,
      featureSwitch: FeatureSwitch
)(implicit
      override val messagesApi: MessagesApi,
      override val controllerComponents: MessagesControllerComponents,
      val config: ApplicationConfig,
      executionContext: ExecutionContext
) extends PropertyLinkingController {

  def onPageLoad(): Action[AnyContent] =
    authenticated.andThen(withAppointAgentSession) { implicit request =>
      request.sessionData match {
        case data: ManagingProperty =>
          val key =
            if (featureSwitch.isAgentListYearsEnabled == false)
              if (data.singleProperty)
                Some("propertyRepresentation.confirmation.yourProperty")
              else
                data.managingPropertyChoice match {
                  case All.name            => Some("propertyRepresentation.confirmation.allProperties")
                  case ChooseFromList.name => Some("propertyRepresentation.confirmation.selectedProperties")
                  case _                   => None
                }
            else None

          val secondKey: Option[String] =
            if (!featureSwitch.isAgentListYearsEnabled)
              Some("propertyRepresentation.confirmation.thisAgentCan.list.2")
            else
              (data.bothRatingLists, data.specificRatingList) match {
                case (Some(true), _) => Some("propertyRepresentation.confirmation.secondBulletPoint.both_years")
                case (Some(false), Some("2023")) => Some("propertyRepresentation.confirmation.secondBulletPoint.2023")
                case (Some(false), Some("2017")) => Some("propertyRepresentation.confirmation.secondBulletPoint.2017")
                case _                           => None
              }

          Ok(
            confirmationView(
              agentName = request.agentDetails.name,
              assignedToMessageKey = key,
              secondBulletPoint = secondKey
            )
          )
      }
    }
}
