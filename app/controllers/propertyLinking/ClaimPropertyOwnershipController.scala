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

package controllers.propertyLinking

import actions.AuthenticatedAction
import actions.propertylinking.WithLinkingSession
import actions.propertylinking.requests.LinkingSessionRequest
import com.google.inject.Singleton
import config.ApplicationConfig
import connectors.vmv.VmvConnector
import controllers._
import form.Mappings._
import models._
import models.properties.PropertyHistory
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepo
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import java.time.LocalDate
import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClaimPropertyOwnershipController @Inject() (
      val errorHandler: CustomErrorHandler,
      @Named("propertyLinkingSession") val sessionRepository: SessionRepo,
      authenticatedAction: AuthenticatedAction,
      withLinkingSession: WithLinkingSession,
      vmvConnector: VmvConnector,
      ownershipToPropertyView: views.html.propertyLinking.ownershipToProperty
)(implicit
      executionContext: ExecutionContext,
      override val messagesApi: MessagesApi,
      override val controllerComponents: MessagesControllerComponents,
      val config: ApplicationConfig
) extends PropertyLinkingController {

  import ClaimPropertyOwnership._

  def showOwnership: Action[AnyContent] =
    authenticatedAction.andThen(withLinkingSession).async { implicit request =>
      val form = ownershipForm(request.ses.propertyOccupancy.flatMap(_.lastOccupiedDate))

      if (request.ses.earliestStartDate.isAfter(LocalDate.now()))
        sessionRepository
          .saveOrUpdate[LinkingSession](
            request.ses.copy(
              propertyOwnership = Some(PropertyOwnership(fromDate = request.ses.earliestStartDate)),
              propertyOccupancy = Some(PropertyOccupancy(stillOccupied = true, lastOccupiedDate = None))
            )
          )
          .map { _ =>
            Redirect(propertyLinking.routes.ChooseEvidenceController.show)
          }
      else
        Future.successful(
          Ok(
            ownershipToPropertyView(
              ClaimPropertyOwnershipVM(
                request.ses.propertyOwnership.fold(form)(ownership => form.fillAndValidate(ownership)),
                request.ses.address,
                request.ses.earliestStartDate,
                request.ses.localAuthorityReference
              ),
              request.ses.clientDetails,
              getBackLink
            )
          )
        )
    }

  def submitOwnership: Action[AnyContent] =
    authenticatedAction.andThen(withLinkingSession).async { implicit request =>
      def badRequest(form: Form[PropertyOwnership]): Result =
        BadRequest(
          ownershipToPropertyView(
            ClaimPropertyOwnershipVM(
              form,
              request.ses.address,
              request.ses.earliestStartDate,
              request.ses.localAuthorityReference
            ),
            request.ses.clientDetails,
            getBackLink
          )
        )

      val boundForm = ownershipForm(request.ses.propertyOccupancy.flatMap(_.lastOccupiedDate)).bindFromRequest()

      boundForm.fold(
        errors => Future.successful(badRequest(errors)),
        formData =>
          vmvConnector.getPropertyHistory(request.ses.uarn).flatMap { propertyHistory =>
            listYearCheck(formData.fromDate, propertyHistory) match {
              case Some(errorKey) =>
                Future.successful(
                  badRequest(boundForm.withError("interestedStartDate", errorKey))
                )
              case None =>
                sessionRepository
                  .saveOrUpdate[LinkingSession](request.ses.copy(propertyOwnership = Some(formData)))
                  .map { _ =>
                    if (request.ses.fromCya.contains(true)) Redirect(propertyLinking.routes.DeclarationController.show)
                    else Redirect(propertyLinking.routes.ClaimPropertyOccupancyController.showOccupancy)
                  }
            }
          }
      )
    }
}

object ClaimPropertyOwnership {

  def ownershipForm(endDate: Option[LocalDate]): Form[PropertyOwnership] =
    Form(
      mapping(
        "interestedStartDate" -> dmyDate
          .verifying("interestedStartDate.error.dateInFuture", d => d.isBefore(LocalDate.now))
          .verifying(
            error = "interestedStartDate.error.startDateMustBeBeforeEnd",
            firstOccupied => endDate.forall(lastOccupied => firstOccupied.isBefore(lastOccupied))
          )
      )(PropertyOwnership.apply)(PropertyOwnership.unapply)
    )

  def getBackLink(implicit request: LinkingSessionRequest[_]): String =
    if (request.ses.fromCya.contains(true)) propertyLinking.routes.DeclarationController.show.url
    else propertyLinking.routes.ClaimPropertyRelationshipController.back.url

  private val listYearStarts: Seq[(String, LocalDate)] = Seq(
    "2017" -> LocalDate.of(2017, 4, 1),
    "2023" -> LocalDate.of(2023, 4, 1),
    "2026" -> LocalDate.of(2026, 4, 1)
  )

  def listYearCheck(startDate: LocalDate, propertyHistory: PropertyHistory): Option[String] = {
    val derivedListYear = listYearStarts.collect { case (year, start) if !startDate.isBefore(start) => year }.lastOption

    derivedListYear.flatMap { year =>
      val existsInYearOrLater = propertyHistory.history.exists(h => h.listYear.toInt >= year.toInt)
      if (!existsInYearOrLater) Some("interestedStartDate.error.removedFromListYear") else None
    }
  }
}

case class ClaimPropertyOwnershipVM(
      form: Form[_],
      address: String,
      onOrBeforeDate: LocalDate,
      localAuthorityReference: String
)
