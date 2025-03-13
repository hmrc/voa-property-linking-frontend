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
import actions.agentrelationship.request.AppointAgentSessionRequest
import businessrates.authorisation.config.FeatureSwitch
import config.ApplicationConfig
import controllers.PropertyLinkingController
import models.propertyrepresentation.AgentAppointmentChangesRequest.submitAgentAppointmentRequest
import models.propertyrepresentation._
import play.api.Logging
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepo
import services.AgentRelationshipService
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class CheckYourAnswersController @Inject() (
      val errorHandler: CustomErrorHandler,
      authenticated: AuthenticatedAction,
      withAppointAgentSession: WithAppointAgentSessionRefiner,
      agentRelationshipService: AgentRelationshipService,
      @Named("appointNewAgentSession") val appointNewAgentSession: SessionRepo,
      @Named("appointAgentPropertiesSession") val appointAgentPropertiesSession: SessionRepo,
      checkYourAnswersView: views.html.propertyrepresentation.appoint.checkYourAnswers
)(implicit
      override val messagesApi: MessagesApi,
      override val controllerComponents: MessagesControllerComponents,
      executionContext: ExecutionContext,
      val config: ApplicationConfig
) extends PropertyLinkingController with Logging {

  def onPageLoad(): Action[AnyContent] =
    authenticated.andThen(withAppointAgentSession).async { implicit request =>
      PartialFunction
        .condOpt(request.sessionData) { case data: ManagingProperty =>
          Future.successful(Ok(checkYourAnswersView(getBackLinkFromSession, submitAgentAppointmentRequest, data)))
        }
        .getOrElse {
          logger.info("Failed to find ManagingProperty data in the session cache.")
          errorHandler.notFoundTemplate.map(html => NotFound(html))
        }
    }

  def onSubmit: Action[AnyContent] =
    authenticated.andThen(withAppointAgentSession).async { implicit request =>
      submitAgentAppointmentRequest
        .bindFromRequest()
        .fold(
          errors =>
            request.sessionData match {
              case data: ManagingProperty =>
                Future.successful(BadRequest(checkYourAnswersView(getBackLinkFromSession, errors, data)))
            },
          success =>
            for {
              _ <- request.sessionData match {
                     case data: ManagingProperty =>
                       appointNewAgentSession.saveOrUpdate(
                         data
                           .copy(appointmentScope = Some(AppointmentScope.withName(success.scope)))
                           .copy(backLink = None)
                       )
                   }
              sessionDataOpt <- appointAgentPropertiesSession.get[AppointAgentToSomePropertiesSession]
              agentListYears <- agentRelationshipService.getMyOrganisationAgents()
              agentAnswers   <- appointNewAgentSession.get[ManagingProperty]
              listYears: List[String] = agentAnswers.fold(List("2017", "2023"))(answers =>
                                          answers.specificRatingList.fold(List("2017", "2023"))(List(_))
                                        )
              _ <- agentRelationshipService.postAgentAppointmentChange(
                     AgentAppointmentChangeRequest(
                       action = AppointmentAction.APPOINT,
                       scope = AppointmentScope.withName(success.scope),
                       agentRepresentativeCode = success.agentRepresentativeCode,
                       propertyLinks = sessionDataOpt.flatMap(_.agentAppointAction.map(_.propertyLinkIds)),
                       listYears = Some(listYears)
                     )
                   )
            } yield Redirect(routes.ConfirmAgentAppointController.onPageLoad())
        )
    }

  private def getBackLinkFromSession(implicit request: AppointAgentSessionRequest[AnyContent]) =
    request.sessionData.backLink.getOrElse(config.dashboardUrl("home"))
}
