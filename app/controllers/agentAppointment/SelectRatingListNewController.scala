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
import models.propertyrepresentation.{AppointNewAgentSession, ManagingProperty, RatingListYearsNew, SelectedAgent}
import models.searchApi.AgentPropertiesParameters
import play.api.data.Form
import play.api.data.Forms.{mapping, optional, text}
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepo
import services.AgentRelationshipService
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SelectRatingListNewController @Inject() (
      authenticated: AuthenticatedAction,
      withAppointAgentSession: WithAppointAgentSessionRefiner,
      agentRelationshipService: AgentRelationshipService,
      @Named("appointNewAgentSession") val sessionRepo: SessionRepo,
      selectRatingListNewView: views.html.appointAgent.selectRatingListNew
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
              selectRatingListNewView(
                answers.agentOrganisationName,
                fromCyaChange,
                ratingListYearsNew.fill(
                  RatingListYearsNew(
                    listYearOne = Some("2026").filter(answers.ratingLists.contains),
                    listYearTwo = Some("2023").filter(answers.ratingLists.contains),
                    listYearThree = Some("2017").filter(answers.ratingLists.contains)
                  )
                ),
                backLink = getBackLink(fromCyaChange, answers.ratingLists)
              )
            )
          )
        case answers: SelectedAgent if answers.ratingLists.nonEmpty =>
          Future.successful(
            Ok(
              selectRatingListNewView(
                answers.agentOrganisationName,
                fromCyaChange,
                ratingListYearsNew.fill(
                  RatingListYearsNew(
                    listYearOne = Some("2026").filter(answers.ratingLists.contains),
                    listYearTwo = Some("2023").filter(answers.ratingLists.contains),
                    listYearThree = Some("2017").filter(answers.ratingLists.contains)
                  )
                ),
                backLink = getBackLink(fromCyaChange, answers.ratingLists)
              )
            )
          )
        case answers: SelectedAgent =>
          Future.successful(
            Ok(
              selectRatingListNewView(
                answers.agentOrganisationName,
                fromCyaChange,
                ratingListYearsNew,
                backLink = getBackLink(fromCyaChange, answers.ratingLists)
              )
            )
          )
        case answers: ManagingProperty =>
          Future.successful(
            Ok(
              selectRatingListNewView(
                answers.agentOrganisationName,
                fromCyaChange,
                ratingListYearsNew,
                backLink = getBackLink(fromCyaChange, answers.ratingLists)
              )
            )
          )
      }).flatten
    }

  def submitRatingListYear(fromCyaChange: Boolean = false): Action[AnyContent] =
    authenticated.andThen(withAppointAgentSession).async { implicit request =>
      ratingListYearsNew
        .bindFromRequest()
        .fold(
          errors =>
            {
              for {
                selectedAgentOpt <- sessionRepo.get[SelectedAgent]
                selectedAgent = selectedAgentOpt.getOrElse(throw NoAgentSavedException("no agent saved"))
              } yield Future.successful(
                BadRequest(
                  selectRatingListNewView(
                    selectedAgent.agentOrganisationName,
                    fromCyaChange,
                    errors,
                    getBackLink(fromCyaChange, selectedAgent.ratingLists)
                  )
                )
              )
            }.flatten,
          success =>
            {
              for {
                selectedAgentOpt <- sessionRepo.get[SelectedAgent]
                selectedAgent = selectedAgentOpt.getOrElse(throw NoAgentSavedException("no agent saved"))
                propertyLinks <- agentRelationshipService.getMyOrganisationPropertyLinksWithAgentFiltering(
                                   params = GetPropertyLinksParameters(),
                                   pagination = AgentPropertiesParameters(agentCode = selectedAgent.agentCode),
                                   agentOrganisationId = selectedAgent.agentCode,
                                   organisationId = request.organisationId
                                 )
                ratingLists: List[String] =
                  List(success.listYearOne, success.listYearTwo, success.listYearThree).flatten.filter(_.nonEmpty)
              } yield sessionRepo
                .get[AppointNewAgentSession]
                .map { case Some(sessionData) =>
                  sessionData match {
                    case managingProperty: ManagingProperty if fromCyaChange =>
                      sessionRepo.saveOrUpdate(
                        managingProperty.copy(
                          ratingLists = ratingLists,
                          backLink = Some(
                            getBacklinkForCheckAnswersPage(
                              propertyLinks.authorisations.size
                            )
                          )
                        )
                      )
                      Future.successful(Redirect(routes.CheckYourAnswersController.onPageLoad()))
                    case managingProperty: ManagingProperty =>
                      sessionRepo.saveOrUpdate(
                        managingProperty.copy(
                          ratingLists = ratingLists,
                          backLink = Some(
                            getBacklinkForCheckAnswersPage(
                              propertyLinks.authorisations.size
                            )
                          )
                        )
                      )
                      Future.successful(
                        Redirect(
                          getBacklinkForCheckAnswersPage(
                            propertyLinks.authorisations.size
                          )
                        )
                      )
                    case _ =>
                      propertyLinks.authorisations.size match {
                        case 0 =>
                          sessionRepo.saveOrUpdate(
                            ManagingProperty(
                              selectedAgent.copy(
                                ratingLists = ratingLists,
                                backLink = Some(routes.SelectRatingListNewController.show(fromCyaChange).url)
                              ),
                              selection = "none",
                              singleProperty = false,
                              totalPropertySelectionSize = 0,
                              propertySelectedSize = 0
                            )
                          )
                          Future.successful(Redirect(routes.CheckYourAnswersController.onPageLoad()))
                        case 1 =>
                          sessionRepo.saveOrUpdate(
                            selectedAgent.copy(
                              ratingLists = ratingLists,
                              backLink = Some(routes.SelectRatingListNewController.show(fromCyaChange).url)
                            )
                          )
                          Future.successful(Redirect(routes.AddAgentController.oneProperty()))
                        case _ =>
                          sessionRepo.saveOrUpdate(
                            selectedAgent.copy(
                              ratingLists = ratingLists,
                              backLink = Some(routes.SelectRatingListNewController.show(fromCyaChange).url)
                            )
                          )
                          Future.successful(Redirect(routes.AddAgentController.multipleProperties()))
                      }
                  }
                }
                .flatten
            }.flatten
        )
    }

  def getBackLink(fromCya: Boolean, ratingLists: Seq[String]): String =
    if (fromCya && ratingLists.nonEmpty)
      routes.CheckYourAnswersController.onPageLoad().url
    else controllers.agentAppointment.routes.AddAgentController.isCorrectAgent().url

  private def ratingListYearsNew: Form[RatingListYearsNew] = {
    val atLeastOneNonEmpty: Constraint[RatingListYearsNew] = Constraint("constraint.atLeastOneNonEmpty") { data =>
      if (data.listYearOne.nonEmpty || data.listYearTwo.nonEmpty || data.listYearThree.nonEmpty) {
        Valid
      } else {
        Invalid("At least one field must be provided")
      }
    }

    val listYearOneValidation: Constraint[RatingListYearsNew] = Constraint("constraint.listYearOneValid") { data =>
      data.listYearOne match {
        case Some("2026") | None => Valid
        case _                   => Invalid("If provided, listYearOne must be 2026")
      }
    }

    val listYearTwoValidation: Constraint[RatingListYearsNew] = Constraint("constraint.listYearTwoValid") { data =>
      data.listYearTwo match {
        case Some("2023") | None => Valid
        case _                   => Invalid("If provided, listYearTwo must be 2023")
      }
    }

    val listYearThreeValidation: Constraint[RatingListYearsNew] = Constraint("constraint.listYearThreeValid") { data =>
      data.listYearThree match {
        case Some("2017") | None => Valid
        case _                   => Invalid("If provided, listYearThree must be 2017")
      }
    }

    Form(
      mapping(
        "listYearOne"   -> optional(text),
        "listYearTwo"   -> optional(text),
        "listYearThree" -> optional(text)
      )(RatingListYearsNew.apply)(RatingListYearsNew.unapply)
        .verifying(atLeastOneNonEmpty)
        .verifying(listYearOneValidation)
        .verifying(listYearTwoValidation)
        .verifying(listYearThreeValidation)
    )
  }

  private def getBacklinkForCheckAnswersPage(propertySize: Int): String =
    propertySize match {
      case 0 =>
        routes.AddAgentController.isCorrectAgent().url
      case 1 =>
        routes.AddAgentController.oneProperty().url
      case _ =>
        routes.AddAgentController.multipleProperties().url
    }

}
