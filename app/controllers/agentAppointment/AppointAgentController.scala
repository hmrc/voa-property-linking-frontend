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
import models.propertyrepresentation._
import models.searchApi.AgentPropertiesFilter.Both
import models.searchApi._
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepo
import services.AgentRelationshipService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class AppointAgentController @Inject() (
      val errorHandler: CustomErrorHandler,
      accounts: GroupAccounts,
      authenticated: AuthenticatedAction,
      agentRelationshipService: AgentRelationshipService,
      @Named("appointNewAgentSession") val appointNewAgentSession: SessionRepo,
      @Named("appointLinkSession") val propertyLinksSessionRepo: SessionRepo,
      @Named("appointAgentPropertiesSession") val appointAgentPropertiesSession: SessionRepo,
      appointAgentSummaryView: views.html.propertyrepresentation.appoint.appointAgentSummary,
      appointAgentPropertiesView: views.html.propertyrepresentation.appoint.appointAgentProperties
)(implicit
      override val messagesApi: MessagesApi,
      override val controllerComponents: MessagesControllerComponents,
      executionContext: ExecutionContext,
      val config: ApplicationConfig
) extends PropertyLinkingController {
  val logger: Logger = Logger(this.getClass)

  lazy val addressForm: Form[String] = Form(single("address" -> nonEmptyText))

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
        backLinkUrl: RedirectUrl,
        fromManageAgentJourney: Boolean
  ): Action[AnyContent] =
    authenticated.async { implicit request =>
      searchForAppointableProperties(
        pagination,
        agentCode,
        agentAppointed,
        backLinkUrl,
        Some(GetPropertyLinksParameters()),
        fromManageAgentJourney
      )
    }

  // this endpoint only exists so we don't 404 when changing language after getting an error on search
  def showFilterPropertiesForAppoint(
        pagination: PaginationParameters,
        agentCode: Long,
        agentAppointed: Option[String],
        backLinkUrl: RedirectUrl,
        fromManageAgentJourney: Boolean
  ): Action[AnyContent] =
    authenticated.async { implicit request =>
      searchForAppointableProperties(
        pagination,
        agentCode,
        agentAppointed,
        backLinkUrl,
        fromManageAgentJourney = fromManageAgentJourney
      )
    }

  def filterPropertiesForAppoint(
        pagination: PaginationParameters,
        agentCode: Long,
        agentAppointed: Option[String],
        backLinkUrl: RedirectUrl,
        fromManageAgentJourney: Boolean
  ): Action[AnyContent] =
    authenticated.async { implicit request =>
      filterAppointPropertiesForm
        .bindFromRequest()
        .fold(
          hasErrors = errors =>
            appointAgentPropertiesBadRequest(errors, agentCode, agentAppointed, backLinkUrl, fromManageAgentJourney),
          success = (filter: FilterAppointPropertiesForm) =>
            searchForAppointableProperties(
              pagination,
              agentCode,
              agentAppointed,
              backLinkUrl,
              Some(GetPropertyLinksParameters().copy(address = filter.address, agent = filter.agent)),
              fromManageAgentJourney
            )
        )
    }

  def paginatePropertiesForAppoint(
        pagination: PaginationParameters,
        agentCode: Long,
        agentAppointed: Option[String],
        backLinkUrl: RedirectUrl,
        fromManageAgentJourney: Boolean
  ): Action[AnyContent] =
    authenticated.async { implicit request =>
      searchForAppointableProperties(
        pagination,
        agentCode,
        agentAppointed,
        backLinkUrl,
        fromManageAgentJourney = fromManageAgentJourney
      )
    }

  def sortPropertiesForAppoint(
        sortField: ExternalPropertyLinkManagementSortField,
        pagination: PaginationParameters,
        agentCode: Long,
        agentAppointed: Option[String],
        backLinkUrl: RedirectUrl,
        fromManageAgentJourney: Boolean
  ): Action[AnyContent] =
    authenticated.async { implicit request =>
      appointAgentPropertiesSession.get[AppointAgentToSomePropertiesSession].flatMap {
        case Some(sessionData) =>
          searchForAppointableProperties(
            pagination,
            agentCode,
            agentAppointed,
            backLinkUrl,
            Some(
              GetPropertyLinksParameters()
                .copy(
                  address = sessionData.filters.address,
                  agent = sessionData.filters.agent,
                  sortorder = sessionData.filters.sortOrder,
                  sortfield = sortField
                )
                .reverseSorting
            ),
            fromManageAgentJourney = fromManageAgentJourney
          )
        case None =>
          searchForAppointableProperties(
            pagination,
            agentCode,
            agentAppointed,
            backLinkUrl,
            Some(GetPropertyLinksParameters().copy(sortfield = sortField).reverseSorting),
            fromManageAgentJourney = fromManageAgentJourney
          )
      }
    }

  // todo fix this
  private def searchForAppointableProperties(
        pagination: PaginationParameters,
        agentCode: Long,
        agentAppointed: Option[String],
        backLinkUrl: RedirectUrl,
        searchParamsOpt: Option[GetPropertyLinksParameters] = None,
        fromManageAgentJourney: Boolean
  )(implicit request: AuthenticatedRequest[_], hc: HeaderCarrier): Future[Result] =
    for {
      sessionDataOpt    <- appointAgentPropertiesSession.get[AppointAgentToSomePropertiesSession]
      agentOrganisation <- accounts.withAgentCode(agentCode.toString)
      searchParams = searchParamsOpt match {
                       case Some(params) => params
                       case None =>
                         GetPropertyLinksParameters().copy(
                           address = sessionDataOpt.flatMap(_.filters.address),
                           agent = sessionDataOpt.flatMap(_.filters.agent),
                           sortorder =
                             sessionDataOpt.fold(ExternalPropertyLinkManagementSortOrder.ASC)(_.filters.sortOrder)
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
               data.copy(filters = searchFilters)
             )
           )
      _ <- propertyLinksSessionRepo.saveOrUpdate(SessionPropertyLinks(response))
      result <- agentOrganisation match {
                  case Some(organisation) =>
                    Future.successful(
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
                          backLink = Some(config.safeRedirect(backLinkUrl)),
                          manageJourneyFlag = fromManageAgentJourney
                        )
                      )
                    )
                  case None =>
                    notFound
                }
    } yield result

  def confirmAppointAgentToSome: Action[AnyContent] =
    authenticated.async { implicit request =>
      appointAgentPropertiesSession.get[AppointAgentToSomePropertiesSession].flatMap {
        case Some(AppointAgentToSomePropertiesSession(Some(agent), _)) =>
          Future.successful(Ok(appointAgentSummaryView(action = agent)))
        case _ => errorHandler.notFoundTemplate.map(html => NotFound(html))
      }
    }

  // this endpoint only exists so we don't 404 when changing language after getting an error on submit
  def showAppointAgentSummary(
        agentCode: Long,
        agentAppointed: Option[String],
        backLinkUrl: RedirectUrl,
        fromManageAgentJourney: Boolean
  ): Action[AnyContent] =
    authenticated.async { implicit request =>
      searchForAppointableProperties(
        PaginationParameters(),
        agentCode,
        agentAppointed,
        backLinkUrl,
        fromManageAgentJourney = fromManageAgentJourney
      )
    }

  def appointAgentSummary(
        agentCode: Long,
        agentAppointed: Option[String],
        backLinkUrl: RedirectUrl,
        fromManageAgentJourney: Boolean
  ): Action[AnyContent] =
    authenticated.async { implicit request =>
      appointAgentBulkActionForm
        .bindFromRequest()
        .fold(
          errors =>
            appointAgentPropertiesBadRequest(errors, agentCode, agentAppointed, backLinkUrl, fromManageAgentJourney),
          success = (action: AgentAppointBulkAction) =>
            accounts.withAgentCode(action.agentCode.toString).flatMap {
              case Some(group) =>
                (
                  for {
                    sessionDataOpt <- appointAgentPropertiesSession.get[AppointAgentToSomePropertiesSession]
                    agentListYears <- agentRelationshipService.getMyOrganisationAgents()
                    listYears = agentListYears.agents
                                  .find(_.representativeCode == agentCode)
                                  .flatMap(_.listYears)
                                  .getOrElse(Seq("2017", "2023"))
                                  .toList
                    _ <- agentRelationshipService
                           .postAgentAppointmentChange(
                             AgentAppointmentChangeRequest(
                               agentRepresentativeCode = agentCode,
                               action = AppointmentAction.APPOINT,
                               scope = AppointmentScope.PROPERTY_LIST,
                               propertyLinks = Some(action.propertyLinkIds),
                               listYears = Some(listYears)
                             )
                           )

                    _ <- appointAgentPropertiesSession.saveOrUpdate[AppointAgentToSomePropertiesSession](
                           sessionDataOpt.fold(AppointAgentToSomePropertiesSession(agentAppointAction = Some(action)))(
                             data => data.copy(agentAppointAction = Some(action))
                           )
                         )
                  } yield Redirect(controllers.agentAppointment.routes.AppointAgentController.confirmAppointAgentToSome)
                ) recoverWith {
                  case e: services.AppointRevokeException =>
                    for {
                      agentList <- agentRelationshipService.getMyOrganisationAgents()
                      response <- agentRelationshipService.getMyOrganisationPropertyLinksWithAgentFiltering(
                                    GetPropertyLinksParameters(),
                                    AgentPropertiesParameters(agentCode = action.agentCode),
                                    request.organisationAccount.id,
                                    group.id
                                  )
                    } yield BadRequest(
                      appointAgentPropertiesView(
                        f = Some(appointAgentBulkActionForm.withError("appoint.error", "error.transaction")),
                        model = AppointAgentPropertiesVM(group, response),
                        pagination = PaginationParameters(),
                        params = GetPropertyLinksParameters(),
                        agentCode = action.agentCode,
                        agentAppointed = None,
                        organisationAgents = agentList,
                        backLink = Some(action.backLinkUrl),
                        manageJourneyFlag = true
                      )
                    )
                  case e: Exception => throw e
                }
              case None => notFound
            }
        )
    }

  private def appointAgentPropertiesBadRequest(
        errors: Form[_],
        agentCode: Long,
        agentAppointed: Option[String],
        backLinkUrl: RedirectUrl,
        fromManageAgentJourney: Boolean
  )(implicit request: BasicAuthenticatedRequest[_]) =
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
        } yield BadRequest(
          appointAgentPropertiesView(
            Some(errors),
            AppointAgentPropertiesVM(group, response),
            PaginationParameters(),
            GetPropertyLinksParameters(),
            agentCode,
            agentAppointed,
            agentList,
            backLink = Some(config.safeRedirect(backLinkUrl)),
            manageJourneyFlag = true
          )
        )
      case None => notFound
    }

  def filterProperties(authorisations: Seq[OwnerAuthorisation], agentOrganisaionId: Long): Seq[OwnerAuthorisation] =
    authorisations.filter(auth => auth.agents.map(_.organisationId).contains(agentOrganisaionId))

  def appointAgentBulkActionForm: Form[AgentAppointBulkAction] =
    Form(
      mapping(
        "agentCode"   -> longNumber,
        "name"        -> text,
        "linkIds"     -> list(text).verifying(nonEmptyList),
        "backLinkUrl" -> text
      )(AgentAppointBulkAction.apply)(AgentAppointBulkAction.unapply)
    )

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

case class AppointAgentPropertiesVM(
      agentGroup: GroupAccount,
      response: OwnerAuthResult,
      agentCode: Option[Long] = None,
      showAllProperties: Boolean = false
)

case class FilterAppointPropertiesForm(address: Option[String], agent: Option[String])
