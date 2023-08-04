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

class AddAgentController @Inject()(
      val errorHandler: CustomErrorHandler,
      authenticated: AuthenticatedAction,
      withAppointAgentSession: WithAppointAgentSessionRefiner,
      agentRelationshipService: AgentRelationshipService,
      @Named("appointNewAgentSession") val sessionRepo: SessionRepo,
      startPageView: views.html.propertyrepresentation.appoint.start,
      agentCodeView: views.html.propertyrepresentation.appoint.agentCode,
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
        propertyLinkSubmissionId: Option[String] = None
  ): Action[AnyContent] = authenticated.async { implicit request =>
    val backLink = (propertyLinkId, valuationId, propertyLinkSubmissionId) match {
      case (Some(linkId), Some(valId), Some(submissionId)) =>
        config.businessRatesValuationFrontendUrl(
          s"property-link/$linkId/valuations/$valId?submissionId=$submissionId#agents-tab")
      case (None, Some(valId), Some(submissionId)) =>
        controllers.detailedvaluationrequest.routes.DvrController
          .myOrganisationRequestDetailValuationCheck(
            propertyLinkSubmissionId = submissionId,
            valuationId = valId,
            tabName = Some("agents-tab"))
          .url
      case _ => config.dashboardUrl("home")
    }
    sessionRepo
      .start[Start](Start(backLink = Some(backLink)))
      .map(_ => Redirect(controllers.agentAppointment.routes.AddAgentController.showStartPage))
  }

  def showStartPage: Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async { implicit request =>
    Future.successful(Ok(startPageView(getBackLink)))
  }

  def showAgentCodePage: Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async { implicit request =>
    val backLink = routes.AddAgentController.showStartPage.url
    for {
      agentDetailsOpt <- sessionRepo.get[AppointNewAgentSession]
    } yield agentDetailsOpt match {
      case Some(answers) =>
        answers match {
          case answers : SearchedAgent =>
            Ok(agentCodeView(agentCode.fill(answers.agentCode.toString), getBackLink))
          case answers : SelectedAgent =>
            Ok(agentCodeView(agentCode.fill(answers.agentCode.toString), getBackLink))
          case answers : ManagingProperty =>
            Ok(agentCodeView(agentCode.fill(answers.agentCode.toString), getBackLink))
          case _ =>
            Ok(agentCodeView(agentCode, getBackLink))
        }
      case _ =>
        Ok(agentCodeView(agentCode, backLink))
    }
  }

  def getAgentDetails: Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async { implicit request =>
    agentCode
      .bindFromRequest()
      .fold(
        errors => Future.successful(BadRequest(agentCodeView(errors, getBackLink))),
        success => {
          Try(success.toLong).toOption match {
            case None =>
              Future.successful(
                BadRequest(
                  agentCodeView(
                    agentCode.copy(
                      data = Map("agentCode" -> success),
                      errors = Seq[FormError](FormError(key = "agentCode", message = "error.agentCode.required"))),
                    getBackLink
                  )))
            case Some(representativeCode) => {
              for {
                organisationsAgents <- agentRelationshipService.getMyOrganisationAgents()
                agentNameAndAddressOpt <- agentRelationshipService.getAgentNameAndAddress(success.toLong).recoverWith {
                                           case b: BadRequestException => Future.successful(None)
                                         }
              } yield {
                agentNameAndAddressOpt match {
                  case None =>
                    Future.successful(BadRequest(agentCodeView(
                      agentCode.copy(
                        data = Map("agentCode" -> s"$representativeCode"),
                        errors = Seq[FormError](
                          FormError(key = "agentCode", message = "error.propertyRepresentation.unknownAgent"))),
                      getBackLink
                    )))
                  case Some(_) if organisationsAgents.agents.exists(a => a.representativeCode == representativeCode) =>
                    Future.successful(BadRequest(agentCodeView(
                      agentCode.copy(
                        data = Map("agentCode" -> s"$representativeCode"),
                        errors = Seq[FormError](
                          FormError(key = "agentCode", message = "error.propertyRepresentation.agentAlreadyAppointed"))
                      ),
                      getBackLink
                    )))
                  case Some(agent) =>
                    println(Console.MAGENTA_B + "asdasdad" + Console.RESET)
                    sessionRepo.get[AppointNewAgentSession].map {
                      case Some(sessionData) =>
                        sessionData match {
                          case answers: ManagingProperty =>
                            sessionRepo
                              .saveOrUpdate(
                                answers.copy(
                                  agentCode = representativeCode,
                                  agentOrganisationName = agent.name,
                                  agentAddress = agent.address,
                                  backLink = request.sessionData.backLink
                                )
                              )
                              .map(_ => Redirect(controllers.agentAppointment.routes.AddAgentController.isCorrectAgent))
                          case _ =>
                            sessionRepo
                              .saveOrUpdate(
                                SearchedAgent(
                                  agentCode = representativeCode,
                                  agentOrganisationName = agent.name,
                                  agentAddress = agent.address,
                                  backLink = request.sessionData.backLink
                                ))
                              .map(_ => Redirect(controllers.agentAppointment.routes.AddAgentController.isCorrectAgent))
                        }
                      case _ =>
                        sessionRepo
                          .saveOrUpdate(
                            SearchedAgent(
                              agentCode = representativeCode,
                              agentOrganisationName = agent.name,
                              agentAddress = agent.address,
                              backLink = request.sessionData.backLink
                            ))
                          .map(_ => Redirect(controllers.agentAppointment.routes.AddAgentController.isCorrectAgent))
                    }.flatten
                }
              }
            }.flatten
          }
        }
      )
  }

  def isCorrectAgent: Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async { implicit request =>
    val backLink = routes.AddAgentController.showAgentCodePage.url
    Future.successful(Ok(isTheCorrectAgentView(isThisTheCorrectAgent, request.agentDetails, backLink)))
  }

  def agentSelected: Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async { implicit request =>
    isThisTheCorrectAgent
      .bindFromRequest()
      .fold(
        errors => {
          Future.successful(BadRequest(isTheCorrectAgentView(errors, request.agentDetails, getBackLink)))
        },
        success => {
          if (success) {
            {
              for {
                searchedAgentOpt <- sessionRepo.get[SearchedAgent]
                searchedAgent = searchedAgentOpt.getOrElse(throw NoAgentSavedException("no agent saved"))
                propertyLinks <- agentRelationshipService.getMyOrganisationPropertyLinksWithAgentFiltering(
                                  params = GetPropertyLinksParameters(),
                                  pagination = AgentPropertiesParameters(agentCode = searchedAgent.agentCode),
                                  agentOrganisationId = searchedAgent.agentCode,
                                  organisationId = request.organisationId
                                )
              } yield {
                sessionRepo.get[AppointNewAgentSession].map {
                  case Some(sessionData) =>
                    println(Console.MAGENTA_B + s"did i go here$sessionData" + Console.RESET)
                    sessionData match {
                      case _: ManagingProperty =>
                        println(Console.MAGENTA_B + "damamammdamsm" + Console.RESET)
                        Future.successful(Redirect(controllers.agentAppointment.routes.AddAgentController.checkAnswers))
                      case _ =>
                        propertyLinks.authorisations.size match {
                          case 0 =>
                            println(Console.MAGENTA_B + "HUH!!!!" +Console.RESET)
                            agentRelationshipService
                              .assignAgent(AgentAppointmentChangeRequest(
                                action = AppointmentAction.APPOINT,
                                scope = AppointmentScope.RELATIONSHIP,
                                agentRepresentativeCode = searchedAgent.agentCode,
                                propertyLinks = None,
                                listYears = Some(List("2017", "2023"))
                              ))
                              .flatMap(_ => sessionRepo.saveOrUpdate(ManagingProperty(SelectedAgent(searchedAgent, success), selection = "none", singleProperty = false)).map( _ =>
                                Ok(checkYourAnswersView(submitAgentAppointmentRequest, request.sessionData.asInstanceOf[ManagingProperty], None)))
                              )
                          case 1 =>
                            sessionRepo.saveOrUpdate(SelectedAgent(searchedAgent, success))
                            Future.successful(Redirect(controllers.agentAppointment.routes.AddAgentController.oneProperty))
                          case _ =>
                            sessionRepo.saveOrUpdate(SelectedAgent(searchedAgent, success))
                            println (Console.MAGENTA_B + request.sessionData + Console.RESET)
                            Future.successful(Redirect(controllers.agentAppointment.routes.AddAgentController.multipleProperties))
                        }
                    }
                }.flatten
              }
            }.flatten
          } else {
            Future.successful(Redirect(controllers.agentAppointment.routes.AddAgentController.showAgentCodePage))
          }
        }
      )
  }

  def oneProperty: Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async { implicit request =>
    println(Console.MAGENTA_B + "????????" + Console.RESET)
    Future.successful(Ok(agentToManageOnePropertyView(manageOneProperty, request.agentDetails.name)))
  }

  def submitOneProperty: Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async { implicit request =>
    manageOneProperty
      .bindFromRequest()
      .fold(
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
            Redirect(controllers.agentAppointment.routes.AddAgentController.checkAnswers)
          }
        }
      )
  }

  def multipleProperties: Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async {
    implicit request =>
      println(Console.MAGENTA_B + "!!!!" + Console.RESET)
        for {
          agentDetailsOpt <- sessionRepo.get[AppointNewAgentSession]
        } yield agentDetailsOpt match {
          case Some(answers) =>
            answers match {
              case answers : ManagingProperty =>
                Ok(agentToManageMultiplePropertiesView(manageMultipleProperties.fill(AddAgentOptions.fromName(answers.managingPropertyChoice).get), request.agentDetails.name, getBackLink))
              case _ =>
                Ok(agentToManageMultiplePropertiesView(manageMultipleProperties, request.agentDetails.name, getBackLink))
            }
          case _ =>
            Ok(agentToManageMultiplePropertiesView(manageMultipleProperties, request.agentDetails.name, getBackLink))
        }
  }

  def submitMultipleProperties: Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async {
    implicit request =>
      manageMultipleProperties
        .bindFromRequest()
        .fold(
          errors => {
            Future.successful(BadRequest(agentToManageMultiplePropertiesView(errors, request.agentDetails.name, getBackLink)))
          },
          success => {
            for {
              selectedAgentOpt <- sessionRepo.get[SelectedAgent]
              selectedAgent = selectedAgentOpt.getOrElse(throw NoAgentSavedException("no agent saved"))
              _ <- sessionRepo.saveOrUpdate(
                    ManagingProperty(selectedAgent = selectedAgent.copy(backLink =
                      Some(routes.AddAgentController.submitMultipleProperties.url)),
                      selection = success.name, singleProperty = false))
            } yield {
              success match {
                case ChooseFromList =>
                  println(Console.MAGENTA_B + "i have gone down this path" + Console.RESET)
                  joinOldJourney(selectedAgent.agentCode)
                case propertyrepresentation.All | propertyrepresentation.NoProperties =>
                  Redirect(controllers.agentAppointment.routes.AddAgentController.checkAnswers)
              }
            }
          }
        )
  }

  def checkAnswers(): Action[AnyContent] = authenticated.andThen(withAppointAgentSession) { implicit request =>
    PartialFunction
      .condOpt(request.sessionData) {
        case data: ManagingProperty =>
          println(Console.MAGENTA_B + data + Console.RESET)
          sessionRepo.saveOrUpdate(
            data.copy().copy(backLink = Some(routes.AddAgentController.checkAnswers.url)))

            val propertyAmount =
              if(data.totalPropertySelectionSize != None && data.propertySelectedSize != None)
                Some(data.propertySelectedSize.get,data.totalPropertySelectionSize.get) else None
                println(Console.CYAN_B + propertyAmount + Console.RESET)
          Ok(checkYourAnswersView(submitAgentAppointmentRequest, data, propertyAmount))
      }
      .getOrElse(NotFound(errorHandler.notFoundTemplate))
  }

  def confirmAppointAgent: Action[AnyContent] = authenticated.andThen(withAppointAgentSession) { implicit request =>
    request.sessionData match {
      case data: ManagingProperty =>
        val key = {
          if (data.singleProperty) {
            Some("propertyRepresentation.confirmation.yourProperty")
          } else {
            data.managingPropertyChoice match {
              case All.name  => Some("propertyRepresentation.confirmation.allProperties")
              case ChooseFromList.name => Some("propertyRepresentation.confirmation.selectedProperties")
              case _      => None
            }
          }
        }
        Ok(confirmationView(agentName = request.agentDetails.name, assignedToMessageKey = key))
    }
  }

  def appointAgent: Action[AnyContent] = authenticated.andThen(withAppointAgentSession).async { implicit request =>
    submitAgentAppointmentRequest.bindFromRequest.fold(
      errors => {
        PartialFunction
          .condOpt(request.sessionData) {
            case data: ManagingProperty =>
              Future.successful(BadRequest(checkYourAnswersView(errors, data, None)))
          }
          .getOrElse(Future.successful(NotFound(errorHandler.notFoundTemplate)))
      }, { success =>
        {
          for {
            _ <- request.sessionData match {
                  case data: ManagingProperty =>
                    sessionRepo.saveOrUpdate(
                      data.copy(appointmentScope = Some(AppointmentScope.withName(success.scope)))
                        .copy(backLink = Some(routes.AddAgentController.appointAgent.url)))
                }
            _ <- agentRelationshipService.assignAgent(
                  AgentAppointmentChangeRequest(
                    action = AppointmentAction.APPOINT,
                    scope = AppointmentScope.withName(success.scope),
                    agentRepresentativeCode = success.agentRepresentativeCode,
                    propertyLinks = None,
                    listYears = Some(List("2017", "2023"))
                  ))
          } yield Redirect(controllers.agentAppointment.routes.AddAgentController.confirmAppointAgent)
        }
      }
    )
  }

  private def getBackLink(implicit request: AppointAgentSessionRequest[AnyContent]) = {
    request.sessionData.backLink.getOrElse(config.dashboardUrl("home"))
  }

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
