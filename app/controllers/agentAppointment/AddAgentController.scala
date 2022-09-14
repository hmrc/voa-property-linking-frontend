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

class AddAgentController @Inject()(
      val errorHandler: CustomErrorHandler,
      authenticated: AuthenticatedAction,
      withAppointAgentSession: WithAppointAgentSessionRefiner,
      agentRelationshipService: AgentRelationshipService,
      @Named("appointNewAgentSession") val sessionRepo: SessionRepo,
      startPageView: views.html.propertyrepresentation.appoint.start,
      isTheCorrectAgentView: views.html.propertyrepresentation.appoint.isThisYourAgent,
      agentToManageOnePropertyView: views.html.propertyrepresentation.appoint.agentToManageOneProperty,
      agentToManageMultiplePropertiesView: views.html.propertyrepresentation.appoint.agentToManageMultipleProperties,
      checkYourAnswersView: views.html.propertyrepresentation.appoint.checkYourAnswers,
      confirmationView: views.html.propertyrepresentation.appoint.confirmation)(
      implicit override val messagesApi: MessagesApi,
      override val controllerComponents: MessagesControllerComponents,
      executionContext: ExecutionContext,
      val config: ApplicationConfig
) extends PropertyLinkingController {

  def start(
        propertyLinkId: Option[Long] = None,
        valuationId: Option[Long] = None,
        propertyLinkSubmissionId: Option[String] = None,
        uarn: Option[Long] = None): Action[AnyContent] = authenticated.async { implicit request =>
    val backLink = (propertyLinkId, valuationId, propertyLinkSubmissionId, uarn) match {
      case (Some(linkId), Some(valId), Some(submissionId), None) =>
        config.businessRatesValuationFrontendUrl(
          s"property-link/$linkId/valuations/$valId?submissionId=$submissionId#agents-tab")
      case (None, Some(valId), Some(submissionId), Some(u)) =>
        s"${controllers.detailedvaluationrequest.routes.DvrController.myOrganisationRequestDetailValuationCheck(propertyLinkSubmissionId = submissionId, valuationId = valId, uarn = u).url}#agents-tab"
      case _ => config.dashboardUrl("home")
    }
    sessionRepo
      .start[Start](Start(backLink = Some(backLink)))
      .map(_ => Redirect(controllers.agentAppointment.routes.AddAgentController.showStartPage()))
  }

  def showStartPage(): Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async { implicit request =>
    Future.successful(Ok(startPageView(agentCode, getBackLink)))
  }

  def getAgentDetails(): Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async { implicit request =>
    agentCode.bindFromRequest.fold(
      errors => Future.successful(BadRequest(startPageView(errors, getBackLink))),
      success => {
        Try(success.toLong).toOption match {
          case None =>
            Future.successful(
              BadRequest(
                startPageView(
                  agentCode.copy(
                    data = Map("agentCode" -> success),
                    errors = Seq[FormError](FormError(key = "agentCode", message = "error.agentCode.required"))),
                  getBackLink
                )))
          case Some(representativeCode) =>
            for {
              organisationsAgents <- agentRelationshipService.getMyOrganisationAgents()
              agentNameAndAddressOpt <- agentRelationshipService.getAgentNameAndAddress(success.toLong) recoverWith {
                                         case b: BadRequestException => Future.successful(None)
                                       }
            } yield {
              agentNameAndAddressOpt match {
                case None =>
                  BadRequest(
                    startPageView(
                      agentCode.copy(
                        data = Map("agentCode" -> s"$representativeCode"),
                        errors = Seq[FormError](
                          FormError(key = "agentCode", message = "error.propertyRepresentation.unknownAgent"))),
                      getBackLink
                    ))
                case Some(_) if organisationsAgents.agents.exists(a => a.representativeCode == representativeCode) =>
                  BadRequest(
                    startPageView(
                      agentCode.copy(
                        data = Map("agentCode" -> s"$representativeCode"),
                        errors = Seq[FormError](
                          FormError(key = "agentCode", message = "error.propertyRepresentation.agentAlreadyAppointed"))
                      ),
                      getBackLink
                    ))
                case Some(agent) =>
                  sessionRepo.saveOrUpdate(
                    SearchedAgent(
                      agentCode = representativeCode,
                      agentOrganisationName = agent.name,
                      agentAddress = agent.address,
                      backLink = request.sessionData.backLink
                    ))
                  Redirect(controllers.agentAppointment.routes.AddAgentController.isCorrectAgent())
              }
            }

        }
      }
    )
  }

  def isCorrectAgent(): Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async { implicit request =>
    Future.successful(Ok(isTheCorrectAgentView(isThisTheCorrectAgent, request.agentDetails)))
  }

  def agentSelected(): Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async { implicit request =>
    isThisTheCorrectAgent.bindFromRequest.fold(
      errors => {
        Future.successful(BadRequest(isTheCorrectAgentView(errors, request.agentDetails)))
      },
      success => {
        if (success) {
          for {
            searchedAgentOpt <- sessionRepo.get[SearchedAgent]
            searchedAgent = searchedAgentOpt.getOrElse(throw NoAgentSavedException("no agent saved"))
            _ <- sessionRepo.saveOrUpdate(SelectedAgent(searchedAgent, success))
            propertyLinks <- agentRelationshipService.getMyOrganisationPropertyLinksWithAgentFiltering(
                              params = GetPropertyLinksParameters(),
                              pagination = AgentPropertiesParameters(agentCode = searchedAgent.agentCode),
                              agentOrganisationId = searchedAgent.agentCode,
                              organisationId = request.organisationId
                            )
          } yield {
            propertyLinks.authorisations.size match {
              case 0 =>
                agentRelationshipService.assignAgent(
                  AgentAppointmentChangesRequest(
                    scope = AppointmentScope.RELATIONSHIP.toString,
                    agentRepresentativeCode = searchedAgent.agentCode
                  )
                )
                Ok(confirmationView(request.agentDetails.name, None))
              case 1 => Redirect(controllers.agentAppointment.routes.AddAgentController.oneProperty())
              case _ => Redirect(controllers.agentAppointment.routes.AddAgentController.multipleProperties())
            }
          }
        } else {
          Future.successful(Redirect(controllers.agentAppointment.routes.AddAgentController.showStartPage()))
        }
      }
    )
  }

  def oneProperty(): Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async { implicit request =>
    Future.successful(Ok(agentToManageOnePropertyView(manageOneProperty, request.agentDetails.name)))
  }

  def submitOneProperty(): Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async {
    implicit request =>
      manageOneProperty.bindFromRequest.fold(
        errors => {
          Future.successful(BadRequest(agentToManageOnePropertyView(errors, request.agentDetails.name)))
        },
        success => {
          for {
            selectedAgentOpt <- sessionRepo.get[SelectedAgent]
            selectedAgent = selectedAgentOpt.getOrElse(throw NoAgentSavedException("no agent saved"))
            _ <- sessionRepo.saveOrUpdate(
                  ManagingProperty(selectedAgent = selectedAgent, selection = success.name, singleProperty = true))
          } yield {
            Redirect(controllers.agentAppointment.routes.AddAgentController.checkAnswers())
          }
        }
      )
  }

  def multipleProperties(): Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async {
    implicit request =>
      Future.successful(Ok(agentToManageMultiplePropertiesView(manageMultipleProperties, request.agentDetails.name)))
  }

  def submitMultipleProperties(): Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async {
    implicit request =>
      manageMultipleProperties.bindFromRequest.fold(
        errors => {
          Future.successful(BadRequest(agentToManageMultiplePropertiesView(errors, request.agentDetails.name)))
        },
        success => {
          for {
            selectedAgentOpt <- sessionRepo.get[SelectedAgent]
            selectedAgent = selectedAgentOpt.getOrElse(throw NoAgentSavedException("no agent saved"))
            _ <- sessionRepo.saveOrUpdate(
                  ManagingProperty(selectedAgent = selectedAgent, selection = success.name, singleProperty = false))
          } yield {
            success match {
              case ChooseFromList => joinOldJourney(selectedAgent.agentCode)
              case propertyrepresentation.All | propertyrepresentation.NoProperties =>
                Redirect(controllers.agentAppointment.routes.AddAgentController.checkAnswers())
            }
          }
        }
      )
  }

  def checkAnswers(): Action[AnyContent] = authenticated.andThen(withAppointAgentSession) { implicit request =>
    PartialFunction
      .condOpt(request.sessionData) {
        case data: ManagingProperty =>
          Ok(checkYourAnswersView(submitAgentAppointmentRequest, data))
      }
      .getOrElse(NotFound(errorHandler.notFoundTemplate))
  }

  def appointAgent(): Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async { implicit request =>
    submitAgentAppointmentRequest.bindFromRequest.fold(
      errors => {
        PartialFunction
          .condOpt(request.sessionData) {
            case data: ManagingProperty =>
              Future.successful(BadRequest(checkYourAnswersView(errors, data)))
          }
          .getOrElse(Future.successful(NotFound(errorHandler.notFoundTemplate)))
      }, { success =>
        {
          agentRelationshipService
            .assignAgent(success)
            .map { _ =>
              val key =
                if (success.scope == AppointmentScope.RELATIONSHIP.toString)
                  None
                else Some("propertyRepresentation.confirmation.whatHappensNext.allOrSome")
              Ok(confirmationView(agentName = request.agentDetails.name, guidanceMessageKey = key))
            }
        }
      }
    )
  }

  private def getBackLink(implicit request: AppointAgentSessionRequest[AnyContent]) =
    request.sessionData.backLink.getOrElse(config.dashboardUrl("home"))

  private def joinOldJourney(agentCode: Long) =
    Redirect(
      controllers.agentAppointment.routes.AppointAgentController.getMyOrganisationPropertyLinksWithAgentFiltering(
        pagination = PaginationParameters(),
        agentCode = agentCode,
        agentAppointed = Some(Both.name),
        backLink = controllers.agentAppointment.routes.AddAgentController.multipleProperties.url
      ))

}

case class NoAgentSavedException(message: String) extends Exception(message)
