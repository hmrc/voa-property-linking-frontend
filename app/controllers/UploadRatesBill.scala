/*
 * Copyright 2016 HM Revenue & Customs
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

package controllers

import config.{Environment, Wiring}
import form.EnumMapping
import models.{DoesHaveRatesBill, DoesNotHaveRatesBill, HasRatesBill, RatesBill}
import play.api.data.{Form, FormError}
import play.api.data.Forms._
import play.api.i18n.Messages
import play.api.libs.Files.TemporaryFile
import play.api.mvc.{Action, AnyContent, Request}
import play.api.mvc.MultipartFormData.FilePart
import session.{LinkingSessionRequest, WithLinkingSession}

object UploadRatesBill extends PropertyLinkingController {
  lazy val uploadConnector = Wiring().fileUploadConnector
  lazy val ratesBillConnector = Wiring().ratesBillVerificationConnector

  def show() = WithLinkingSession { implicit request =>
    Ok(views.html.uploadRatesBill.show(UploadRatesBillVM(uploadRatesBillForm)))
  }

  def submit() = WithLinkingSession.async { implicit request =>
    uploadRatesBillForm.bindFromRequest().fold(
      errors => BadRequest(views.html.uploadRatesBill.show(UploadRatesBillVM(errors))),
      answer =>
        answer.hasRatesBill match {
          case DoesHaveRatesBill =>
            handleRatesBill(answer)
          case DoesNotHaveRatesBill if request.ses.claimedProperty.canReceiveMail =>
            Redirect(routes.LinkErrors.pinPostalProcess())
          case DoesNotHaveRatesBill =>
            Redirect(routes.UploadEvidence.otherProof())
        }
    )
  }

  private def handleRatesBill(s: SubmitRatesBill)(implicit req: LinkingSessionRequest[AnyContent]) =
    retrieveFile(if (Environment.isDev) req.request.body.asMultipartFormData.get.file("ratesBill") else None).flatMap {
      case Some(f) => isValid(RatesBill(f.content)) map {
        case true => Redirect(routes.UploadRatesBill.ratesBillApproved())
        case false => Redirect(routes.UploadRatesBill.ratesBillPending())
      }
      case None => BadRequest(views.html.uploadRatesBill.show(
        UploadRatesBillVM(uploadRatesBillForm.fill(s).withError(FormError("ratesBill", Messages("uploadRatesBill.ratesBillMissing.error"))))
      ))
    }

  private def retrieveFile(file: Option[FilePart[TemporaryFile]])(implicit request: LinkingSessionRequest[_]) =
    uploadConnector.retrieveFile(request.accountId, request.sessionId, "ratesBill", file)

  private def isValid(rb: RatesBill)(implicit request: LinkingSessionRequest[_]) =
    ratesBillConnector.verify(request.ses.claimedProperty.billingAuthorityReference, rb).map(_.isValid)

  def ratesBillApproved() = Action { implicit request =>
    Ok(views.html.uploadRatesBill.ratesBillApproved())
  }

  def ratesBillPending() = Action { implicit request =>
    Ok(views.html.uploadRatesBill.ratesBillPending())
  }

  lazy val uploadRatesBillForm = Form(mapping(
    "hasRatesBill" -> EnumMapping(HasRatesBill)
  )(SubmitRatesBill.apply)(SubmitRatesBill.unapply))
}

case class UploadRatesBillVM(form: Form[_])

case class SubmitRatesBill(hasRatesBill: HasRatesBill)
