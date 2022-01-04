/*
 * Copyright 2022 HM Revenue & Customs
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
import models.propertyrepresentation.{FilterAppointProperties, FilterRevokePropertiesSessionData}
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

class AppointAgentController @Inject()(
      val errorHandler: CustomErrorHandler,
      accounts: GroupAccounts,
      authenticated: AuthenticatedAction,
      agentRelationshipService: AgentRelationshipService,
      @Named("appointLinkSession") val propertyLinksSessionRepo: SessionRepo,
      @Named("revokeAgentPropertiesSession") val revokeAgentPropertiesSessionRepo: SessionRepo,
      @Named("appointAgentPropertiesSession") val appointAgentPropertiesSession: SessionRepo,
      appointAgentSummaryView: views.html.propertyrepresentation.appoint.appointAgentSummary,
      revokeAgentSummaryView: views.html.propertyrepresentation.revokeAgentSummary,
      revokeAgentPropertiesView: views.html.propertyrepresentation.revokeAgentProperties,
      appointAgentPropertiesView: views.html.propertyrepresentation.appoint.appointAgentProperties
)(
      implicit override val messagesApi: MessagesApi,
      override val controllerComponents: MessagesControllerComponents,
      executionContext: ExecutionContext,
      val config: ApplicationConfig
) extends PropertyLinkingController {

  val logger: Logger = Logger(this.getClass)

  lazy val addressForm: Form[String] = Form(single("address" -> text))

  lazy val filterAppointPropertiesForm: Form[FilterAppointPropertiesForm] =
    Form(
      mapping(
        "address" -> optional(text),
        "agent"   -> optional(text)
      )(FilterAppointPropertiesForm.apply)(FilterAppointPropertiesForm.unapply)
        .verifying("error.propertyRepresentation.appoint.filter", f => f.address.nonEmpty || f.agent.nonEmpty)
    )

  def getMyOrganisationPropertyLinksWithAgentFiltering(
        pagination: PaginationParameters,
        agentCode: Long,
        agentAppointed: Option[String],
        backLink: String
  ): Action[AnyContent] = authenticated.async { implicit request =>
    searchForAppointableProperties(pagination, agentCode, agentAppointed, backLink, Some(GetPropertyLinksParameters()))
  }

  def filterPropertiesForAppoint(
        pagination: PaginationParameters,
        agentCode: Long,
        agentAppointed: Option[String],
        backLink: String
  ): Action[AnyContent] = authenticated.async { implicit request =>
    filterAppointPropertiesForm
      .bindFromRequest()
      .fold(
        hasErrors = errors => appointAgentPropertiesBadRequest(errors),
        success = (filter: FilterAppointPropertiesForm) =>
          searchForAppointableProperties(
            pagination,
            agentCode,
            agentAppointed,
            backLink,
            Some(GetPropertyLinksParameters().copy(address = filter.address, agent = filter.agent)))
      )
  }

  def paginatePropertiesForAppoint(
        pagination: PaginationParameters,
        agentCode: Long,
        agentAppointed: Option[String],
        backLink: String
  ): Action[AnyContent] = authenticated.async { implicit request =>
    searchForAppointableProperties(pagination, agentCode, agentAppointed, backLink)
  }

  def sortPropertiesForAppoint(
        sortField: ExternalPropertyLinkManagementSortField,
        pagination: PaginationParameters,
        agentCode: Long,
        agentAppointed: Option[String],
        backLink: String
  ): Action[AnyContent] = authenticated.async { implicit request =>
    appointAgentPropertiesSession.get[FilterAppointProperties].flatMap {
      case Some(sessionData) =>
        searchForAppointableProperties(
          pagination,
          agentCode,
          agentAppointed,
          backLink,
          Some(
            GetPropertyLinksParameters()
              .copy(
                address = sessionData.address,
                agent = sessionData.agent,
                sortorder = sessionData.sortOrder,
                sortfield = sortField)
              .reverseSorting)
        )
      case None =>
        searchForAppointableProperties(
          pagination,
          agentCode,
          agentAppointed,
          backLink,
          Some(GetPropertyLinksParameters().copy(sortfield = sortField).reverseSorting))
    }
  }

  private def searchForAppointableProperties(
        pagination: PaginationParameters,
        agentCode: Long,
        agentAppointed: Option[String],
        backLink: String,
        searchParamsOpt: Option[GetPropertyLinksParameters] = None)(
        implicit request: AuthenticatedRequest[_],
        hc: HeaderCarrier) =
    for {
      sessionDataOpt    <- appointAgentPropertiesSession.get[FilterAppointProperties]
      agentOrganisation <- accounts.withAgentCode(agentCode.toString)
      searchParams = searchParamsOpt match {
        case Some(params) => params
        case None =>
          GetPropertyLinksParameters().copy(
            address = sessionDataOpt.flatMap(_.address),
            agent = sessionDataOpt.flatMap(_.agent),
            sortorder = sessionDataOpt.fold(ExternalPropertyLinkManagementSortOrder.ASC)(_.sortOrder)
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
      _ <- appointAgentPropertiesSession.saveOrUpdate[FilterAppointProperties](
            FilterAppointProperties(
              address = searchParams.address,
              agent = searchParams.agent,
              sortOrder = searchParams.sortorder
            )
          )
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

  def appointAgentSummary(): Action[AnyContent] = authenticated.async { implicit request =>
    appointAgentBulkActionForm
      .bindFromRequest()
      .fold(
        errors => appointAgentPropertiesBadRequest(errors),
        success = (action: AgentAppointBulkAction) => {
          accounts.withAgentCode(action.agentCode.toString).flatMap {
            case Some(group) =>
              agentRelationshipService
                .createAndSubmitAgentRepRequest(
                  pLinkIds = action.propertyLinkIds,
                  agentCode = action.agentCode
                )
                .map(
                  _ =>
                    Ok(
                      appointAgentSummaryView(
                        action = action,
                        agentOrganisation = group.companyName,
                        backLinkUrl = action.backLinkUrl)))
                .recoverWith {
                  case e: services.AppointRevokeException =>
                    for {
                      agentList <- agentRelationshipService.getMyOrganisationAgents()
                      response <- agentRelationshipService.getMyOrganisationPropertyLinksWithAgentFiltering(
                                   GetPropertyLinksParameters(),
                                   AgentPropertiesParameters(agentCode = action.agentCode),
                                   request.organisationAccount.id,
                                   group.id
                                 )
                    } yield
                      BadRequest(appointAgentPropertiesView(
                        f = Some(appointAgentBulkActionForm.withError("appoint.error", "error.transaction")),
                        model = AppointAgentPropertiesVM(group, response),
                        pagination = PaginationParameters(),
                        params = GetPropertyLinksParameters(),
                        agentCode = action.agentCode,
                        agentAppointed = None,
                        organisationAgents = agentList,
                        backLink = Some(action.backLinkUrl)
                      ))
                  case e: Exception => throw e
                }
            case None =>
              Future.successful(notFound)
          }
        }
      )
  }

  private def appointAgentPropertiesBadRequest(errors: Form[_])(implicit request: BasicAuthenticatedRequest[_]) = {
    val data: Map[String, String] = errors.data
    accounts.withAgentCode(data("agentCode")).flatMap {
      case Some(group) =>
        for {
          agentList <- agentRelationshipService.getMyOrganisationAgents()
          response <- agentRelationshipService.getMyOrganisationPropertyLinksWithAgentFiltering(
                       GetPropertyLinksParameters(),
                       AgentPropertiesParameters(agentCode = data("agentCode").toLong),
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
              data("agentCode").toLong,
              data.get("agentAppointed"),
              agentList,
              backLink = Some(data("backLinkUrl"))
            ))
      case None =>
        Future.successful(notFound)
    }
  }

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
      revokeAgentPropertiesSessionRepo.get[FilterRevokePropertiesSessionData].flatMap {
        case Some(sessionData) =>
          searchPropertiesForRevoke(
            pagination,
            agentCode,
            Some(
              GetPropertyLinksParameters()
                .copy(
                  address = sessionData.address,
                  sortfield = ExternalPropertyLinkManagementSortField.ADDRESS,
                  sortorder = sessionData.sortOrder)
                .reverseSorting)
          )
        case None =>
          searchPropertiesForRevoke(
            pagination,
            agentCode,
            Some(
              GetPropertyLinksParameters().reverseSorting
                .copy(sortfield = ExternalPropertyLinkManagementSortField.ADDRESS)))
      }
    }

  private def searchPropertiesForRevoke(
        pagination: PaginationParameters,
        agentCode: Long,
        searchParamsOpt: Option[GetPropertyLinksParameters] = None)(
        implicit request: AuthenticatedRequest[_],
        hc: HeaderCarrier) =
    accounts.withAgentCode(agentCode.toString).flatMap {
      case Some(group) =>
        for {
          sessionDataOpt <- revokeAgentPropertiesSessionRepo.get[FilterRevokePropertiesSessionData]
          searchParams = searchParamsOpt match {
            case Some(params) => params
            case None =>
              GetPropertyLinksParameters().copy(
                address = sessionDataOpt.flatMap(_.address),
                sortorder = sessionDataOpt.fold(ExternalPropertyLinkManagementSortOrder.ASC)(_.sortOrder))
          }
          response: OwnerAuthResult <- agentRelationshipService
                                        .getMyAgentPropertyLinks(
                                          agentCode = agentCode,
                                          searchParams = searchParams,
                                          pagination = PaginationParams(
                                            pagination.startPoint,
                                            pagination.pageSize,
                                            requestTotalRowCount = false))
                                        .map(oar =>
                                          oar.copy(authorisations = oar.authorisations.take(pagination.pageSize)))
          _ <- revokeAgentPropertiesSessionRepo.saveOrUpdate[FilterRevokePropertiesSessionData](
                FilterRevokePropertiesSessionData(address = searchParams.address, sortOrder = searchParams.sortorder))
          _ <- propertyLinksSessionRepo.saveOrUpdate(SessionPropertyLinks(response))
        } yield {
          Ok(
            revokeAgentPropertiesView(
              None,
              AppointAgentPropertiesVM(group, response),
              pagination,
              searchParams,
              agentCode,
              agent.routes.ManageAgentController.manageAgent(Some(agentCode)).url
            ))
        }
      case None => Future.successful(NotFound(s"Unknown Agent: $agentCode"))
    }

  def filterPropertiesForRevoke(pagination: PaginationParameters, agentCode: Long): Action[AnyContent] =
    authenticated.async { implicit request =>
      addressForm
        .bindFromRequest()
        .fold(
          hasErrors = errors => searchPropertiesForRevoke(pagination, agentCode),
          success = (address: String) =>
            searchPropertiesForRevoke(
              pagination,
              agentCode,
              Some(GetPropertyLinksParameters().copy(address = Some(address))))
        )
    }

  def revokeAgentSummary(): Action[AnyContent] = authenticated.async { implicit request =>
    revokeAgentBulkActionForm
      .bindFromRequest()
      .fold(
        hasErrors = errors => {
          val data: Map[String, String] = errors.data
          val pagination = AgentPropertiesParameters(agentCode = data("agentCode").toLong)

          accounts.withAgentCode(pagination.agentCode.toString).flatMap {
            case Some(AgentGroupAccount(group, agentCode)) =>
              for {
                response <- agentRelationshipService
                             .getMyAgentPropertyLinks(
                               agentCode = pagination.agentCode,
                               searchParams = GetPropertyLinksParameters(
                                 address = pagination.address,
                                 agent = Some(group.companyName),
                                 sortfield = ExternalPropertyLinkManagementSortField.withName(
                                   pagination.sortField.name.toUpperCase),
                                 sortorder = ExternalPropertyLinkManagementSortOrder.withName(
                                   pagination.sortOrder.name.toUpperCase)
                               ),
                               pagination = PaginationParams(
                                 startPoint = pagination.startPoint,
                                 pageSize = pagination.pageSize,
                                 requestTotalRowCount = false)
                             )
                             .map { oar =>
                               oar.copy(
                                 authorisations = oar.authorisations.take(pagination.pageSize),
                                 filterTotal = oar.authorisations.size)
                             }
              } yield {
                BadRequest(revokeAgentPropertiesView(
                  Some(errors),
                  model = AppointAgentPropertiesVM(group, response),
                  pagination = PaginationParameters(),
                  params = GetPropertyLinksParameters(),
                  agentCode = agentCode,
                  backLink = data("backLinkUrl")
                ))
              }
            case _ =>
              Future.successful(notFound)
          }
        },
        success = (action: AgentRevokeBulkAction) => {
          accounts.withAgentCode(action.agentCode.toString).flatMap {
            case Some(group) =>
              agentRelationshipService
                .createAndSubmitAgentRevokeRequest(pLinkIds = action.propertyLinkIds, agentCode = action.agentCode)
                .map(_ => Ok(revokeAgentSummaryView(action, group.companyName)))
                .recoverWith {
                  case e: services.AppointRevokeException =>
                    for {
                      response <- agentRelationshipService
                                   .getMyOrganisationsPropertyLinks(
                                     GetPropertyLinksParameters(agent = Some(group.companyName)),
                                     DefaultPaginationParams)
                                   .map { oar =>
                                     val filteredProperties = filterProperties(oar.authorisations, group.id)
                                     oar.copy(
                                       authorisations = filteredProperties.take(DefaultPaginationParams.pageSize),
                                       filterTotal = filteredProperties.size)
                                   }
                    } yield
                      BadRequest(revokeAgentPropertiesView(
                        Some(revokeAgentBulkActionForm.withError("appoint.error", "error.transaction")),
                        model = AppointAgentPropertiesVM(group, response),
                        pagination = PaginationParameters(),
                        params = GetPropertyLinksParameters(),
                        agentCode = action.agentCode,
                        backLink = action.backLinkUrl
                      ))
                  case e: Exception => throw e
                }
            case None =>
              Future.successful(notFound)
          }
        }
      )
  }

  def filterProperties(authorisations: Seq[OwnerAuthorisation], agentOrganisaionId: Long): Seq[OwnerAuthorisation] =
    authorisations.filter(auth => auth.agents.map(_.organisationId).contains(agentOrganisaionId))

  def appointAgentBulkActionForm: Form[AgentAppointBulkAction] =
    Form(
      mapping(
        "agentCode"   -> longNumber,
        "linkIds"     -> list(text).verifying(nonEmptyList),
        "backLinkUrl" -> text
      )(AgentAppointBulkAction.apply)(AgentAppointBulkAction.unpack))

  def revokeAgentBulkActionForm: Form[AgentRevokeBulkAction] =
    Form(
      mapping(
        "agentCode"   -> longNumber,
        "linkIds"     -> list(text).verifying(nonEmptyList),
        "backLinkUrl" -> text
      )(AgentRevokeBulkAction.apply)(AgentRevokeBulkAction.unpack))

}

case class AppointAgentPropertiesVM(
      agentGroup: GroupAccount,
      response: OwnerAuthResult,
      agentCode: Option[Long] = None,
      showAllProperties: Boolean = false
)

case class FilterAppointPropertiesForm(address: Option[String], agent: Option[String])
