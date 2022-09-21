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
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepo
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler
import uk.gov.voa.play.form.ConditionalMappings._
import views.helpers.Errors

import java.time.LocalDate
import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClaimPropertyOwnershipController @Inject()(
      val errorHandler: CustomErrorHandler,
      @Named("propertyLinkingSession") val sessionRepository: SessionRepo,
      authenticatedAction: AuthenticatedAction,
      withLinkingSession: WithLinkingSession,
      ownershipToPropertyView: views.html.propertyLinking.ownershipToProperty)(
      implicit executionContext: ExecutionContext,
      override val messagesApi: MessagesApi,
      override val controllerComponents: MessagesControllerComponents,
      val config: ApplicationConfig
) extends PropertyLinkingController {

  import ClaimPropertyOwnership._

  def showOwnership(): Action[AnyContent] = authenticatedAction.andThen(withLinkingSession).async { implicit request =>
    {
      val form = ownershipForm(request.ses.earliestStartDate, request.ses.propertyOccupancy.flatMap(_.lastOccupiedDate))

      if (request.ses.earliestStartDate.isAfter(LocalDate.now()))
        sessionRepository
          .saveOrUpdate[LinkingSession](request.ses.copy(
            propertyOwnership =
              Some(PropertyOwnership(interestedOnOrBefore = false, fromDate = Some(request.ses.earliestStartDate))),
            propertyOccupancy = Some(PropertyOccupancy(stillOccupied = true, lastOccupiedDate = None))
          ))
          .map { _ =>
            Redirect(routes.ChooseEvidenceController.show())
          } else
        Future.successful(
          Ok(ownershipToPropertyView(
            ClaimPropertyOwnershipVM(
              request.ses.propertyOwnership.fold(form)(ownership => form.fillAndValidate(ownership)),
              request.ses.address,
              request.ses.earliestStartDate,
              request.ses.localAuthorityReference
            ),
            request.ses.clientDetails,
            getBackLink
          )))
    }
  }

  def submitOwnership(): Action[AnyContent] =
    authenticatedAction.andThen(withLinkingSession).async { implicit request =>
      ownershipForm(request.ses.earliestStartDate, request.ses.propertyOccupancy.flatMap(_.lastOccupiedDate))
        .bindFromRequest()
        .fold(
          errors =>
            Future.successful(BadRequest(ownershipToPropertyView(
              ClaimPropertyOwnershipVM(
                errors,
                request.ses.address,
                request.ses.earliestStartDate,
                request.ses.localAuthorityReference),
              request.ses.clientDetails,
              getBackLink
            ))),
          formData =>
            sessionRepository
              .saveOrUpdate[LinkingSession](request.ses.copy(propertyOwnership = Some(formData)))
              .map { _ =>
                if (request.ses.fromCya.contains(true)) Redirect(routes.DeclarationController.show())
                else Redirect(routes.ClaimPropertyOccupancyController.showOccupancy())
            }
        )
    }
}

object ClaimPropertyOwnership {

  def ownershipForm(earliestStartDate: LocalDate, endDate: Option[LocalDate]): Form[PropertyOwnership] =
    Form(
      mapping(
        "interestedOnOrBefore" -> mandatoryBoolean,
        "fromDate" -> mandatoryIfFalse(
          "interestedOnOrBefore",
          dmyDateAfterThreshold(earliestStartDate)
            .verifying(Errors.dateMustBeInPast, d => d.isBefore(LocalDate.now))
            .verifying(
              error = "interestedOnOrBefore.error.startDateMustBeBeforeEnd",
              firstOccupied => endDate.forall(lastOccupied => firstOccupied.isBefore(lastOccupied))
            )
        )
      )(PropertyOwnership.apply)(PropertyOwnership.unapply))

  def getBackLink(implicit request: LinkingSessionRequest[_]): String =
    if (request.ses.fromCya.contains(true)) routes.DeclarationController.show().url
    else routes.ClaimPropertyRelationshipController.back().url
}

case class ClaimPropertyOwnershipVM(
      form: Form[_],
      address: String,
      onOrBeforeDate: LocalDate,
      localAuthorityReference: String)
