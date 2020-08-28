/*
 * Copyright 2020 HM Revenue & Customs
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
import binders.pagination.PaginationParameters
import binders.propertylinks.{ExternalPropertyLinkManagementSortField, ExternalPropertyLinkManagementSortOrder, GetPropertyLinksParameters}
import config.ApplicationConfig
import connectors._
import controllers._
import form.FormValidation.nonEmptyList
import javax.inject.{Inject, Named}
import models.GroupAccount.AgentGroupAccount
import models._
import models.searchApi.AgentPropertiesFilter.Both
import models.searchApi._
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepo
import services.AgentRelationshipService
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import scala.concurrent.{ExecutionContext, Future}

class AppointAgentController @Inject()(
      val errorHandler: CustomErrorHandler,
      accounts: GroupAccounts,
      authenticated: AuthenticatedAction,
      agentRelationshipService: AgentRelationshipService,
      @Named("appointLinkSession") val propertyLinksSessionRepo: SessionRepo
)(
      implicit override val messagesApi: MessagesApi,
      override val controllerComponents: MessagesControllerComponents,
      executionContext: ExecutionContext,
      val config: ApplicationConfig
) extends PropertyLinkingController {

  val logger: Logger = Logger(this.getClass)

  def getMyOrganisationPropertyLinksWithAgentFiltering(
        pagination: PaginationParameters,
        params: GetPropertyLinksParameters,
        agentCode: Long,
        agentAppointed: Option[String],
        backLink: String
  ): Action[AnyContent] = authenticated.async { implicit request =>
    for {
      agentOrganisation <- accounts.withAgentCode(agentCode.toString)
      response <- agentRelationshipService.getMyOrganisationPropertyLinksWithAgentFiltering(
                   params = params,
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
      _ <- propertyLinksSessionRepo.saveOrUpdate(SessionPropertyLinks(response))
    } yield {
      agentOrganisation match {
        case Some(organisation) =>
          Ok(
            views.html.propertyrepresentation.appoint.appointAgentProperties(
              f = None,
              model = AppointAgentPropertiesVM(
                organisation,
                response,
                Some(agentCode),
                !agentAppointed.contains("NO")
              ),
              pagination = pagination,
              params = params,
              agentCode = agentCode,
              agentAppointed = agentAppointed,
              backLink = Some(backLink)
            ))
        case None =>
          notFound
      }
    }
  }

  def appointAgentSummary(): Action[AnyContent] = authenticated.async { implicit request =>
    appointAgentBulkActionForm
      .bindFromRequest()
      .fold(
        errors => {
          val data: Map[String, String] = errors.data
          accounts.withAgentCode(data("agentCode")).flatMap {
            case Some(group) =>
              for {
                response <- agentRelationshipService.getMyOrganisationPropertyLinksWithAgentFiltering(
                             GetPropertyLinksParameters(),
                             AgentPropertiesParameters(agentCode = data("agentCode").toLong),
                             request.organisationAccount.id,
                             group.id
                           )
              } yield
                BadRequest(views.html.propertyrepresentation.appoint.appointAgentProperties(
                  Some(errors),
                  AppointAgentPropertiesVM(group, response),
                  PaginationParameters(),
                  GetPropertyLinksParameters(),
                  data("agentCode").toLong,
                  data.get("agentAppointed"),
                  backLink = Some(data("backLinkUrl"))
                ))
            case None =>
              Future.successful(notFound)
          }
        },
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
                      views.html.propertyrepresentation.appoint.appointAgentSummary(
                        action = action,
                        agentOrganisation = group.companyName,
                        backLinkUrl = action.backLinkUrl)))
                .recoverWith {
                  case e: services.AppointRevokeException =>
                    for {
                      response <- agentRelationshipService.getMyOrganisationPropertyLinksWithAgentFiltering(
                                   GetPropertyLinksParameters(),
                                   AgentPropertiesParameters(agentCode = action.agentCode),
                                   request.organisationAccount.id,
                                   group.id
                                 )
                    } yield
                      BadRequest(views.html.propertyrepresentation.appoint.appointAgentProperties(
                        f = Some(appointAgentBulkActionForm.withError("appoint.error", "error.transaction")),
                        model = AppointAgentPropertiesVM(group, response),
                        pagination = PaginationParameters(),
                        params = GetPropertyLinksParameters(),
                        agentCode = action.agentCode,
                        agentAppointed = None,
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

  def selectAgentPropertiesSearchSort(
        pagination: PaginationParameters,
        params: GetPropertyLinksParameters,
        agentCode: Long
  ) = authenticated.async { implicit request =>
    accounts.withAgentCode(agentCode.toString).flatMap {
      case Some(group) =>
        for {
          response <- agentRelationshipService
                       .getMyOrganisationsPropertyLinks(
                         GetPropertyLinksParameters(
                           address = params.address,
                           agent = Some(group.companyName),
                           sortfield = params.sortfield,
                           sortorder = params.sortorder),
                         PaginationParams(pagination.startPoint, pagination.pageSize, false)
                       )
                       .map(oar => oar.copy(authorisations = filterProperties(oar.authorisations, group.id)))
                       .map(oar => oar.copy(authorisations = oar.authorisations.take(pagination.pageSize)))
          _ <- propertyLinksSessionRepo.saveOrUpdate(SessionPropertyLinks(response))
        } yield {
          Ok(
            views.html.propertyrepresentation
              .revokeAgentProperties(
                None,
                AppointAgentPropertiesVM(group, response),
                pagination,
                params,
                agentCode,
                agent.routes.ManageAgentController.manageAgent(Some(agentCode)).url))
        }
      case None => Future.successful(NotFound(s"Unknown Agent: $agentCode"))
    }
  }

  def revokeAgentSummary() = authenticated.async { implicit request =>
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
                             .getMyOrganisationsPropertyLinks(
                               GetPropertyLinksParameters(
                                 address = pagination.address,
                                 agent = Some(group.companyName),
                                 sortfield = ExternalPropertyLinkManagementSortField.withName(
                                   pagination.sortField.name.toUpperCase),
                                 sortorder = ExternalPropertyLinkManagementSortOrder.withName(
                                   pagination.sortOrder.name.toUpperCase)
                               ),
                               PaginationParams(pagination.startPoint, pagination.pageSize, false)
                             )
                             .map(oar => oar.copy(authorisations = filterProperties(oar.authorisations, group.id)))
                             .map(oar => oar.copy(filterTotal = oar.authorisations.size))
                             .map(oar => oar.copy(authorisations = oar.authorisations.take(pagination.pageSize)))
              } yield {
                BadRequest(views.html.propertyrepresentation.revokeAgentProperties(
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
                .map {
                  case _ => Ok(views.html.propertyrepresentation.revokeAgentSummary(action, group.companyName))
                }
                .recoverWith {
                  case e: services.AppointRevokeException =>
                    for {
                      response <- agentRelationshipService
                                   .getMyOrganisationsPropertyLinks(
                                     GetPropertyLinksParameters(agent = Some(group.companyName)),
                                     DefaultPaginationParams)
                                   .map(oar =>
                                     oar.copy(authorisations = filterProperties(oar.authorisations, group.id)))
                                   .map(oar => oar.copy(filterTotal = oar.authorisations.size))
                                   .map(oar =>
                                     oar.copy(
                                       authorisations = oar.authorisations.take(DefaultPaginationParams.pageSize)))
                    } yield
                      BadRequest(views.html.propertyrepresentation.revokeAgentProperties(
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

  def filterProperties(authorisations: Seq[OwnerAuthorisation], agentId: Long): Seq[OwnerAuthorisation] =
    authorisations.filter(auth => auth.agents.map(_.organisationId).contains(agentId))

  def appointAgentBulkActionForm =
    Form(
      mapping(
        "agentCode"   -> longNumber,
        "linkIds"     -> list(text).verifying(nonEmptyList),
        "backLinkUrl" -> text
      )(AgentAppointBulkAction.apply)(AgentAppointBulkAction.unpack))

  def revokeAgentBulkActionForm =
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
