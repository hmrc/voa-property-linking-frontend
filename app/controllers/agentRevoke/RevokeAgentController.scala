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

package controllers.agentRevoke

import actions.AuthenticatedAction
import actions.requests.AuthenticatedRequest
import binders.pagination.PaginationParameters
import binders.propertylinks.{ExternalPropertyLinkManagementSortField, ExternalPropertyLinkManagementSortOrder, GetPropertyLinksParameters}
import config.ApplicationConfig
import connectors._
import controllers._
import form.FormValidation.nonEmptyList
import models.GroupAccount.AgentGroupAccount
import models._
import models.propertyrepresentation._
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

class RevokeAgentController @Inject() (
      val errorHandler: CustomErrorHandler,
      accounts: GroupAccounts,
      authenticated: AuthenticatedAction,
      agentRelationshipService: AgentRelationshipService,
      @Named("revokeAgentPropertiesSession") val revokeAgentPropertiesSessionRepo: SessionRepo,
      @Named("appointLinkSession") val propertyLinksSessionRepo: SessionRepo,
      revokeAgentSummaryView: views.html.propertyrepresentation.revokeAgentSummary,
      revokeAgentPropertiesView: views.html.propertyrepresentation.revokeAgentProperties)(implicit
      override val messagesApi: MessagesApi,
      override val controllerComponents: MessagesControllerComponents,
      executionContext: ExecutionContext,
      val config: ApplicationConfig
) extends PropertyLinkingController {
  val logger: Logger = Logger(this.getClass)

  lazy val addressForm: Form[String] = Form(single("address" -> nonEmptyText))

  lazy val filterRevokePropertiesForm: Form[FilterRevokePropertiesForm] =
    Form(
      mapping(
        "address" -> optional(text),
        "agent"   -> optional(text)
      )(FilterRevokePropertiesForm.apply)(FilterRevokePropertiesForm.unapply)
        .verifying("error.propertyRepresentation.appoint.filter", f => f.address.nonEmpty || f.agent.nonEmpty)
    )

  def selectAgentPropertiesSearchSort(pagination: PaginationParameters, agentCode: Long): Action[AnyContent] =
    authenticated.async { implicit request =>
      searchPropertiesForRevoke(pagination, agentCode, Some(GetPropertyLinksParameters()))
    }

  def paginateRevokeProperties(pagination: PaginationParameters, agentCode: Long): Action[AnyContent] =
    authenticated.async { implicit request =>
      searchPropertiesForRevoke(pagination, agentCode)
    }

  def sortRevokePropertiesByAddress(pagination: PaginationParameters, agentCode: Long): Action[AnyContent] =
    authenticated.async { implicit request =>
      revokeAgentPropertiesSessionRepo.get[RevokeAgentFromSomePropertiesSession].flatMap { data =>
        searchPropertiesForRevoke(
          pagination,
          agentCode,
          data.fold(
            Some(
              GetPropertyLinksParameters().reverseSorting
                .copy(sortfield = ExternalPropertyLinkManagementSortField.ADDRESS)
            )
          )(d =>
            Some(
              GetPropertyLinksParameters()
                .copy(
                  address = d.filters.address,
                  sortfield = ExternalPropertyLinkManagementSortField.ADDRESS,
                  sortorder = d.filters.sortOrder
                )
                .reverseSorting
            )
          )
        )
      }
    }

  private def searchPropertiesForRevoke(
        pagination: PaginationParameters,
        agentCode: Long,
        searchParamsOpt: Option[GetPropertyLinksParameters] = None
  )(implicit request: AuthenticatedRequest[_], hc: HeaderCarrier) =
    accounts.withAgentCode(agentCode.toString).flatMap {
      case Some(group) =>
        for {
          sessionDataOpt <- revokeAgentPropertiesSessionRepo.get[RevokeAgentFromSomePropertiesSession]
          searchParams = searchParamsOpt match {
                           case Some(params) => params
                           case None =>
                             GetPropertyLinksParameters().copy(
                               address = sessionDataOpt.flatMap(_.filters.address),
                               sortorder =
                                 sessionDataOpt.fold(ExternalPropertyLinkManagementSortOrder.ASC)(_.filters.sortOrder)
                             )
                         }
          response: OwnerAuthResult <-
            agentRelationshipService
              .getMyAgentPropertyLinks(
                agentCode = agentCode,
                searchParams = searchParams,
                pagination = PaginationParams(pagination.startPoint, pagination.pageSize, requestTotalRowCount = false)
              )
              .map(oar => oar.copy(authorisations = oar.authorisations.take(pagination.pageSize)))
          searchFilters =
            FilterRevokePropertiesSessionData(address = searchParams.address, sortOrder = searchParams.sortorder)
          _ <- revokeAgentPropertiesSessionRepo.saveOrUpdate[RevokeAgentFromSomePropertiesSession](
                 sessionDataOpt.fold(RevokeAgentFromSomePropertiesSession(filters = searchFilters))(data =>
                   data.copy(filters = searchFilters)
                 )
               )
        } yield Ok(
          revokeAgentPropertiesView(
            None,
            RevokeAgentPropertiesVM(group, response),
            pagination,
            searchParams,
            agentCode,
            agent.routes.ManageAgentController.showManageAgent.url
          )
        )
      case None => Future.successful(NotFound(s"Unknown Agent: $agentCode"))
    }
  // this endpoint only exists so we don't 404 when changing language after getting an error on submit
  def showFilterPropertiesForRevoke(pagination: PaginationParameters, agentCode: Long): Action[AnyContent] =
    authenticated.async { implicit request =>
      searchPropertiesForRevoke(pagination, agentCode)
    }

  def filterPropertiesForRevoke(pagination: PaginationParameters, agentCode: Long): Action[AnyContent] =
    authenticated.async { implicit request =>
      addressForm
        .bindFromRequest()
        .fold(
          hasErrors = errors =>
            accounts.withAgentCode(agentCode.toString).flatMap {
              case Some(group) =>
                for {
                  response: OwnerAuthResult <- agentRelationshipService
                                                 .getMyAgentPropertyLinks(
                                                   agentCode = agentCode,
                                                   searchParams = GetPropertyLinksParameters(),
                                                   pagination = PaginationParams(
                                                     pagination.startPoint,
                                                     pagination.pageSize,
                                                     requestTotalRowCount = false
                                                   )
                                                 )
                } yield BadRequest(
                  revokeAgentPropertiesView(
                    Some(errors),
                    RevokeAgentPropertiesVM(group, response),
                    pagination,
                    GetPropertyLinksParameters(),
                    agentCode,
                    agent.routes.ManageAgentController.showManageAgent.url
                  )
                )
              case None => Future.successful(NotFound(s"Unknown Agent: $agentCode"))
            },
          success = (address: String) =>
            searchPropertiesForRevoke(
              pagination,
              agentCode,
              Some(GetPropertyLinksParameters().copy(address = Some(address)))
            )
        )
    }

  def confirmRevokeAgentFromSome: Action[AnyContent] =
    authenticated.async { implicit request =>
      revokeAgentPropertiesSessionRepo.get[RevokeAgentFromSomePropertiesSession].flatMap {
        case Some(RevokeAgentFromSomePropertiesSession(Some(agent), _)) =>
          Future.successful(Ok(revokeAgentSummaryView(action = agent, agentOrganisation = agent.name)))
        case _ => errorHandler.notFoundTemplate.map(html => NotFound(html))
      }
    }
  // this endpoint only exists so we don't 404 when changing language after getting an error on submit
  def showRevokeAgentSummary(
        pagination: PaginationParameters,
        agentCode: Long
  ): Action[AnyContent] =
    authenticated.async { implicit request =>
      searchPropertiesForRevoke(pagination, agentCode)
    }

  def revokeAgentSummary(pagination: PaginationParameters, agentCode: Long): Action[AnyContent] =
    authenticated.async { implicit request =>
      revokeAgentBulkActionForm
        .bindFromRequest()
        .fold(
          hasErrors = errors => {
            val data: Map[String, String] = errors.data
            val pagination = AgentPropertiesParameters(agentCode = agentCode)

            accounts.withAgentCode(pagination.agentCode.toString).flatMap {
              case Some(AgentGroupAccount(group, agentCode)) =>
                for {
                  response <- agentRelationshipService
                                .getMyAgentPropertyLinks(
                                  agentCode = pagination.agentCode,
                                  searchParams = GetPropertyLinksParameters(
                                    address = pagination.address,
                                    agent = Some(group.companyName),
                                    sortfield = ExternalPropertyLinkManagementSortField
                                      .withName(pagination.sortField.name.toUpperCase),
                                    sortorder = ExternalPropertyLinkManagementSortOrder
                                      .withName(pagination.sortOrder.name.toUpperCase)
                                  ),
                                  pagination = PaginationParams(
                                    startPoint = pagination.startPoint,
                                    pageSize = pagination.pageSize,
                                    requestTotalRowCount = false
                                  )
                                )
                                .map { oar =>
                                  oar.copy(
                                    authorisations = oar.authorisations.take(pagination.pageSize),
                                    filterTotal = oar.authorisations.size
                                  )
                                }
                } yield BadRequest(
                  revokeAgentPropertiesView(
                    Some(errors),
                    model = RevokeAgentPropertiesVM(group, response),
                    pagination = PaginationParameters(),
                    params = GetPropertyLinksParameters(),
                    agentCode = agentCode,
                    backLink = data("backLinkUrl")
                  )
                )
              case _ => notFound
            }
          },
          success = (action: AgentRevokeBulkAction) =>
            accounts.withAgentCode(action.agentCode.toString).flatMap {
              case Some(group) =>
                (
                  for {
                    sessionDataOpt <- revokeAgentPropertiesSessionRepo.get[RevokeAgentFromSomePropertiesSession]
                    _ <- agentRelationshipService
                           .postAgentAppointmentChange(
                             AgentAppointmentChangeRequest(
                               agentRepresentativeCode = action.agentCode,
                               action = AppointmentAction.REVOKE,
                               scope = AppointmentScope.PROPERTY_LIST,
                               propertyLinks = Some(action.propertyLinkIds),
                               listYears = None
                             )
                           )
                    _ <- revokeAgentPropertiesSessionRepo.saveOrUpdate[RevokeAgentFromSomePropertiesSession](
                           sessionDataOpt.fold(RevokeAgentFromSomePropertiesSession(agentRevokeAction = Some(action)))(
                             data => data.copy(agentRevokeAction = Some(action))
                           )
                         )
                  } yield Redirect(
                    controllers.agentRevoke.routes.RevokeAgentController.confirmRevokeAgentFromSome
                  )
                ).recoverWith {
                  case e: services.AppointRevokeException =>
                    for {
                      response <- agentRelationshipService
                                    .getMyOrganisationsPropertyLinks(
                                      GetPropertyLinksParameters(agent = Some(group.companyName)),
                                      DefaultPaginationParams
                                    )
                                    .map { oar =>
                                      val filteredProperties = filterProperties(oar.authorisations, group.id)
                                      oar.copy(
                                        authorisations = filteredProperties.take(DefaultPaginationParams.pageSize),
                                        filterTotal = filteredProperties.size
                                      )
                                    }
                    } yield BadRequest(
                      revokeAgentPropertiesView(
                        Some(revokeAgentBulkActionForm.withError("appoint.error", "error.transaction")),
                        model = RevokeAgentPropertiesVM(group, response),
                        pagination = PaginationParameters(),
                        params = GetPropertyLinksParameters(),
                        agentCode = action.agentCode,
                        backLink = action.backLinkUrl
                      )
                    )
                  case e: Exception => throw e
                }
              case None => notFound
            }
        )
    }

  def filterProperties(authorisations: Seq[OwnerAuthorisation], agentOrganisaionId: Long): Seq[OwnerAuthorisation] =
    authorisations.filter(auth => auth.agents.map(_.organisationId).contains(agentOrganisaionId))

  def revokeAgentBulkActionForm: Form[AgentRevokeBulkAction] =
    Form(
      mapping(
        "agentCode"   -> longNumber,
        "name"        -> text,
        "linkIds"     -> list(text).verifying(nonEmptyList),
        "backLinkUrl" -> text
      )(AgentRevokeBulkAction.apply)(AgentRevokeBulkAction.unapply)
    )

}

case class RevokeAgentPropertiesVM(
      agentGroup: GroupAccount,
      response: OwnerAuthResult,
      agentCode: Option[Long] = None,
      showAllProperties: Boolean = false
)

case class FilterRevokePropertiesForm(address: Option[String], agent: Option[String])
