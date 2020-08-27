/*
 * Copyright 2020 HM Revenue & Customs
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
import models.UploadEvidenceData
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.BusinessRatesAttachmentsService
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import scala.concurrent.ExecutionContext

class ChooseEvidence @Inject()(
      val errorHandler: CustomErrorHandler,
      authenticatedAction: AuthenticatedAction,
      withLinkingSession: WithLinkingSession,
      businessRatesAttachmentService: BusinessRatesAttachmentsService
)(
      implicit executionContext: ExecutionContext,
      override val messagesApi: MessagesApi,
      override val controllerComponents: MessagesControllerComponents,
      val config: ApplicationConfig
) extends PropertyLinkingController {

  private val logger = Logger(this.getClass.getName)

  def show: Action[AnyContent] = authenticatedAction.andThen(withLinkingSession).async { implicit request =>
    logger.debug("show choose evidence page")
    businessRatesAttachmentService
      .persistSessionData(request.ses, UploadEvidenceData.empty)
      .map(_ => Ok(views.html.propertyLinking.chooseEvidence(ChooseEvidence.form, request.ses.clientDetails)))
  }

  def submit: Action[AnyContent] = authenticatedAction.andThen(withLinkingSession) { implicit request =>
    ChooseEvidence.form
      .bindFromRequest()
      .fold(
        errors => BadRequest(views.html.propertyLinking.chooseEvidence(errors, request.ses.clientDetails)), {
          case true  => Redirect(routes.UploadController.show(EvidenceChoices.RATES_BILL))
          case false => Redirect(routes.UploadController.show(EvidenceChoices.OTHER))
        }
      )
  }
}

object ChooseEvidence {
  lazy val form = Form(single(keys.hasRatesBill -> mandatoryBoolean))
  lazy val keys = new {
    val hasRatesBill = "hasRatesBill"
  }
}
