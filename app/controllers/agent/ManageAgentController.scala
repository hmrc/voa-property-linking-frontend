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

package controllers.agent

import actions.AuthenticatedAction
import binders.pagination.PaginationParameters
import binders.propertylinks.GetPropertyLinksParameters
import config.ApplicationConfig
import controllers.{PaginationParams, PropertyLinkingController}
import models.propertyrepresentation.AgentAppointmentChangesRequest.submitAgentAppointmentRequest
import models.propertyrepresentation._
import models.searchApi.AgentPropertiesFilter.Both
import models.searchApi.OwnerAuthResult
import play.api.Logger
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import repositories.SessionRepo
import services.AgentRelationshipService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class ManageAgentController @Inject() (
      val errorHandler: CustomErrorHandler,
      authenticated: AuthenticatedAction,
      agentRelationshipService: AgentRelationshipService,
      manageAgentView: views.html.propertyrepresentation.manage.manageAgent,
      myAgentsView: views.html.propertyrepresentation.manage.myAgents,
      removeAgentFromOrganisationView: views.html.propertyrepresentation.manage.removeAgentFromOrganisation,
      unassignAgentFromPropertyView: views.html.propertyrepresentation.manage.unassignAgentFromProperty,
      addAgentToAllPropertiesView: views.html.propertyrepresentation.manage.addAgentToAllProperties,
      confirmAddAgentToAllPropertiesView: views.html.propertyrepresentation.manage.confirmAddAgentToAllProperties,
      unassignAgentFromAllPropertiesView: views.html.propertyrepresentation.manage.unassignAgentFromAllProperties,
      confirmUnassignAgentFromAllPropertiesView: views.html.propertyrepresentation.manage.confirmUnassignAgentFromAllProperties,
      confirmRemoveAgentFromOrganisationView: views.html.propertyrepresentation.manage.confirmRemoveAgentFromOrganisation,
      manageAgentPropertiesView: views.html.propertyrepresentation.manage.manageAgentProperties,
      @Named("manageAgent") val manageAgentSessionRepo: SessionRepo
)(implicit
      override val messagesApi: MessagesApi,
      override val controllerComponents: MessagesControllerComponents,
      executionContext: ExecutionContext,
      val config: ApplicationConfig
) extends PropertyLinkingController {

  val logger = Logger(this.getClass.getName)

  def showAgents: Action[AnyContent] =
    authenticated.async { implicit request =>
      for {
        organisationsAgents <- agentRelationshipService.getMyOrganisationAgents()
        propertyLinksCount  <- agentRelationshipService.getMyOrganisationPropertyLinksCount()
      } yield Ok(myAgentsView(organisationsAgents.agents, propertyLinksCount))
    }

  def manageAgentProperties(
        agentCode: Long,
        params: GetPropertyLinksParameters = GetPropertyLinksParameters(),
        propertyLinkId: Option[Long] = None,
        valuationId: Option[Long] = None,
        propertyLinkSubmissionId: Option[String] = None
  ): Action[AnyContent] =
    authenticated.async { implicit request =>
      for {
        ownerAuthResult <- agentRelationshipService
                             .getMyAgentPropertyLinks(agentCode, params, PaginationParams(1, 100, true))
        agentDetails <- agentRelationshipService.getAgentNameAndAddress(agentCode)
        myAgents     <- agentRelationshipService.getMyOrganisationAgents()
        listYears = myAgents.agents
                      .find(_.representativeCode == agentCode)
                      .flatMap(_.listYears)
                      .getOrElse(Seq.empty[String])
        backLink = (propertyLinkId, valuationId, propertyLinkSubmissionId, myAgents.resultCount) match {
                     case (None, None, None, 1) => config.dashboardUrl("home")
                     case (Some(linkId), Some(valId), Some(submissionId), _) =>
                       config.businessRatesValuationFrontendUrl(
                         s"property-link/$linkId/valuations/$valId?submissionId=$submissionId#agents-tab"
                       )
                     case (None, Some(valId), Some(submissionId), _) =>
                       controllers.detailedvaluationrequest.routes.DvrController
                         .myOrganisationRequestDetailValuationCheck(
                           propertyLinkSubmissionId = submissionId,
                           valuationId = valId,
                           tabName = Some("agents-tab")
                         )
                         .url
                     case _ => controllers.agent.routes.ManageAgentController.showAgents.url
                   }
      } yield Ok(manageAgentPropertiesView(ownerAuthResult, agentCode, agentDetails, listYears, backLink))
    }

  def startManageAgent(agentCode: Long): Action[AnyContent] =
    authenticated.async { implicit request =>
      for {
        organisationsAgents <- agentRelationshipService.getMyOrganisationAgents()
        agentToBeManagedOpt: Option[AgentSummary] = organisationsAgents.agents match {
                                                      case agent :: Nil => Some(agent)
                                                      case Nil          => None
                                                      case agents => agents.find(a => a.representativeCode == agentCode)
                                                    }
        _ <- agentToBeManagedOpt match {
               case None        => Future.successful(NotFound(errorHandler.notFoundErrorTemplate))
               case Some(agent) => manageAgentSessionRepo.start[AgentSummary](agent)
             }

      } yield Redirect(controllers.agent.routes.ManageAgentController.showManageAgent)
    }

  def showManageAgent: Action[AnyContent] =
    authenticated.async { implicit request =>
      getManageAgentView().flatMap {
        case None       => errorHandler.notFoundTemplate.map(html => NotFound(html))
        case Some(page) => Future.successful(Ok(page))
      }
    }

  private[agent] def getManageAgentView(
        submitManageAgentForm: Form[ManageAgentRequest] = ManageAgentRequest.submitManageAgentRequest
  )(implicit request: Request[_]) =
    for {
      agentToBeManagedOpt: Option[AgentSummary] <- manageAgentSessionRepo.get[AgentSummary]
      organisationsAgents                       <- agentRelationshipService.getMyOrganisationAgents()
      propertyLinks: OwnerAuthResult <-
        agentRelationshipService.getMyOrganisationsPropertyLinks(
          searchParams = GetPropertyLinksParameters(),
          pagination = PaginationParams(startPoint = 1, pageSize = 100, requestTotalRowCount = false)
        )
      ipPropertyLinksCount = propertyLinks.total
    } yield agentToBeManagedOpt match {
      case None => None
      case Some(agent) =>
        val ManageAgentOptionItemList =
          ipPropertyLinksCount match {
            case 0 if agent.propertyCount == 0 =>
              List(
                ManageAgentOptionItem(ChangeRatingList),
                ManageAgentOptionItem(RemoveFromYourAccount)
              )
            case 1 if agent.propertyCount == 0 =>
              List(
                ManageAgentOptionItem(AssignToYourProperty),
                ManageAgentOptionItem(ChangeRatingList),
                ManageAgentOptionItem(RemoveFromYourAccount)
              )
            case 1 if agent.propertyCount == 1 =>
              List(
                ManageAgentOptionItem(UnassignFromYourProperty),
                ManageAgentOptionItem(ChangeRatingList)
              )
            case numberOfPropertyLinks if numberOfPropertyLinks > 1 && agent.propertyCount == 0 =>
              List(
                ManageAgentOptionItem(AssignToAllProperties),
                ManageAgentOptionItem(AssignToOneOrMoreProperties),
                ManageAgentOptionItem(ChangeRatingList),
                ManageAgentOptionItem(RemoveFromYourAccount)
              )
            case numberOfPropertyLinks if numberOfPropertyLinks > agent.propertyCount =>
              List(
                ManageAgentOptionItem(AssignToAllProperties),
                ManageAgentOptionItem(AssignToOneOrMoreProperties),
                ManageAgentOptionItem(UnassignFromAllProperties),
                ManageAgentOptionItem(UnassignFromOneOrMoreProperties),
                ManageAgentOptionItem(ChangeRatingList)
              )
            case numberOfPropertyLinks if numberOfPropertyLinks == agent.propertyCount =>
              List(
                ManageAgentOptionItem(UnassignFromAllProperties),
                ManageAgentOptionItem(UnassignFromOneOrMoreProperties),
                ManageAgentOptionItem(ChangeRatingList)
              )
          }
        Some(
          manageAgentView(
            submitManageAgentForm,
            ManageAgentOptionItemList,
            agent,
            calculateBackLink(organisationsAgents, agent.representativeCode)
          )
        )
    }

  def submitManageAgent(agentCode: Long): Action[AnyContent] =
    authenticated.async { implicit request =>
      ManageAgentRequest.submitManageAgentRequest
        .bindFromRequest()
        .fold(
          errors =>
            getManageAgentView(errors).flatMap {
              case None       => errorHandler.notFoundTemplate.map(html => NotFound(html))
              case Some(page) => Future.successful(BadRequest(page))
            },
          success =>
            success.manageAgentOption match {
              case AssignToSomeProperties | AssignToOneOrMoreProperties =>
                Future.successful(joinOldAgentAppointJourney(agentCode))
              case AssignToAllProperties | AssignToYourProperty =>
                Future.successful(Redirect(controllers.agent.routes.ManageAgentController.showAssignToAll))
              case UnassignFromAllProperties =>
                Future.successful(Redirect(controllers.agent.routes.ManageAgentController.showUnassignFromAll))
              case UnassignFromSomeProperties | UnassignFromOneOrMoreProperties =>
                Future.successful(joinOldAgentRevokeJourney(agentCode))
              case RemoveFromYourAccount =>
                Future.successful(
                  Redirect(controllers.agent.routes.ManageAgentController.showRemoveAgentFromIpOrganisation)
                )
              case ChangeRatingList =>
                Future.successful(Redirect(controllers.manageAgent.routes.ChooseRatingListController.show))
              case UnassignFromYourProperty =>
                Future.successful(Redirect(controllers.agent.routes.ManageAgentController.showUnassignFromAll))
            }
        )
    }

  def showAssignToAll: Action[AnyContent] =
    authenticated.async { implicit request =>
      for {
        agent     <- getCachedAgent
        linkCount <- agentRelationshipService.getMyOrganisationPropertyLinksCount()
      } yield Ok(
        addAgentToAllPropertiesView(
          submitAgentAppointmentRequest,
          agent.name,
          agent.representativeCode,
          multiplePropertyLinks = linkCount > 1
        )
      )
    }

  def showUnassignFromAll: Action[AnyContent] =
    authenticated.async { implicit request =>
      for {
        agent               <- getCachedAgent
        organisationsAgents <- agentRelationshipService.getMyOrganisationAgents()
        linkCount           <- agentRelationshipService.getMyOrganisationPropertyLinksCount()
      } yield
        if (linkCount == 1)
          Ok(
            unassignAgentFromPropertyView(
              submitAgentAppointmentRequest,
              agent,
              calculateBackLink(organisationsAgents, agent.representativeCode)
            )
          )
        else
          Ok(unassignAgentFromAllPropertiesView(submitAgentAppointmentRequest, agent.name, agent.representativeCode))
    }

  private def getCachedAgent(implicit hc: HeaderCarrier) =
    manageAgentSessionRepo.get[AgentSummary].map {
      case Some(agent) => agent
      case None        => throw new IllegalStateException("no agent data cached")
    }

  def assignAgentToAll(agentCode: Long, agentName: String): Action[AnyContent] =
    authenticated.async { implicit request =>
      submitAgentAppointmentRequest
        .bindFromRequest()
        .fold(
          errors =>
            agentRelationshipService
              .getMyOrganisationPropertyLinksCount()
              .map(linkCount =>
                BadRequest(
                  addAgentToAllPropertiesView(errors, agentName, agentCode, multiplePropertyLinks = linkCount > 1)
                )
              ),
          success =>
            for {
              agentListYears <- agentRelationshipService.getMyOrganisationAgents()
              listYears = agentListYears.agents
                            .find(_.representativeCode == agentCode)
                            .flatMap(_.listYears)
                            .getOrElse(Seq("2017", "2023"))
                            .toList
              _ <- agentRelationshipService
                     .postAgentAppointmentChange(
                       AgentAppointmentChangeRequest(
                         action = AppointmentAction.APPOINT,
                         scope = AppointmentScope.ALL_PROPERTIES,
                         agentRepresentativeCode = success.agentRepresentativeCode,
                         propertyLinks = None,
                         listYears = Some(listYears)
                       )
                     )
            } yield Redirect(controllers.agent.routes.ManageAgentController.confirmAssignAgentToAll)
        )
    }

  def unassignAgentFromAll(agentCode: Long, agentName: String): Action[AnyContent] =
    authenticated.async { implicit request =>
      submitAgentAppointmentRequest
        .bindFromRequest()
        .fold(
          errors => Future.successful(BadRequest(unassignAgentFromAllPropertiesView(errors, agentName, agentCode))),
          success =>
            agentRelationshipService
              .postAgentAppointmentChange(
                AgentAppointmentChangeRequest(
                  agentRepresentativeCode = success.agentRepresentativeCode,
                  action = AppointmentAction.REVOKE,
                  scope = AppointmentScope.ALL_PROPERTIES,
                  propertyLinks = None,
                  listYears = None
                )
              )
              .map { _ =>
                Redirect(controllers.agent.routes.ManageAgentController.confirmationUnassignAgentFromAll.url)
              }
        )
    }

  def confirmationUnassignAgentFromAll: Action[AnyContent] =
    authenticated.async { implicit request =>
      for {
        agent     <- getCachedAgent
        linkCount <- agentRelationshipService.getMyOrganisationPropertyLinksCount()
      } yield Ok(
        confirmUnassignAgentFromAllPropertiesView(
          agent.name,
          agent.representativeCode,
          multiplePropertyLinks = linkCount > 1
        )
      )
    }

  def confirmAssignAgentToAll: Action[AnyContent] =
    authenticated.async { implicit request =>
      for {
        agent     <- getCachedAgent
        linkCount <- agentRelationshipService.getMyOrganisationPropertyLinksCount()
      } yield Ok(confirmAddAgentToAllPropertiesView(agent.name, multiplePropertyLinks = linkCount > 1))
    }

  def showRemoveAgentFromIpOrganisation: Action[AnyContent] =
    authenticated.async { implicit request =>
      getCachedAgent.map { agent =>
        Ok(
          removeAgentFromOrganisationView(
            form = submitAgentAppointmentRequest,
            agentCode = agent.representativeCode,
            agentName = agent.name,
            backLink = controllers.agent.routes.ManageAgentController.showManageAgent.url
          )
        )
      }
    }

  def removeAgentFromIpOrganisation(agentCode: Long, agentName: String, backLinkUrl: RedirectUrl): Action[AnyContent] =
    authenticated.async { implicit request =>
      submitAgentAppointmentRequest
        .bindFromRequest()
        .fold(
          errors =>
            Future.successful(
              BadRequest(
                removeAgentFromOrganisationView(errors, agentCode, agentName, config.safeRedirect(backLinkUrl))
              )
            ),
          success =>
            agentRelationshipService
              .postAgentAppointmentChange(
                AgentAppointmentChangeRequest(
                  agentRepresentativeCode = success.agentRepresentativeCode,
                  action = AppointmentAction.REVOKE,
                  scope = AppointmentScope.RELATIONSHIP,
                  propertyLinks = None,
                  listYears = None
                )
              )
              .map(_ => Redirect(controllers.agent.routes.ManageAgentController.confirmRemoveAgentFromOrganisation))
        )
    }

  def confirmRemoveAgentFromOrganisation: Action[AnyContent] =
    authenticated.async { implicit request =>
      getCachedAgent.map { agent =>
        Ok(confirmRemoveAgentFromOrganisationView(agent.name, agent.representativeCode))
      }
    }

  private def calculateBackLink(organisationsAgents: AgentList, agentCode: Long) =
    if (organisationsAgents.resultCount >= 1)
      controllers.agent.routes.ManageAgentController
        .manageAgentProperties(agentCode, GetPropertyLinksParameters())
        .url
    else config.dashboardUrl("home")

  private def joinOldAgentAppointJourney(agentCode: Long) =
    Redirect(
      controllers.agentAppointment.routes.AppointAgentController.getMyOrganisationPropertyLinksWithAgentFiltering(
        pagination = PaginationParameters(),
        agentCode = agentCode,
        agentAppointed = Some(Both.name),
        backLinkUrl = RedirectUrl(controllers.agent.routes.ManageAgentController.showManageAgent.url),
        fromManageAgentJourney = true
      )
    )

  private def joinOldAgentRevokeJourney(agentCode: Long) =
    Redirect(
      controllers.agentAppointment.routes.AppointAgentController.selectAgentPropertiesSearchSort(
        pagination = PaginationParameters(),
        agentCode = agentCode
      )
    )
}
