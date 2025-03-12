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
import businessrates.authorisation.config.FeatureSwitch
import com.google.inject.Singleton
import config.ApplicationConfig
import controllers.PropertyLinkingController
import form.EnumMapping
import models.propertyrepresentation.{AppointNewAgentSession, ManagingProperty, RatingListYearsOptions, SelectedAgent}
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
      featureSwitch: FeatureSwitch,
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
      if (featureSwitch.isAgentListYearsEnabled) {
        for {
          agentDetailsOpt <- sessionRepo.get[AppointNewAgentSession]
          selectedAgent = agentDetailsOpt.getOrElse(throw NoAgentSavedException("no agent saved"))
        } yield selectedAgent match {
          case answers: ManagingProperty if answers.specificRatingList.nonEmpty =>
            Future.successful(
              Ok(
                selectRatingListView(
                  fromCyaChange,
                  ratingListYears.fill(RatingListYearsOptions.fromName(answers.specificRatingList.get).get),
                  backLink = getBackLink(fromCyaChange, answers.specificRatingList, answers.backLink)
                )
              )
            )
          case answers: SelectedAgent if answers.specificRatingList.nonEmpty =>
            Future.successful(
              Ok(
                selectRatingListView(
                  fromCyaChange,
                  ratingListYears.fill(RatingListYearsOptions.fromName(answers.specificRatingList.get).get),
                  backLink = getBackLink(fromCyaChange, answers.specificRatingList, answers.backLink)
                )
              )
            )
          case answers: SelectedAgent =>
            Future.successful(
              Ok(
                selectRatingListView(
                  fromCyaChange,
                  ratingListYears,
                  backLink = getBackLink(fromCyaChange, answers.specificRatingList, answers.backLink)
                )
              )
            )
          case answers: ManagingProperty =>
            Future.successful(
              Ok(
                selectRatingListView(
                  fromCyaChange,
                  ratingListYears,
                  backLink = getBackLink(fromCyaChange, answers.specificRatingList, answers.backLink)
                )
              )
            )
        }
      }.flatten
      else Future.successful(NotFound(errorHandler.notFoundErrorTemplate))
    }

  def submitRatingListYear(fromCyaChange: Boolean = false): Action[AnyContent] =
    authenticated.andThen(withAppointAgentSession).async { implicit request =>
      ratingListYears
        .bindFromRequest()
        .fold(
          errors =>
            {
              for {
                selectedAgentOpt <- sessionRepo.get[ManagingProperty]
                selectedAgent = selectedAgentOpt.getOrElse(throw NoAgentSavedException("no agent saved"))
              } yield Future.successful(
                BadRequest(
                  selectRatingListView(
                    fromCyaChange,
                    errors,
                    getBackLink(fromCyaChange, selectedAgent.specificRatingList, selectedAgent.backLink)
                  )
                )
              )
            }.flatten,
          success =>
            {
              for {
                selectedAgentOpt <- sessionRepo.get[ManagingProperty]
                selectedAgent = selectedAgentOpt.getOrElse(throw NoAgentSavedException("no agent saved"))
                propertyLinks <- agentRelationshipService.getMyOrganisationPropertyLinksWithAgentFiltering(
                                   params = GetPropertyLinksParameters(),
                                   pagination = AgentPropertiesParameters(agentCode = selectedAgent.agentCode),
                                   agentOrganisationId = selectedAgent.agentCode,
                                   organisationId = request.organisationId
                                 )
                _ <- sessionRepo.saveOrUpdate(
                       selectedAgent.copy(specificRatingList = Some(success.name), ratingLists = Seq(success.name))
                     )
              } yield sessionRepo
                .get[AppointNewAgentSession]
                .map { case Some(sessionData) =>
                  sessionData match {
                    case managingProperty: ManagingProperty if fromCyaChange =>
                      sessionRepo.saveOrUpdate(
                        managingProperty.copy(
                          specificRatingList = Some(success.name),
                          ratingLists = Seq(success.name),
                          backLink = Some(
                            getBacklinkForCheckAnswersPage(
                              propertyLinks.authorisations.size,
                              managingProperty.specificRatingList
                            )
                          )
                        )
                      )
                      Future.successful(Redirect(routes.CheckYourAnswersController.onPageLoad()))
                    case _ =>
                      propertyLinks.authorisations.size match {
                        case 0 =>
                          sessionRepo.saveOrUpdate(
                            selectedAgent.copy(
                              specificRatingList = Some(success.name),
                              ratingLists = Seq(success.name),
                              managingPropertyChoice = "none",
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
                            selectedAgent.copy(
                              specificRatingList = Some(success.name),
                              ratingLists = Seq(success.name),
                              backLink = Some(routes.SelectRatingListController.show(fromCyaChange).url)
                            )
                          )
                          Future.successful(
                            Redirect(controllers.agentAppointment.routes.AddAgentController.oneProperty())
                          )
                        case _ =>
                          sessionRepo.saveOrUpdate(
                            selectedAgent.copy(
                              specificRatingList = Some(success.name),
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

  def getBackLink(fromCya: Boolean, specficRatingList: Option[String], backLink: Option[String]) =
    if (
      fromCya && specficRatingList != None && backLink.get != controllers.agentAppointment.routes.RatingListOptionsController
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

  private def getBacklinkForCheckAnswersPage(propertySize: Int, specificTaxYears: Option[String]): String =
    propertySize match {
      case 0 if featureSwitch.isAgentListYearsEnabled =>
        if (specificTaxYears.nonEmpty) routes.SelectRatingListController.show().url
        else routes.RatingListOptionsController.show().url
      case 0 =>
        routes.AddAgentController.isCorrectAgent().url
      case 1 =>
        routes.AddAgentController.oneProperty().url
      case _ =>
        routes.AddAgentController.multipleProperties().url
    }

}
