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
import actions.requests.BasicAuthenticatedRequest
import binders.pagination.PaginationParameters
import binders.propertylinks.GetPropertyLinksParameters
import config.ApplicationConfig
import connectors._
import controllers._
import form.FormValidation.nonEmptyList
import models._
import models.propertyrepresentation.{AppointAgentToSomePropertiesSession, AppointNewAgentSession, ManagingProperty}
import models.searchApi._
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepo
import services.AgentRelationshipService
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import javax.inject.{Inject, Named}
import scala.concurrent.ExecutionContext

class AppointPropertiesController @Inject()(
      val errorHandler: CustomErrorHandler,
      accounts: GroupAccounts,
      authenticated: AuthenticatedAction,
      agentRelationshipService: AgentRelationshipService,
      @Named("appointNewAgentSession") val appointNewAgentSession: SessionRepo,
      @Named("appointLinkSession") val propertyLinksSessionRepo: SessionRepo,
      @Named("revokeAgentPropertiesSession") val revokeAgentPropertiesSessionRepo: SessionRepo,
      @Named("appointAgentPropertiesSession") val appointAgentPropertiesSession: SessionRepo,
      appointAgentPropertiesView: views.html.propertyrepresentation.appoint.appointAgentProperties)(
      implicit override val messagesApi: MessagesApi,
      override val controllerComponents: MessagesControllerComponents,
      executionContext: ExecutionContext,
      val config: ApplicationConfig
) extends PropertyLinkingController {
  val logger: Logger = Logger(this.getClass)

  def onSubmit(
        paginationParameters: PaginationParameters,
        agentCode: Long,
        agentAppointed: Option[String],
        backLinkUrl: RedirectUrl,
        fromManageAgentJourney: Boolean): Action[AnyContent] =
    authenticated.async { implicit request =>
      appointAgentBulkActionForm
        .bindFromRequest()
        .fold(
          errors =>
            accounts.withAgentCode(agentCode.toString).flatMap {
              case Some(group) =>
                for {
                  agentList <- agentRelationshipService.getMyOrganisationAgents()
                  response <- agentRelationshipService.getMyOrganisationPropertyLinksWithAgentFiltering(
                               GetPropertyLinksParameters(),
                               AgentPropertiesParameters(agentCode),
                               request.organisationAccount.id,
                               group.id
                             )
                } yield
                  BadRequest(appointAgentPropertiesView(
                    Some(errors),
                    AppointAgentPropertiesVM(group, response),
                    paginationParameters,
                    GetPropertyLinksParameters(),
                    agentCode,
                    agentAppointed,
                    agentList,
                    backLink = Some(config.safeRedirect(backLinkUrl)),
                    fromManageAgentJourney
                  ))
              case None => notFound
          },
          success = (action: AgentAppointBulkAction) => {
            for {
              sessionDataOpt <- appointAgentPropertiesSession.get[AppointAgentToSomePropertiesSession]
              _ <- appointAgentPropertiesSession.saveOrUpdate[AppointAgentToSomePropertiesSession](
                    sessionDataOpt.fold(AppointAgentToSomePropertiesSession(agentAppointAction = Some(action)))(data =>
                      data.copy(agentAppointAction = Some(action))))
              propertySelectionSize <- agentRelationshipService.getMyOrganisationPropertyLinksCount()
              agentSessionData      <- appointNewAgentSession.get[AppointNewAgentSession]
              result <- agentSessionData match {
                         case Some(data: ManagingProperty) =>
                           appointNewAgentSession
                             .saveOrUpdate(
                               data.copy(
                                 propertySelectedSize = action.propertyLinkIds.size,
                                 totalPropertySelectionSize = propertySelectionSize,
                                 backLink = Some(config.safeRedirect(backLinkUrl))
                               ))
                             .map(_ => Redirect(agentAppointment.routes.CheckYourAnswersController.onPageLoad()))
                         case _ =>
                           errorHandler.notFoundTemplate.map(html => NotFound(html))
                       }
            } yield result
          }
        )
    }

  def appointAgentBulkActionForm: Form[AgentAppointBulkAction] =
    Form(
      mapping(
        "agentCode"   -> longNumber,
        "name"        -> text,
        "linkIds"     -> list(text).verifying(nonEmptyList),
        "backLinkUrl" -> text
      )(AgentAppointBulkAction.apply)(AgentAppointBulkAction.unapply))
}
