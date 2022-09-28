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

package controllers.propertyLinking

import actions.AuthenticatedAction
import actions.propertylinking.WithLinkingSession
import actions.propertylinking.requests.LinkingSessionRequest
import com.google.inject.Singleton
import config.ApplicationConfig
import controllers._
import form.Mappings._
import models._
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepo
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler
import uk.gov.voa.play.form.ConditionalMappings._
import views.helpers.Errors

import java.time.LocalDate
import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClaimPropertyOccupancyController @Inject()(
      val errorHandler: CustomErrorHandler,
      @Named("propertyLinkingSession") val sessionRepository: SessionRepo,
      authenticatedAction: AuthenticatedAction,
      withLinkingSession: WithLinkingSession,
      occupancyOfPropertyView: views.html.propertyLinking.occupancyOfProperty)(
      implicit executionContext: ExecutionContext,
      override val messagesApi: MessagesApi,
      override val controllerComponents: MessagesControllerComponents,
      val config: ApplicationConfig
) extends PropertyLinkingController {

  import ClaimPropertyOccupancy._

  def showOccupancy(): Action[AnyContent] = authenticatedAction.andThen(withLinkingSession).async { implicit request =>
    {
      if (request.ses.earliestStartDate.isAfter(LocalDate.now()))
        sessionRepository
          .saveOrUpdate[LinkingSession](request.ses.copy(
            propertyOccupancy = Some(PropertyOccupancy(stillOccupied = true, lastOccupiedDate = None))))
          .map { _ =>
            Redirect(routes.ChooseEvidenceController.show())
          } else {
        val form = request.ses.propertyOccupancy.fold(occupancyForm(request.ses.earliestStartDate)) { occupancy =>
          occupancyForm(
            startDate = request.ses.propertyOwnership.flatMap(_.fromDate).getOrElse(request.ses.earliestStartDate))
            .fillAndValidate(occupancy)
        }
        Future.successful(
          Ok(
            occupancyOfPropertyView(
              form,
              request.ses.clientDetails,
              getBackLink
            )))
      }
    }
  }

  def submitOccupancy(): Action[AnyContent] =
    authenticatedAction.andThen(withLinkingSession).async { implicit request =>
      occupancyForm(
        startDate = request.ses.propertyOwnership.flatMap(_.fromDate).getOrElse(request.ses.earliestStartDate))
        .bindFromRequest()
        .fold(
          errors =>
            Future.successful(BadRequest(occupancyOfPropertyView(errors, request.ses.clientDetails, getBackLink))),
          formData =>
            sessionRepository
              .saveOrUpdate[LinkingSession](request.ses.copy(propertyOccupancy = Some(formData)))
              .map { _ =>
                if (request.ses.fromCya.contains(true))
                  Redirect(controllers.propertyLinking.routes.DeclarationController.show().url)
                else Redirect(routes.ChooseEvidenceController.show())
            }
        )
    }
}

object ClaimPropertyOccupancy {

  def occupancyForm(startDate: LocalDate): Form[PropertyOccupancy] =
    Form(
      mapping(
        "stillOccupied" -> mandatoryBoolean,
        "lastOccupiedDate" -> mandatoryIfFalse(
          "stillOccupied",
          dmyDate
            .verifying(Errors.dateMustBeInPast, d => d.isBefore(LocalDate.now))
            .verifying("error.date.mustBeAfterStartDate", d => d.isAfter(startDate))
        )
      )(PropertyOccupancy.apply)(PropertyOccupancy.unapply))

  def getBackLink(implicit request: LinkingSessionRequest[_]): String =
    if (request.ses.fromCya.contains(true)) routes.DeclarationController.show().url
    else routes.ClaimPropertyOwnershipController.showOwnership().url
}
