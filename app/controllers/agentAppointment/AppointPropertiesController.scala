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
import actions.requests.{AuthenticatedRequest, BasicAuthenticatedRequest}
import binders.pagination.PaginationParameters
import binders.propertylinks.ExternalPropertyLinkManagementSortField.ExternalPropertyLinkManagementSortField
import binders.propertylinks.{ExternalPropertyLinkManagementSortField, ExternalPropertyLinkManagementSortOrder, GetPropertyLinksParameters}
import config.ApplicationConfig
import connectors._
import controllers._
import form.FormValidation.nonEmptyList
import models.GroupAccount.AgentGroupAccount
import models._
import models.propertyrepresentation.{AgentAppointmentChangeRequest, AppointAgentToSomePropertiesSession, AppointNewAgentSession, AppointmentAction, AppointmentScope, FilterAppointProperties, FilterRevokePropertiesSessionData, ManagingProperty, RevokeAgentFromSomePropertiesSession}
import models.searchApi.AgentPropertiesFilter.Both
import models.searchApi._
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepo
import services.AgentRelationshipService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

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


  def onPageLoad(pagination: PaginationParameters,
                  agentCode: Long,
                  agentAppointed: Option[String],
                  backLink: String): Action[AnyContent] = authenticated.async { implicit request =>
    searchForAppointableProperties(pagination, agentCode, agentAppointed, backLink, Some(GetPropertyLinksParameters()))
  }

  private def searchForAppointableProperties(pagination: PaginationParameters,
                                              agentCode: Long,
                                              agentAppointed: Option[String],
                                              backLink: String,
                                              searchParamsOpt: Option[GetPropertyLinksParameters] = None)(
                                              implicit request: AuthenticatedRequest[_],
                                              hc: HeaderCarrier) =
    for {
      sessionDataOpt <- appointAgentPropertiesSession.get[AppointAgentToSomePropertiesSession]
      agentOrganisation <- accounts.withAgentCode(agentCode.toString)
      searchParams = searchParamsOpt match {
        case Some(params) => params
        case None =>
          GetPropertyLinksParameters().copy(
            address = sessionDataOpt.flatMap(_.filters.address),
            agent = sessionDataOpt.flatMap(_.filters.agent),
            sortorder = sessionDataOpt.fold(ExternalPropertyLinkManagementSortOrder.ASC)(_.filters.sortOrder)
          )

      }
      agentList <- agentRelationshipService.getMyOrganisationAgents()
      response <- agentRelationshipService.getMyOrganisationPropertyLinksWithAgentFiltering(
        params = searchParams,
        pagination = AgentPropertiesParameters(
          agentCode = agentCode,
          pageNumber = pagination.page,
          pageSize = pagination.pageSize,
          agentAppointed = agentAppointed.getOrElse(Both.name)
        ),
        organisationId = request.organisationAccount.id,
        agentOrganisationId =
          agentOrganisation.fold(throw new IllegalArgumentException("agent organisation required."))(_.id)
      )
      searchFilters = FilterAppointProperties(
        address = searchParams.address,
        agent = searchParams.agent,
        sortOrder = searchParams.sortorder
      )
      _ <- appointAgentPropertiesSession.saveOrUpdate[AppointAgentToSomePropertiesSession](
        sessionDataOpt.fold(AppointAgentToSomePropertiesSession(filters = searchFilters))(data =>
          data.copy(filters = searchFilters)))
      _ <- propertyLinksSessionRepo.saveOrUpdate(SessionPropertyLinks(response))
    } yield {
      agentOrganisation match {
        case Some(organisation) =>
          Ok(
            appointAgentPropertiesView(
              f = None,
              model = AppointAgentPropertiesVM(
                organisation,
                response,
                Some(agentCode),
                !agentAppointed.contains("NO")
              ),
              pagination = pagination,
              params = searchParams,
              agentCode = agentCode,
              agentAppointed = agentAppointed,
              organisationAgents = agentList,
              backLink = Some(backLink)
            ))
        case None =>
          notFound
      }
    }

  def onSubmit(agentCode: Long, agentAppointed: Option[String], backLinkUrl: String): Action[AnyContent] =
    authenticated.async { implicit request =>
      appointAgentBulkActionForm
        .bindFromRequest()
        .fold(
          errors => appointAgentPropertiesBadRequest(errors, agentCode, agentAppointed, backLinkUrl),
          success = (action: AgentAppointBulkAction) => {
            for {
              sessionDataOpt <- appointAgentPropertiesSession.get[AppointAgentToSomePropertiesSession]
              _ <- appointAgentPropertiesSession.saveOrUpdate[AppointAgentToSomePropertiesSession](
                sessionDataOpt.fold(AppointAgentToSomePropertiesSession(agentAppointAction = Some(action)))(
                  data => data.copy(agentAppointAction = Some(action))))
              data <- appointNewAgentSession.get[AppointNewAgentSession]
              propertySelectionSize <- agentRelationshipService.getMyOrganisationPropertyLinksCount()
              agent <- appointAgentPropertiesSession.get[AppointAgentToSomePropertiesSession]
            } yield (data, propertySelectionSize, agent) match {
              case (Some(data: ManagingProperty), propertySelectionSize, Some(AppointAgentToSomePropertiesSession(Some(agent), _))) =>
                appointNewAgentSession.saveOrUpdate(
                  data.copy(propertySelectedSize = Some(agent.propertyLinkIds.size),
                    totalPropertySelectionSize = Some(propertySelectionSize)))
                Redirect(controllers.agentAppointment.routes.CheckYourAnswersController.onPageLoad())
              case _ =>
                NotFound(errorHandler.notFoundTemplate)
            }
          }
        )
    }

  private def appointAgentPropertiesBadRequest(
                                                errors: Form[_],
                                                agentCode: Long,
                                                agentAppointed: Option[String],
                                                backLinkUrl: String)(implicit request: BasicAuthenticatedRequest[_]) =
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
          BadRequest(
            appointAgentPropertiesView(
              Some(errors),
              AppointAgentPropertiesVM(group, response),
              PaginationParameters(),
              GetPropertyLinksParameters(),
              agentCode,
              agentAppointed,
              agentList,
              backLink = Some(backLinkUrl)
            ))
      case None =>
        Future.successful(notFound)
    }

  def appointAgentBulkActionForm: Form[AgentAppointBulkAction] =
    Form(
      mapping(
        "agentCode" -> longNumber,
        "name" -> text,
        "linkIds" -> list(text).verifying(nonEmptyList),
        "backLinkUrl" -> text
      )(AgentAppointBulkAction.apply)(AgentAppointBulkAction.unapply))
}

