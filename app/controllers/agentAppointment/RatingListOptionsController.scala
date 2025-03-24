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
import binders.propertylinks.GetPropertyLinksParameters
import com.google.inject.Singleton
import config.ApplicationConfig
import controllers.PropertyLinkingController
import form.Mappings.mandatoryBoolean
import models.propertyrepresentation.{AppointNewAgentSession, ManagingProperty, SelectedAgent}
import models.searchApi.AgentPropertiesParameters
import play.api.data.Form
import play.api.data.Forms.single
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepo
import services.AgentRelationshipService
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RatingListOptionsController @Inject() (
      ratingListOptionsView: views.html.appointAgent.ratingListOptions,
      withAppointAgentSession: WithAppointAgentSessionRefiner,
      @Named("appointNewAgentSession") val sessionRepo: SessionRepo,
      authenticated: AuthenticatedAction,
      agentRelationshipService: AgentRelationshipService
)(implicit
      executionContext: ExecutionContext,
      override val messagesApi: MessagesApi,
      override val controllerComponents: MessagesControllerComponents,
      val config: ApplicationConfig,
      val errorHandler: CustomErrorHandler
) extends PropertyLinkingController {

  def show(fromCyaChange: Boolean = false): Action[AnyContent] =
    authenticated.async { implicit request =>
      (for {
        agentDetailsOpt <- sessionRepo.get[AppointNewAgentSession]
        selectedAgent = agentDetailsOpt.getOrElse(throw NoAgentSavedException("no agent saved"))
      } yield selectedAgent match {
        case answers: ManagingProperty if answers.ratingLists.nonEmpty =>
          Future.successful(
            Ok(
              ratingListOptionsView(
                fromCyaChange,
                ratingListYears.fill(answers.ratingLists.size == 2),
                agentName = answers.agentOrganisationName,
                backLink = getBackLink(fromCyaChange)
              )
            )
          )
        case answers: ManagingProperty =>
          Future.successful(
            Ok(
              ratingListOptionsView(
                fromCyaChange,
                ratingListYears.fill(false),
                agentName = answers.agentOrganisationName,
                backLink = getBackLink(fromCyaChange)
              )
            )
          )
        case answers: SelectedAgent if answers.ratingLists.nonEmpty =>
          Future.successful(
            Ok(
              ratingListOptionsView(
                fromCyaChange,
                ratingListYears.fill(answers.ratingLists.size == 2),
                agentName = answers.agentOrganisationName,
                backLink = getBackLink(fromCyaChange)
              )
            )
          )
        case answers: SelectedAgent =>
          Future.successful(
            Ok(
              ratingListOptionsView(
                fromCyaChange,
                ratingListYears,
                agentName = answers.agentOrganisationName,
                backLink = getBackLink(fromCyaChange)
              )
            )
          )
      }).flatten
    }

  def getBackLink(fromCya: Boolean) =
    if (fromCya) routes.CheckYourAnswersController.onPageLoad().url
    else controllers.agentAppointment.routes.AddAgentController.isCorrectAgent().url

  def ratingListYears: Form[Boolean] =
    Form(
      single(
        "multipleListYears" -> mandatoryBoolean
      )
    )

  def submitRatingListYear(fromCyaChange: Boolean = false): Action[AnyContent] =
    authenticated.andThen(withAppointAgentSession).async { implicit request =>
      ratingListYears
        .bindFromRequest()
        .fold(
          errors =>
            Future.successful(
              BadRequest(
                ratingListOptionsView(
                  fromCyaChange,
                  errors,
                  agentName = request.agentDetails.name,
                  backLink = getBackLink(fromCyaChange)
                )
              )
            ),
          success => {
            if (success) {
              for {
                selectedAgentOpt <- sessionRepo.get[SelectedAgent]
                selectedAgent = selectedAgentOpt.getOrElse(throw NoAgentSavedException("no agent saved"))
                propertyLinks <- agentRelationshipService.getMyOrganisationPropertyLinksWithAgentFiltering(
                                   params = GetPropertyLinksParameters(),
                                   pagination = AgentPropertiesParameters(agentCode = selectedAgent.agentCode),
                                   agentOrganisationId = selectedAgent.agentCode,
                                   organisationId = request.organisationId
                                 )
              } yield sessionRepo
                .get[AppointNewAgentSession]
                .map { case Some(sessionData) =>
                  sessionData match {
                    case managingProperty: ManagingProperty if fromCyaChange =>
                      sessionRepo.saveOrUpdate(
                        managingProperty.copy(
                          ratingLists = Seq("2023", "2017"),
                          backLink = Some(getBacklinkForCheckAnswersPage(propertyLinks.authorisations.size, None))
                        )
                      )
                      Future.successful(Redirect(routes.CheckYourAnswersController.onPageLoad()))
                    case selectedAgent: SelectedAgent =>
                      propertyLinks.authorisations.size match {
                        case 0 =>
                          sessionRepo.saveOrUpdate(
                            ManagingProperty(
                              selectedAgent.copy(
                                ratingLists = Seq("2023", "2017")
                              ),
                              selection = "none",
                              singleProperty = false,
                              totalPropertySelectionSize = 0,
                              propertySelectedSize = 0
                            ).copy(backLink = Some(routes.RatingListOptionsController.show(fromCyaChange).url))
                          )
                          Future.successful(
                            Redirect(controllers.agentAppointment.routes.CheckYourAnswersController.onPageLoad())
                          )
                        case 1 =>
                          sessionRepo.saveOrUpdate(
                            selectedAgent.copy(
                              ratingLists = Seq("2023", "2017"),
                              backLink = Some(routes.RatingListOptionsController.show(fromCyaChange).url)
                            )
                          )
                          Future.successful(
                            Redirect(controllers.agentAppointment.routes.AddAgentController.oneProperty())
                          )
                        case _ =>
                          sessionRepo.saveOrUpdate(
                            selectedAgent.copy(
                              ratingLists = Seq("2023", "2017"),
                              backLink = Some(routes.RatingListOptionsController.show(fromCyaChange).url)
                            )
                          )
                          Future.successful(
                            Redirect(controllers.agentAppointment.routes.AddAgentController.multipleProperties())
                          )
                      }
                    case selectedAgent: ManagingProperty =>
                      propertyLinks.authorisations.size match {
                        case 0 =>
                          sessionRepo.saveOrUpdate(
                            selectedAgent.copy(
                              ratingLists = Seq("2023", "2017"),
                              backLink = Some(routes.RatingListOptionsController.show(fromCyaChange).url)
                            )
                          )
                          Future.successful(
                            Redirect(controllers.agentAppointment.routes.CheckYourAnswersController.onPageLoad())
                          )
                        case 1 =>
                          sessionRepo.saveOrUpdate(
                            selectedAgent.copy(
                              ratingLists = Seq("2023", "2017"),
                              backLink = Some(routes.RatingListOptionsController.show(fromCyaChange).url)
                            )
                          )
                          Future.successful(
                            Redirect(controllers.agentAppointment.routes.AddAgentController.oneProperty())
                          )
                        case _ =>
                          sessionRepo.saveOrUpdate(
                            selectedAgent.copy(
                              ratingLists = Seq("2023", "2017"),
                              backLink = Some(routes.RatingListOptionsController.show(fromCyaChange).url)
                            )
                          )
                          Future.successful(
                            Redirect(controllers.agentAppointment.routes.AddAgentController.multipleProperties())
                          )
                      }
                  }
                }
                .flatten
            }.flatten
            else {
              sessionRepo.get[AppointNewAgentSession].map { case Some(answers) =>
                answers match {
                  case answers: ManagingProperty =>
                    sessionRepo.saveOrUpdate(
                      answers.copy(
                        backLink = Some(routes.RatingListOptionsController.show(fromCyaChange).url)
                      )
                    )
                  case answers: SelectedAgent =>
                    sessionRepo.saveOrUpdate(
                      ManagingProperty(
                        answers,
                        selection = "",
                        singleProperty = false,
                        totalPropertySelectionSize = 0,
                        propertySelectedSize = 0
                      ).copy(backLink = Some(routes.RatingListOptionsController.show(fromCyaChange).url))
                    )
                }
              }
              Future.successful(
                Redirect(controllers.agentAppointment.routes.SelectRatingListController.show(fromCyaChange))
              )
            }
          }
        )
    }

  private def getBacklinkForCheckAnswersPage(propertySize: Int, specificListYear: Option[String]): String =
    propertySize match {
      case 0 =>
        if (specificListYear.nonEmpty) routes.SelectRatingListController.show().url
        else routes.RatingListOptionsController.show().url
      case 1 =>
        routes.AddAgentController.oneProperty().url
      case _ =>
        routes.AddAgentController.multipleProperties().url
    }

}
