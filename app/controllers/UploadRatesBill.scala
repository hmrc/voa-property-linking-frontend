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
import connectors.RatesBillFlag
import form.EnumMapping
import models.{DoesHaveRatesBill, DoesNotHaveRatesBill, HasRatesBill}
import play.api.data.Forms._
import play.api.data.{Form, FormError}
import play.api.i18n.Messages
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.{Action, AnyContent}
import session.{LinkingSessionRequest, WithLinkingSession}

import scala.concurrent.Future

object UploadRatesBill extends PropertyLinkingController {
  lazy val uploadConnector = Wiring().fileUploadConnector
  lazy val ratesBillConnector = Wiring().ratesBillVerificationConnector
  lazy val propertyLinkConnector = Wiring().propertyLinkConnector

  def show() = WithLinkingSession { implicit request =>
    Ok(views.html.uploadRatesBill.show(UploadRatesBillVM(uploadRatesBillForm)))
  }

  def submit() = WithLinkingSession { implicit request =>
    uploadRatesBillForm.bindFromRequest().fold(
      errors => BadRequest(views.html.uploadRatesBill.show(UploadRatesBillVM(errors))),
      answer => handle(answer) flatMap {
        case RatesBillUploaded =>
          requestLink map { _ => Redirect(routes.UploadRatesBill.ratesBillUploaded()) }
        case NoRatesBill =>
          Redirect(routes.UploadEvidence.show())
        case RatesBillMissing(s) =>
          BadRequest(views.html.uploadRatesBill.show(
            UploadRatesBillVM(uploadRatesBillForm.fill(s).withError(FormError("ratesBill", Messages("uploadRatesBill.ratesBillMissing.error"))))
          ))
      }
    )
  }

  private def handle(answer: SubmitRatesBill)(implicit req: LinkingSessionRequest[AnyContent]): Future[RatesBillUploadResult] =
    answer.hasRatesBill match {
      case DoesHaveRatesBill => retrieveFile(if (!Environment.isTest) req.request.body.asMultipartFormData.get.file("ratesBill") else None).map {
        case f :: _ => RatesBillUploaded
        case Nil => RatesBillMissing(answer)
      }
      case DoesNotHaveRatesBill => NoRatesBill
    }

  private def retrieveFile(file: Option[FilePart[TemporaryFile]])(implicit request: LinkingSessionRequest[_]) =
    uploadConnector.retrieveFiles(request.groupId, request.sessionId, "ratesBill", file.map(Seq(_)).getOrElse(Seq.empty))

  private def requestLink(implicit req: LinkingSessionRequest[AnyContent]) =
    propertyLinkConnector.linkToProperty(
      req.ses.claimedProperty.uarn,
      req.ses.claimedProperty.billingAuthorityReference, req.groupId,
      req.ses.declaration.getOrElse(throw new Exception("No declaration")),
      java.util.UUID.randomUUID.toString, RatesBillFlag
    )

  def ratesBillUploaded() = Action { implicit request =>
    Ok(views.html.uploadRatesBill.ratesBillUploaded())
  }

  lazy val uploadRatesBillForm = Form(mapping(
    "hasRatesBill" -> EnumMapping(HasRatesBill)
  )(SubmitRatesBill.apply)(SubmitRatesBill.unapply))
}

case class UploadRatesBillVM(form: Form[_])

case class SubmitRatesBill(hasRatesBill: HasRatesBill)

sealed trait RatesBillUploadResult
case object RatesBillUploaded extends RatesBillUploadResult
case object NoRatesBill extends RatesBillUploadResult
case class RatesBillMissing(s: SubmitRatesBill) extends RatesBillUploadResult

