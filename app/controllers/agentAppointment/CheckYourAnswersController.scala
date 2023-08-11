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
import actions.agentrelationship.request.AppointAgentSessionRequest
import binders.pagination.PaginationParameters
import binders.propertylinks.GetPropertyLinksParameters
import config.ApplicationConfig
import controllers.PropertyLinkingController
import controllers.agentAppointment.AppointNewAgentForms._
import models.propertyrepresentation
import models.propertyrepresentation.AgentAppointmentChangesRequest.submitAgentAppointmentRequest
import models.propertyrepresentation._
import models.searchApi.AgentPropertiesFilter.Both
import models.searchApi.AgentPropertiesParameters
import play.api.data.FormError
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepo
import services.AgentRelationshipService
import uk.gov.hmrc.http.BadRequestException
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class CheckYourAnswersController @Inject()(
                                            val errorHandler: CustomErrorHandler,
                                            authenticated: AuthenticatedAction,
                                            withAppointAgentSession: WithAppointAgentSessionRefiner,
                                            agentRelationshipService: AgentRelationshipService,
                                            @Named("appointNewAgentSession") val sessionRepo: SessionRepo,
                                            @Named("appointNewAgentSession") val appointNewAgentSession: SessionRepo,
                                            @Named("appointLinkSession") val propertyLinksSessionRepo: SessionRepo,
                                            @Named("revokeAgentPropertiesSession") val revokeAgentPropertiesSessionRepo: SessionRepo,
                                            @Named("appointAgentPropertiesSession") val appointAgentPropertiesSession: SessionRepo,
                                            checkYourAnswersView: views.html.propertyrepresentation.appoint.checkYourAnswers,
                                            confirmationView: views.html.propertyrepresentation.appoint.confirmation)(
                                            implicit override val messagesApi: MessagesApi,
                                            override val controllerComponents: MessagesControllerComponents,
                                            executionContext: ExecutionContext,
                                            val config: ApplicationConfig
                                          ) extends PropertyLinkingController {

  def onPageLoad(): Action[AnyContent] = authenticated.andThen(withAppointAgentSession) { implicit request =>
    PartialFunction
      .condOpt(request.sessionData) {
        case data: ManagingProperty =>
          sessionRepo.saveOrUpdate(
            data.copy().copy(backLink = Some(routes.AddAgentController.checkAnswers.url)))
          val propertyAmount =
            if (data.totalPropertySelectionSize.isDefined && data.propertySelectedSize.isDefined)
              Some(data.propertySelectedSize.get, data.totalPropertySelectionSize.get) else None
          Ok(checkYourAnswersView(submitAgentAppointmentRequest, data, propertyAmount))
      }
      .getOrElse(NotFound(errorHandler.notFoundTemplate))
  }

  def onSubmit: Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async { implicit request =>
    submitAgentAppointmentRequest.bindFromRequest.fold(
      errors => {
        PartialFunction
          .condOpt(request.sessionData) {
            case data: ManagingProperty =>
              Future.successful(BadRequest(checkYourAnswersView(errors, data, None)))
          }
          .getOrElse(Future.successful(NotFound(errorHandler.notFoundTemplate)))
      }, { success => {
        for {
          _ <- request.sessionData match {
            case data: ManagingProperty =>
              sessionRepo.saveOrUpdate(
                data.copy(appointmentScope = Some(AppointmentScope.withName(success.scope)))
                  .copy(backLink = Some(routes.AddAgentController.appointAgent.url)))
          }
          sessionDataOpt <- appointAgentPropertiesSession.get[AppointAgentToSomePropertiesSession]
          agentListYears <- agentRelationshipService.getMyOrganisationAgents()
          listYears = agentListYears.agents
            .find(_.representativeCode == agentCode)
            .flatMap(_.listYears)
            .getOrElse(Seq("2017", "2023"))
            .toList
          _ <- agentRelationshipService.postAgentAppointmentChange(
            AgentAppointmentChangeRequest(
              action = AppointmentAction.APPOINT,
              scope = AppointmentScope.withName(success.scope),
              agentRepresentativeCode = success.agentRepresentativeCode,
              propertyLinks = sessionDataOpt.flatMap(_.agentAppointAction.map(_.propertyLinkIds)),
              listYears = Some(listYears)
            ))
          //TODO: Add error handling in here
        } yield Redirect(controllers.agentAppointment.routes.AddAgentController.confirmAppointAgent)
      }
      }
    )
  }
}