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
class SelectRatingListController @Inject()(
      authenticated: AuthenticatedAction,
      featureSwitch: FeatureSwitch,
      withAppointAgentSession: WithAppointAgentSessionRefiner,
      agentRelationshipService: AgentRelationshipService,
      @Named("appointNewAgentSession") val sessionRepo: SessionRepo,
      selectRatingListView: views.html.appointAgent.selectRatingList)(
      implicit executionContext: ExecutionContext,
      override val messagesApi: MessagesApi,
      override val controllerComponents: MessagesControllerComponents,
      val config: ApplicationConfig,
      val errorHandler: CustomErrorHandler
) extends PropertyLinkingController {

  def show(fromCyaChange: Boolean = false): Action[AnyContent] = authenticated.async { implicit request =>
    if (featureSwitch.isAgentListYearsEnabled) {
      for {
        agentDetailsOpt <- sessionRepo.get[AppointNewAgentSession]
        selectedAgent = agentDetailsOpt.getOrElse(throw NoAgentSavedException("no agent saved"))
      } yield {
        selectedAgent match {
          case answers: ManagingProperty if (answers.specificRatingList.nonEmpty) =>
            Future.successful(
              Ok(
                selectRatingListView(
                  fromCyaChange,
                  ratingListYears.fill(RatingListYearsOptions.fromName(answers.specificRatingList.get).get),
                  backLink = getBackLink(fromCyaChange)
                )))
          case answers: SelectedAgent if (answers.specificRatingList.nonEmpty) =>
            Future.successful(
              Ok(
                selectRatingListView(
                  fromCyaChange,
                  ratingListYears.fill(RatingListYearsOptions.fromName(answers.specificRatingList.get).get),
                  backLink = getBackLink(fromCyaChange)
                )))
          case _ =>
            Future.successful(
              Ok(
                selectRatingListView(
                  fromCyaChange,
                  ratingListYears,
                  backLink = getBackLink(fromCyaChange)
                )))
        }
      }
    }.flatten
    else Future.successful(NotFound(errorHandler.notFoundErrorTemplate))
  }

  def submitRatingListYear(fromCyaChange: Boolean = false): Action[AnyContent] =
    authenticated.andThen(withAppointAgentSession).async { implicit request =>
      ratingListYears
        .bindFromRequest()
        .fold(
          errors => {
            Future.successful(BadRequest(selectRatingListView(fromCyaChange, errors, getBackLink(fromCyaChange))))
          },
          success => {
            if (fromCyaChange) {
              sessionRepo
                .get[AppointNewAgentSession]
                .map { session =>
                  session.get match {
                    case managingProperty: ManagingProperty =>
                      sessionRepo.saveOrUpdate(
                        managingProperty.copy(
                          bothRatingLists = None,
                          specificRatingList = Some(success.name)
                        )
                      )
                      Future.successful(Redirect(routes.CheckYourAnswersController.onPageLoad()))
                  }
                }
                .flatten
            } else {
              for {
                selectedAgentOpt <- sessionRepo.get[SelectedAgent]
                selectedAgent = selectedAgentOpt.getOrElse(throw NoAgentSavedException("no agent saved"))
                propertyLinks <- agentRelationshipService.getMyOrganisationPropertyLinksWithAgentFiltering(
                                  params = GetPropertyLinksParameters(),
                                  pagination = AgentPropertiesParameters(agentCode = selectedAgent.agentCode),
                                  agentOrganisationId = selectedAgent.agentCode,
                                  organisationId = request.organisationId
                                )
                _ <- sessionRepo.saveOrUpdate(selectedAgent.copy(specificRatingList = Some(success.name)))
              } yield {
                sessionRepo
                  .get[AppointNewAgentSession]
                  .map {
                    case Some(sessionData) =>
                      sessionData match {
                        case _ =>
                          propertyLinks.authorisations.size match {
                            case 0 =>
                              sessionRepo.saveOrUpdate(ManagingProperty(
                                selectedAgent.copy(
                                  bothRatingLists = None,
                                  specificRatingList = Some(success.name),
                                ),
                                selection = "none",
                                singleProperty = false,
                                totalPropertySelectionSize = 0,
                                propertySelectedSize = 0,
                              ).copy(backLink = Some(getBackLink(fromCyaChange))))
                              Future.successful(
                                Redirect(controllers.agentAppointment.routes.CheckYourAnswersController.onPageLoad()))
                            case 1 =>
                              sessionRepo.saveOrUpdate(
                                selectedAgent.copy(
                                  bothRatingLists = None,
                                  specificRatingList = Some(success.name),
                                  backLink = Some(routes.SelectRatingListController.show(fromCyaChange).url)
                                )
                              )
                              Future.successful(
                                Redirect(controllers.agentAppointment.routes.AddAgentController.oneProperty()))
                            case _ =>
                              sessionRepo.saveOrUpdate(
                                selectedAgent.copy(
                                  bothRatingLists = None,
                                  specificRatingList = Some(success.name),
                                  backLink = Some(routes.SelectRatingListController.show(fromCyaChange).url)
                                )
                              )
                              Future.successful(
                                Redirect(controllers.agentAppointment.routes.AddAgentController.multipleProperties()))
                          }
                      }
                  }
                  .flatten
              }
            }.flatten
          }
        )
    }

  def getBackLink(fromCya: Boolean) =
    if (fromCya) routes.CheckYourAnswersController.onPageLoad().url
    else controllers.agentAppointment.routes.RatingListOptionsController.show().url

  def ratingListYears: Form[RatingListYearsOptions] = Form(
    Forms.single(
      "multipleListYears" -> EnumMapping(RatingListYearsOptions)
    )
  )

}
