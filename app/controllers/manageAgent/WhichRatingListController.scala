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

package controllers.manageAgent

import actions.AuthenticatedAction
import com.google.inject.Singleton
import config.ApplicationConfig
import controllers.PropertyLinkingController
import form.Mappings.mandatoryBoolean
import models.propertyrepresentation.AgentSummary
import models.{RatingListYears, RatingListYearsNew}
import play.api.data.Form
import play.api.data.Forms.{mapping, optional, text}
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.ManageAgentSessionRepository
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import javax.inject.Inject
import scala.concurrent.ExecutionContext

@Singleton
class WhichRatingListController @Inject() (
      whichListView: views.html.manageAgent.whichRatingList,
      whichListViewNew: views.html.manageAgent.whichRatingListNew,
      manageAgentSessionRepository: ManageAgentSessionRepository,
      authenticated: AuthenticatedAction
)(implicit
      executionContext: ExecutionContext,
      override val messagesApi: MessagesApi,
      override val controllerComponents: MessagesControllerComponents,
      val config: ApplicationConfig,
      val errorHandler: CustomErrorHandler
) extends PropertyLinkingController {

  def show: Action[AnyContent] =
    authenticated.async { implicit request =>
      manageAgentSessionRepository.get[AgentSummary].map {
        case Some(AgentSummary(_, _, _, _, _, Some(listYears), _)) =>
          Ok(whichListView(ratingListYears, currentRatingList = listYears.toList, backLink = getBackLink))
        case _ => NotFound(errorHandler.notFoundErrorTemplate)
      }
    }

  def showRevalEnabled: Action[AnyContent] =
    authenticated.async { implicit request =>
      manageAgentSessionRepository.get[AgentSummary].map {
        case Some(AgentSummary(_, _, agentName, _, _, Some(listYears), proposedListYearsOpt)) =>
          val proposedListYears = proposedListYearsOpt.getOrElse(Seq.empty)
          Ok(
            whichListViewNew(
              form = ratingListYearsNew.fill(
                RatingListYearsNew(
                  listYearOne = proposedListYears.headOption,
                  listYearTwo = proposedListYears.lift(1),
                  listYearThree = proposedListYears.lift(2)
                )
              ),
              currentRatingList = listYears.toList,
              backLink = backLinkAgentJourney2026,
              agentName = agentName
            )
          )
        case _ => NotFound(errorHandler.notFoundErrorTemplate)
      }
    }

  def submitRatingListYears: Action[AnyContent] =
    authenticated.async { implicit request =>
      manageAgentSessionRepository.get[AgentSummary].map {
        case Some(agentSummary @ AgentSummary(_, _, _, _, _, Some(listYears), _)) =>
          ratingListYears
            .bindFromRequest()
            .fold(
              errors =>
                BadRequest(
                  whichListView(form = errors, currentRatingList = listYears.toList, backLink = getBackLink)
                ),
              formData =>
                if (formData.multipleListYears)
                  Redirect(controllers.manageAgent.routes.AreYouSureController.show("2023").url)
                else
                  Redirect(controllers.manageAgent.routes.AreYouSureController.show("2017").url)
            )
        case _ => NotFound(errorHandler.notFoundErrorTemplate)
      }
    }

  def submitRatingListYearsRevalEnabled: Action[AnyContent] =
    authenticated.async { implicit request =>
      manageAgentSessionRepository.get[AgentSummary].map {
        case Some(agentSummary @ AgentSummary(_, _, agentName, _, _, Some(listYears), _)) =>
          ratingListYearsNew
            .bindFromRequest()
            .fold(
              errors =>
                BadRequest(
                  whichListViewNew(
                    form = errors,
                    currentRatingList = listYears.toList,
                    backLink = backLinkAgentJourney2026,
                    agentName = agentName
                  )
                ),
              formData => {
                manageAgentSessionRepository.saveOrUpdate[AgentSummary](
                  agentSummary
                    .copy(proposedListYears =
                      Some(Seq(formData.listYearOne, formData.listYearTwo, formData.listYearThree).flatten)
                    )
                )
                // TODO: needs updating to go to new route for AreYouSureController.show that pulls proposed list years from cache
                Redirect(controllers.manageAgent.routes.AreYouSureController.show("2023").url)
              }
            )
        case _ => NotFound(errorHandler.notFoundErrorTemplate)
      }
    }

  def getBackLink: String = controllers.manageAgent.routes.ChooseRatingListController.show.url
  def backLinkAgentJourney2026: String = controllers.agent.routes.ManageAgentController.showManageAgent.url

  def ratingListYears: Form[RatingListYears] =
    Form(
      mapping(
        "multipleListYears" -> mandatoryBoolean
      )(RatingListYears.apply)(RatingListYears.unapply)
    )

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
        case _ => Invalid("If provided, listYearOne must be 2026")
      }
    }

    val listYearTwoValidation: Constraint[RatingListYearsNew] = Constraint("constraint.listYearTwoValid") { data =>
      data.listYearTwo match {
        case Some("2023") | None => Valid
        case _ => Invalid("If provided, listYearTwo must be 2023")
      }
    }

    val listYearThreeValidation: Constraint[RatingListYearsNew] = Constraint("constraint.listYearThreeValid") { data =>
      data.listYearThree match {
        case Some("2017") | None => Valid
        case _ => Invalid("If provided, listYearThree must be 2017")
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
}
