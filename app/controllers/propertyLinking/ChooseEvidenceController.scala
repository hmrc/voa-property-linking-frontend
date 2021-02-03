/*
 * Copyright 2021 HM Revenue & Customs
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
import binders.propertylinks.EvidenceChoices
import config.ApplicationConfig
import controllers.PropertyLinkingController
import form.Mappings._

import javax.inject.Inject
import models.{LinkingSession, PropertyOwnership, UploadEvidenceData}
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.BusinessRatesAttachmentsService
import services.propertylinking.PropertyLinkingService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class ChooseEvidenceController @Inject()(
      val errorHandler: CustomErrorHandler,
      authenticatedAction: AuthenticatedAction,
      withLinkingSession: WithLinkingSession,
      businessRatesAttachmentService: BusinessRatesAttachmentsService,
      propertyLinkingService: PropertyLinkingService,
      chooseEvidenceView: views.html.propertyLinking.chooseEvidence
)(
      implicit executionContext: ExecutionContext,
      override val messagesApi: MessagesApi,
      override val controllerComponents: MessagesControllerComponents,
      val config: ApplicationConfig
) extends PropertyLinkingController {

  private val logger = Logger(this.getClass.getName)

  def show: Action[AnyContent] = authenticatedAction.andThen(withLinkingSession).async { implicit request =>
    logger.debug("show choose evidence page")
    for {
      startDate <- propertyLinkingService.findEarliestStartDate(request.ses.uarn)
      _         <- businessRatesAttachmentService.persistSessionData(request.ses, UploadEvidenceData.empty)
      backLink  <- backlink(request.ses)
    } yield {
      Ok(chooseEvidenceView(ChooseEvidence.form, request.ses.clientDetails, Some(backLink)))
    }
  }

  private def backlink(ses: LinkingSession)(implicit hc: HeaderCarrier) =
    propertyLinkingService.findEarliestStartDate(ses.uarn).map { startDate =>
      startDate match {
        case Some(date) if date.isAfter(LocalDate.now()) =>
          controllers.propertyLinking.routes.ClaimPropertyRelationshipController
            .showRelationship(
              ses.uarn,
              ses.address,
              ses.clientDetails
            )
            .url
        case _ =>
          controllers.propertyLinking.routes.ClaimPropertyOwnershipController.showOwnership().url
      }
    }

  def submit: Action[AnyContent] = authenticatedAction.andThen(withLinkingSession).async { implicit request =>
    ChooseEvidence.form
      .bindFromRequest()
      .fold(
        errors =>
          backlink(request.ses).map { back =>
            BadRequest(chooseEvidenceView(errors, request.ses.clientDetails, Some(back)))
        }, {
          case true  => Future.successful(Redirect(routes.UploadController.show(EvidenceChoices.RATES_BILL)))
          case false => Future.successful(Redirect(routes.UploadController.show(EvidenceChoices.OTHER)))
        }
      )
  }
}

object ChooseEvidence {
  lazy val form = Form(single("hasRatesBill" -> mandatoryBoolean))
}
