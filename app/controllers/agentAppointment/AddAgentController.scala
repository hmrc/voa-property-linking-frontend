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
import binders.pagination.PaginationParameters
import binders.propertylinks.GetPropertyLinksParameters
import config.ApplicationConfig
import controllers.PropertyLinkingController
import controllers.agentAppointment.AppointNewAgentForms._
import models.propertyrepresentation
import models.propertyrepresentation._
import models.searchApi.AgentPropertiesFilter.Both
import models.searchApi.AgentPropertiesParameters
import play.api.data.FormError
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepo
import services.AgentRelationshipService
import uk.gov.hmrc.http.BadRequestException
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class AddAgentController @Inject() (
      val errorHandler: CustomErrorHandler,
      authenticated: AuthenticatedAction,
      withAppointAgentSession: WithAppointAgentSessionRefiner,
      agentRelationshipService: AgentRelationshipService,
      @Named("appointNewAgentSession") val sessionRepo: SessionRepo,
      startPageView: views.html.propertyrepresentation.appoint.start,
      agentCodeView: views.html.propertyrepresentation.appoint.agentCode,
      isTheCorrectAgentView: views.html.propertyrepresentation.appoint.isThisYourAgent,
      agentToManageOnePropertyView: views.html.propertyrepresentation.appoint.agentToManageOneProperty,
      agentToManageMultiplePropertiesView: views.html.propertyrepresentation.appoint.agentToManageMultipleProperties
)(implicit
      override val messagesApi: MessagesApi,
      override val controllerComponents: MessagesControllerComponents,
      executionContext: ExecutionContext,
      val config: ApplicationConfig
) extends PropertyLinkingController {

  def start(
        propertyLinkId: Option[Long] = None,
        valuationId: Option[Long] = None,
        propertyLinkSubmissionId: Option[String] = None
  ): Action[AnyContent] =
    authenticated.async { implicit request =>
      val backLink = (propertyLinkId, valuationId, propertyLinkSubmissionId) match {
        case (Some(linkId), Some(valId), Some(submissionId)) =>
          config.businessRatesValuationFrontendUrl(
            s"property-link/$linkId/valuations/$valId?submissionId=$submissionId#agents-tab"
          )
        case (None, Some(valId), Some(submissionId)) =>
          controllers.detailedvaluationrequest.routes.DvrController
            .myOrganisationRequestDetailValuationCheck(
              propertyLinkSubmissionId = submissionId,
              valuationId = valId,
              tabName = Some("agents-tab")
            )
            .url
        case _ => config.dashboardUrl("home")
      }
      sessionRepo
        .start[Start](Start(backLink = Some(backLink)))
        .map(_ => Redirect(routes.AddAgentController.showStartPage))
    }

  def showStartPage: Action[AnyContent] =
    authenticated.andThen(withAppointAgentSession).async { implicit request =>
      Future.successful(Ok(startPageView(config.dashboardUrl("home"))))
    }

  def showAgentCodePage(fromCyaChange: Boolean = false): Action[AnyContent] =
    authenticated.andThen(withAppointAgentSession).async { implicit request =>
      val backLink =
        if (fromCyaChange) routes.CheckYourAnswersController.onPageLoad().url
        else routes.AddAgentController.showStartPage.url
      for {
        agentDetailsOpt <- sessionRepo.get[AppointNewAgentSession]
      } yield agentDetailsOpt match {
        case Some(answers) =>
          answers match {
            case answers: ManagingProperty =>
              Ok(agentCodeView(agentCode.fill(answers.agentCode.toString), backLink = backLink))
            case _ =>
              Ok(agentCodeView(agentCode, backLink))
          }
        case _ =>
          Ok(agentCodeView(agentCode, backLink))
      }
    }

  def getAgentDetails(backLinkUrl: RedirectUrl): Action[AnyContent] =
    authenticated.andThen(withAppointAgentSession).async { implicit request =>
      val backLink = config.safeRedirect(backLinkUrl)
      val fromCyaChange = if (backLink == routes.CheckYourAnswersController.onPageLoad().url) true else false
      agentCode
        .bindFromRequest()
        .fold(
          errors => Future.successful(BadRequest(agentCodeView(errors, backLink))),
          success =>
            Try(success.toLong).toOption match {
              case None =>
                Future.successful(
                  BadRequest(
                    agentCodeView(
                      agentCode.copy(
                        data = Map("agentCode" -> success),
                        errors = Seq[FormError](FormError(key = "agentCode", message = "error.agentCode.required"))
                      ),
                      backLink
                    )
                  )
                )
              case Some(representativeCode) =>
                {
                  for {
                    organisationsAgents <- agentRelationshipService.getMyOrganisationAgents()
                    agentNameAndAddressOpt <- agentRelationshipService
                                                .getAgentNameAndAddress(success.toLong)
                                                .recoverWith { case b: BadRequestException =>
                                                  Future.successful(None)
                                                }
                  } yield agentNameAndAddressOpt match {
                    case None =>
                      Future.successful(
                        BadRequest(
                          agentCodeView(
                            agentCode.copy(
                              data = Map("agentCode" -> s"$representativeCode"),
                              errors = Seq[FormError](
                                FormError(key = "agentCode", message = "error.propertyRepresentation.unknownAgent")
                              )
                            ),
                            backLink
                          )
                        )
                      )
                    case Some(_)
                        if organisationsAgents.agents.exists(a => a.representativeCode == representativeCode) =>
                      Future.successful(
                        BadRequest(
                          agentCodeView(
                            agentCode.copy(
                              data = Map("agentCode" -> s"$representativeCode"),
                              errors = Seq[FormError](
                                FormError(
                                  key = "agentCode",
                                  message = "error.propertyRepresentation.agentAlreadyAppointed"
                                )
                              )
                            ),
                            backLink
                          )
                        )
                      )
                    case Some(agent) =>
                      sessionRepo
                        .get[AppointNewAgentSession]
                        .map {
                          case Some(sessionData) =>
                            sessionData match {
                              case answers: ManagingProperty =>
                                sessionRepo
                                  .saveOrUpdate(
                                    answers.copy(
                                      agentCode = representativeCode,
                                      agentOrganisationName = agent.name,
                                      agentAddress = agent.address,
                                      backLink = Some(backLink)
                                    )
                                  )
                                  .map(_ =>
                                    Redirect(
                                      controllers.agentAppointment.routes.AddAgentController
                                        .isCorrectAgent(fromCyaChange)
                                    )
                                  )
                              case _ =>
                                sessionRepo
                                  .saveOrUpdate(
                                    SearchedAgent(
                                      agentCode = representativeCode,
                                      agentOrganisationName = agent.name,
                                      agentAddress = agent.address,
                                      backLink = Some(backLink)
                                    )
                                  )
                                  .map(_ =>
                                    Redirect(
                                      controllers.agentAppointment.routes.AddAgentController
                                        .isCorrectAgent(fromCyaChange)
                                    )
                                  )
                            }
                          case _ =>
                            sessionRepo
                              .saveOrUpdate(
                                SearchedAgent(
                                  agentCode = representativeCode,
                                  agentOrganisationName = agent.name,
                                  agentAddress = agent.address,
                                  backLink = Some(backLink)
                                )
                              )
                              .map(_ =>
                                Redirect(
                                  controllers.agentAppointment.routes.AddAgentController.isCorrectAgent(fromCyaChange)
                                )
                              )
                        }
                        .flatten
                  }
                }.flatten
            }
        )
    }

  def isCorrectAgent(fromCyaChange: Boolean = false): Action[AnyContent] =
    authenticated.andThen(withAppointAgentSession).async { implicit request =>
      Future.successful(
        Ok(
          isTheCorrectAgentView(
            isThisTheCorrectAgent,
            request.agentDetails,
            routes.AddAgentController.getAgentDetails(backLinkUrl = RedirectUrl(getBackLinkFromSession)).url,
            fromCyaChange
          )
        )
      )
    }

  def agentSelected(backLinkUrl: RedirectUrl, fromCyaChange: Boolean = false): Action[AnyContent] = {
    val backLink = config.safeRedirect(backLinkUrl)
    authenticated.andThen(withAppointAgentSession).async { implicit request =>
      isThisTheCorrectAgent
        .bindFromRequest()
        .fold(
          errors =>
            Future.successful(BadRequest(isTheCorrectAgentView(errors, request.agentDetails, backLink, fromCyaChange))),
          success =>
            if (success) {
              for {
                searchedAgentOpt <- sessionRepo.get[SearchedAgent]
                searchedAgent = searchedAgentOpt.getOrElse(throw NoAgentSavedException("no agent saved"))
                propertyLinks <- agentRelationshipService.getMyOrganisationPropertyLinksWithAgentFiltering(
                                   params = GetPropertyLinksParameters(),
                                   pagination = AgentPropertiesParameters(agentCode = searchedAgent.agentCode),
                                   agentOrganisationId = searchedAgent.agentCode,
                                   organisationId = request.organisationId
                                 )
              } yield sessionRepo
                .get[AppointNewAgentSession]
                .map { case Some(sessionData) =>
                  sessionData match {
                    case answers: ManagingProperty if fromCyaChange =>
                      sessionRepo.saveOrUpdate(
                        answers.copy(
                          backLink = Some(
                            getBacklinkForCheckAnswersPage(
                              propertyLinks.authorisations.size,
                              answers.ratingLists
                            )
                          )
                        )
                      )
                      Future.successful(Redirect(routes.CheckYourAnswersController.onPageLoad()))
                    case _ =>
                      sessionRepo.saveOrUpdate(
                        SelectedAgent(searchedAgent, success, Seq.empty).copy(backLink = Some(backLink))
                      )
                      if (config.agentJourney2026) {
                        Future.successful(Redirect(routes.SelectRatingListNewController.show()))
                      } else {
                        Future.successful(Redirect(routes.RatingListOptionsController.show()))
                      }
                  }
                }
                .flatten
            }.flatten
            else
              Future.successful(Redirect(controllers.agentAppointment.routes.AddAgentController.showAgentCodePage()))
        )
    }
  }

  private def getBacklinkForCheckAnswersPage(propertySize: Int, listYears: Seq[String]): String =
    propertySize match {
      case 0 =>
        if (listYears.nonEmpty) routes.SelectRatingListController.show().url
        else routes.RatingListOptionsController.show().url
      case 1 =>
        routes.AddAgentController.oneProperty().url
      case _ =>
        routes.AddAgentController.multipleProperties().url
    }

  def oneProperty(fromCyaChange: Boolean = false): Action[AnyContent] =
    authenticated.andThen(withAppointAgentSession).async { implicit request =>
      for {
        agentDetailsOpt <- sessionRepo.get[AppointNewAgentSession]
        backLink        <- getBackLinkForPreliminaryCheckYourAnswers(fromCyaChange)
      } yield agentDetailsOpt match {
        case Some(answers) =>
          answers match {
            case answers: ManagingProperty if answers.managingPropertyChoice != "" =>
              Ok(
                agentToManageOnePropertyView(
                  manageOneProperty.fill(AddAgentOptions.fromName(answers.managingPropertyChoice).get),
                  request.agentDetails.name,
                  backLink
                )
              )
            case _ =>
              Ok(agentToManageOnePropertyView(manageOneProperty, request.agentDetails.name, backLink))
          }
      }
    }

  def submitOneProperty: Action[AnyContent] =
    authenticated.andThen(withAppointAgentSession).async { implicit request =>
      manageOneProperty
        .bindFromRequest()
        .fold(
          errors =>
            Future.successful(
              BadRequest(agentToManageOnePropertyView(errors, request.agentDetails.name, getBackLinkFromSession))
            ),
          success =>
            for {
              selectedAgentOpt <- sessionRepo.get[SelectedAgent]
              selectedAgent = selectedAgentOpt.getOrElse(throw NoAgentSavedException("no agent saved"))
              _ <- sessionRepo.saveOrUpdate(
                     ManagingProperty(
                       selectedAgent = selectedAgent,
                       selection = success.name,
                       singleProperty = true,
                       totalPropertySelectionSize = 1,
                       propertySelectedSize = if (success.name == "no") 0 else 1
                     ).copy(backLink = Some(routes.AddAgentController.oneProperty().url))
                   )
            } yield Redirect(routes.CheckYourAnswersController.onPageLoad())
        )
    }

  private def getBackLinkFromSession(implicit request: AppointAgentSessionRequest[AnyContent]) = {
    sessionRepo.get[AppointNewAgentSession].map { case Some(answer) =>
      if (config.agentJourney2026) {
        answer match {
          case answer: ManagingProperty if answer.ratingLists.nonEmpty =>
            routes.SelectRatingListNewController.show().url
          case answer: SelectedAgent if answer.ratingLists.nonEmpty =>
            routes.SelectRatingListNewController.show().url
          case _ => routes.AddAgentController.isCorrectAgent().url
        }
      } else {
        answer match {
          case answer: ManagingProperty if answer.ratingLists.size == 1 =>
            routes.SelectRatingListController.show().url
          case answer: ManagingProperty if answer.ratingLists.size == 2 =>
            routes.RatingListOptionsController.show().url
          case answer: SelectedAgent if answer.ratingLists.size == 1 =>
            routes.SelectRatingListController.show().url
          case answer: SelectedAgent if answer.ratingLists.size == 2 =>
            routes.RatingListOptionsController.show().url
          case _ => routes.AddAgentController.isCorrectAgent().url
        }
      }
    }

    request.sessionData.backLink.getOrElse(config.dashboardUrl("home"))
  }

  def multipleProperties(fromCyaChange: Boolean = false): Action[AnyContent] =
    authenticated.andThen(withAppointAgentSession).async { implicit request =>
      for {
        agentDetailsOpt <- sessionRepo.get[AppointNewAgentSession]
        backLink        <- getBackLinkForPreliminaryCheckYourAnswers(fromCyaChange)
      } yield agentDetailsOpt match {
        case Some(answers) =>
          answers match {
            case answers: ManagingProperty if answers.managingPropertyChoice != "" =>
              Ok(
                agentToManageMultiplePropertiesView(
                  manageMultipleProperties.fill(AddAgentOptions.fromName(answers.managingPropertyChoice).get),
                  request.agentDetails.name,
                  backLink
                )
              )
            case _ =>
              Ok(agentToManageMultiplePropertiesView(manageMultipleProperties, request.agentDetails.name, backLink))
          }
        case _ =>
          Ok(agentToManageMultiplePropertiesView(manageMultipleProperties, request.agentDetails.name, backLink))
      }
    }

  private def getBackLinkForPreliminaryCheckYourAnswers(
        fromCya: Boolean
  )(implicit request: AppointAgentSessionRequest[AnyContent]): Future[String] =
    if (fromCya) Future.successful(routes.CheckYourAnswersController.onPageLoad().url)
    else
      sessionRepo.get[AppointNewAgentSession].map { case Some(answer) =>
        if (config.agentJourney2026) {
          answer match {
            case answer: ManagingProperty if answer.ratingLists.nonEmpty =>
              routes.SelectRatingListNewController.show().url
            case answer: SelectedAgent if answer.ratingLists.nonEmpty =>
              routes.SelectRatingListNewController.show().url
            case _ => routes.AddAgentController.isCorrectAgent().url
          }
        } else {
          answer match {
            case answer: ManagingProperty if answer.ratingLists.size == 1 =>
              routes.SelectRatingListController.show().url
            case answer: ManagingProperty if answer.ratingLists.size == 2 =>
              routes.RatingListOptionsController.show().url
            case answer: SelectedAgent if answer.ratingLists.size == 1 =>
              routes.SelectRatingListController.show().url
            case answer: SelectedAgent if answer.ratingLists.size == 2 =>
              routes.RatingListOptionsController.show().url
            case _ => routes.AddAgentController.isCorrectAgent().url
          }
        }
      }

  def submitMultipleProperties: Action[AnyContent] =
    authenticated.andThen(withAppointAgentSession).async { implicit request =>
      manageMultipleProperties
        .bindFromRequest()
        .fold(
          errors =>
            Future.successful(
              BadRequest(agentToManageMultiplePropertiesView(errors, request.agentDetails.name, getBackLinkFromSession))
            ),
          success =>
            for {
              selectedAgentOpt <- sessionRepo.get[SelectedAgent]
              selectedAgent = selectedAgentOpt.getOrElse(throw NoAgentSavedException("no agent saved"))
              propertyCount <- agentRelationshipService.getMyOrganisationPropertyLinksCount()
              _ <- sessionRepo.saveOrUpdate(
                     ManagingProperty(
                       selectedAgent =
                         selectedAgent.copy(backLink = Some(routes.AddAgentController.multipleProperties().url)),
                       selection = success.name,
                       singleProperty = false,
                       totalPropertySelectionSize = propertyCount,
                       propertySelectedSize = if (success.name == "all") propertyCount else 0
                     ).copy(backLink = Some(routes.AddAgentController.multipleProperties().url))
                   )
            } yield success match {
              case ChooseFromList =>
                joinOldJourney(selectedAgent.agentCode)
              case propertyrepresentation.All | propertyrepresentation.NoProperties =>
                Redirect(routes.CheckYourAnswersController.onPageLoad())
            }
        )
    }

  private def joinOldJourney(agentCode: Long) =
    Redirect(
      controllers.agentAppointment.routes.AppointAgentController.getMyOrganisationPropertyLinksWithAgentFiltering(
        pagination = PaginationParameters(),
        agentCode = agentCode,
        agentAppointed = Some(Both.name),
        backLinkUrl = RedirectUrl(routes.AddAgentController.multipleProperties().url),
        fromManageAgentJourney = false
      )
    )
}

case class NoAgentSavedException(message: String) extends Exception(message)
