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
import form.EnumMapping
import models.propertyrepresentation.{AppointNewAgentDetails, AppointNewAgentSession, ManagingPropertySelected, NoProperties, RatingListYearsOptions}
import models.searchApi.AgentPropertiesParameters
import play.api.data.{Form, Forms}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepo
import services.AgentRelationshipService
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}
@Singleton
class SelectRatingListController @Inject() (
      authenticated: AuthenticatedAction,
      withAppointAgentSession: WithAppointAgentSessionRefiner,
      agentRelationshipService: AgentRelationshipService,
      @Named("appointNewAgentSession") val sessionRepo: SessionRepo,
      selectRatingListView: views.html.appointAgent.selectRatingList
)(implicit
      executionContext: ExecutionContext,
      override val messagesApi: MessagesApi,
      override val controllerComponents: MessagesControllerComponents,
      val config: ApplicationConfig,
      val errorHandler: CustomErrorHandler
) extends PropertyLinkingController {

  def show(fromCyaChange: Boolean = false): Action[AnyContent] =
    authenticated.async { implicit request =>
      sessionRepo.get[AppointNewAgentSession].map {
        case Some(answers: AppointNewAgentDetails) =>
          val form = if (answers.ratingLists.size == 1) {
            ratingListYears.fill(
              RatingListYearsOptions.fromName(answers.ratingLists.head).get
            )
          } else ratingListYears

          Ok(
            selectRatingListView(
              fromCyaChange,
              form,
              backLink = getBackLink(fromCyaChange, answers.ratingLists, answers.backLink)
            )
          )

        case _ =>
          throw NoAgentSavedException("no agent saved")
      }
    }

  def submitRatingListYear(fromCyaChange: Boolean = false): Action[AnyContent] =
    authenticated.andThen(withAppointAgentSession).async { implicit request =>
      ratingListYears
        .bindFromRequest()
        .fold(
          errors =>
            {
              for {
                appointNewAgentDetailsOpt <- sessionRepo.get[AppointNewAgentDetails]
                appointNewAgentDetails =
                  appointNewAgentDetailsOpt.getOrElse(throw NoAgentSavedException("no agent saved"))
              } yield Future.successful(
                BadRequest(
                  selectRatingListView(
                    fromCyaChange,
                    errors,
                    getBackLink(fromCyaChange, appointNewAgentDetails.ratingLists, appointNewAgentDetails.backLink)
                  )
                )
              )
            }.flatten,
          success =>
            {
              for {
                appointNewAgentDetailsOpt <- sessionRepo.get[AppointNewAgentDetails]
                appointNewAgentDetails =
                  appointNewAgentDetailsOpt.getOrElse(throw NoAgentSavedException("no agent saved"))
                propertyLinks <- agentRelationshipService.getMyOrganisationPropertyLinksWithAgentFiltering(
                                   params = GetPropertyLinksParameters(),
                                   pagination = AgentPropertiesParameters(agentCode = appointNewAgentDetails.agentCode),
                                   agentOrganisationId = appointNewAgentDetails.agentCode,
                                   organisationId = request.organisationId
                                 )
                _ <- sessionRepo.saveOrUpdate(
                       appointNewAgentDetails.copy(ratingLists = Seq(success.name))
                     )
              } yield sessionRepo
                .get[AppointNewAgentSession]
                .map { case Some(sessionData) =>
                  sessionData match {
                    case appointNewAgentDetails: AppointNewAgentDetails if fromCyaChange =>
                      sessionRepo.saveOrUpdate(
                        appointNewAgentDetails.copy(
                          ratingLists = Seq(success.name),
                          backLink = Some(
                            getBacklinkForCheckAnswersPage(
                              propertyLinks.authorisations.size,
                              appointNewAgentDetails.ratingLists
                            )
                          )
                        )
                      )
                      Future.successful(Redirect(routes.CheckYourAnswersController.onPageLoad()))
                    case appointNewAgentDetails: AppointNewAgentDetails =>
                      propertyLinks.authorisations.size match {
                        case 0 =>
                          sessionRepo.saveOrUpdate(
                            appointNewAgentDetails.copy(
                              status = ManagingPropertySelected,
                              ratingLists = Seq(success.name),
                              managingPropertyChoice = Some(NoProperties),
                              singleProperty = false,
                              totalPropertySelectionSize = 0,
                              propertySelectedSize = 0,
                              backLink = Some(routes.SelectRatingListController.show(fromCyaChange).url)
                            )
                          )
                          Future.successful(
                            Redirect(controllers.agentAppointment.routes.CheckYourAnswersController.onPageLoad())
                          )
                        case 1 =>
                          sessionRepo.saveOrUpdate(
                            appointNewAgentDetails.copy(
                              ratingLists = Seq(success.name),
                              backLink = Some(routes.SelectRatingListController.show(fromCyaChange).url)
                            )
                          )
                          Future.successful(
                            Redirect(controllers.agentAppointment.routes.AddAgentController.oneProperty())
                          )
                        case _ =>
                          sessionRepo.saveOrUpdate(
                            appointNewAgentDetails.copy(
                              ratingLists = Seq(success.name),
                              backLink = Some(routes.SelectRatingListController.show(fromCyaChange).url)
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
        )
    }

  def getBackLink(fromCya: Boolean, listYears: Seq[String], backLink: Option[String]) =
    if (
      fromCya && listYears.nonEmpty && backLink.get != controllers.agentAppointment.routes.RatingListOptionsController
        .show(fromCya)
        .url
    )
      routes.CheckYourAnswersController.onPageLoad().url
    else controllers.agentAppointment.routes.RatingListOptionsController.show().url

  def ratingListYears: Form[RatingListYearsOptions] =
    Form(
      Forms.single(
        "multipleListYears" -> EnumMapping(RatingListYearsOptions)
      )
    )

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

}
