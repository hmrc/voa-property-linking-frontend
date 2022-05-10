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
import services.propertylinking.PropertyLinkingService
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler
import uk.gov.voa.play.form.ConditionalMappings._
import utils.Formatters
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
            startDate = request.ses.propertyOwnership.flatMap(_.fromDate).getOrElse(request.ses.earliestStartDate),
            errorMessageKeySuffix = request.ses.clientDetails.fold("")(_ => ".client")
          ).fillAndValidate(occupancy)
        }
        Future.successful(
          Ok(
            occupancyOfPropertyView(
              form,
              request.ses.clientDetails,
              controllers.propertyLinking.routes.ClaimPropertyOwnershipController.showOwnership().url)))
      }
    }
  }

  def submitOccupancy(): Action[AnyContent] =
    authenticatedAction.andThen(withLinkingSession).async { implicit request =>
      occupancyForm(
        startDate = request.ses.propertyOwnership.flatMap(_.fromDate).getOrElse(request.ses.earliestStartDate),
        errorMessageKeySuffix = request.ses.clientDetails.fold("")(_ => ".client")
      ).bindFromRequest()
        .fold(
          errors =>
            Future.successful(
              BadRequest(
                occupancyOfPropertyView(
                  errors,
                  request.ses.clientDetails,
                  controllers.propertyLinking.routes.ClaimPropertyOwnershipController.showOwnership().url))),
          formData =>
            sessionRepository
              .saveOrUpdate[LinkingSession](request.ses.copy(propertyOccupancy = Some(formData)))
              .map { _ =>
                Redirect(routes.ChooseEvidenceController.show())
            }
        )
    }
}

object ClaimPropertyOccupancy {

  def occupancyForm(startDate: LocalDate, errorMessageKeySuffix: String = "")(
        implicit messages: Messages): Form[PropertyOccupancy] =
    Form(
      mapping(
        "stillOccupied" -> mandatoryBoolean,
        "lastOccupiedDate" -> mandatoryIfFalse(
          "stillOccupied",
          dmyDate
            .verifying(Errors.dateMustBeInPast, d => d.isBefore(LocalDate.now))
            .verifying(
              messages(s"error.date.mustBeAfterStartDate$errorMessageKeySuffix", Formatters.formatDate(startDate)),
              d => d.isAfter(startDate))
        )
      )(PropertyOccupancy.apply)(PropertyOccupancy.unapply))
}
