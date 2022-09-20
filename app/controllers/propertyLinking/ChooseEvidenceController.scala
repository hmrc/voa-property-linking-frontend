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
import binders.propertylinks.EvidenceChoices
import config.ApplicationConfig
import controllers.PropertyLinkingController
import form.Mappings._
import models.{LinkingSession, UploadEvidenceData}
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.BusinessRatesAttachmentsService
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ChooseEvidenceController @Inject()(
      val errorHandler: CustomErrorHandler,
      authenticatedAction: AuthenticatedAction,
      withLinkingSession: WithLinkingSession,
      businessRatesAttachmentService: BusinessRatesAttachmentsService,
      chooseEvidenceView: views.html.propertyLinking.chooseEvidence
)(
      implicit executionContext: ExecutionContext,
      override val messagesApi: MessagesApi,
      override val controllerComponents: MessagesControllerComponents,
      val config: ApplicationConfig
) extends PropertyLinkingController {

  def show: Action[AnyContent] = authenticatedAction.andThen(withLinkingSession).async { implicit request =>
    val form = request.ses.hasRatesBill.fold(ChooseEvidence.form)(ChooseEvidence.form.fillAndValidate)

    for {
      _ <- businessRatesAttachmentService.persistSessionData(request.ses.copy(fromCya = Some(false)))
      backLink = backlink(request.ses)
    } yield {
      Ok(chooseEvidenceView(form, Some(backLink)))
    }
  }

  private def backlink(session: LinkingSession): String =
    if (session.earliestStartDate.isAfter(LocalDate.now))
      controllers.propertyLinking.routes.ClaimPropertyRelationshipController.back.url
//    keeping this because it will need to be re-implemented after VTCCA-5189 is complete
//    else if (session.fromCya.contains(true))
//      controllers.propertyLinking.routes.DeclarationController.show().url
    else
      controllers.propertyLinking.routes.ClaimPropertyOccupancyController.showOccupancy().url

  def submit: Action[AnyContent] = authenticatedAction.andThen(withLinkingSession).async { implicit request =>
    def updateSession(newAnswer: Boolean): Future[Unit] =
      // if the same answer was already in the linking session, then do nothing
      if (request.ses.hasRatesBill.contains(newAnswer)) Future.successful((): Unit)
      else {
        businessRatesAttachmentService.persistSessionData(
          request.ses.copy(hasRatesBill = Some(newAnswer), uploadEvidenceData = UploadEvidenceData.empty))
      }

    ChooseEvidence.form
      .bindFromRequest()
      .fold(
        errors => Future.successful(BadRequest(chooseEvidenceView(errors, Some(backlink(request.ses))))),
        hasRatesBill =>
          updateSession(hasRatesBill).map { _ =>
            if (hasRatesBill) Redirect(routes.UploadController.show(EvidenceChoices.RATES_BILL))
            else Redirect(routes.UploadController.show(EvidenceChoices.OTHER))
        }
      )
  }
}

object ChooseEvidence {
  lazy val form = Form(single("hasRatesBill" -> mandatoryBoolean))
}
