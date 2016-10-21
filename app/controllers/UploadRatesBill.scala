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

import java.nio.file.{Files, Paths}

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

trait UploadRatesBill extends PropertyLinkingController {
  lazy val fileUploadConnector = Wiring().fileUploadConnector
  lazy val propertyLinkConnector = Wiring().propertyLinkConnector
  lazy val ratesBillConnector = Wiring().ratesBillVerificationConnector
  lazy val sessionRepository = Wiring().sessionRepository
  lazy val withLinkingSession = Wiring().withLinkingSession
  lazy val fileSystemConnector = Wiring().fileSystemConnector

  def show() = withLinkingSession { implicit request =>
    Ok(views.html.uploadRatesBill.show(UploadRatesBillVM(uploadRatesBillForm)))
  }

  def submit() = withLinkingSession { implicit request =>
    uploadRatesBillForm.bindFromRequest().fold(
      errors => BadRequest(views.html.uploadRatesBill.show(UploadRatesBillVM(errors))),
      answer => handle(answer) flatMap { x =>
        x match {

        case RatesBillUploaded =>
          requestLink map { _ => Redirect(routes.UploadRatesBill.ratesBillUploaded()) }
        case NoRatesBill =>
          Redirect(routes.UploadEvidence.show())
        case RatesBillMissing(s) =>
          BadRequest(views.html.uploadRatesBill.show(
            UploadRatesBillVM(uploadRatesBillForm.fill(s).withError(FormError("ratesBill", Messages("uploadRatesBill.ratesBillMissing.error"))))
          ))
      }
      }
    )
  }

  private def handle(answer: SubmitRatesBill)(implicit req: LinkingSessionRequest[AnyContent]): Future[RatesBillUploadResult] = {
    answer.hasRatesBill match {
      case DoesHaveRatesBill => {

        uploadFile(req.request.body.asMultipartFormData.get.file("ratesBill"), answer).map { x =>
        x
        }
      }
      case DoesNotHaveRatesBill => NoRatesBill
    }
  }

  private def uploadFile(file: Option[FilePart[TemporaryFile]], answer: SubmitRatesBill)(implicit request: LinkingSessionRequest[_]) = {
    file.map(filepart => {
      val envId = request.ses.envelopeId
      val content = fileSystemConnector.readAllBytes(filepart.ref.file.toPath)
      val contentType = filepart.contentType.getOrElse("application/octet-stream")
      fileUploadConnector.uploadFile(envId, filepart.filename, content, contentType,filepart.ref.file ) map (_ =>
        RatesBillUploaded
        )
    }).getOrElse(Future.successful(RatesBillMissing(answer)))
  }

  private def requestLink(implicit req: LinkingSessionRequest[AnyContent]) =
    propertyLinkConnector.linkToProperty(
      req.ses.claimedProperty.uarn,
      req.ses.claimedProperty.billingAuthorityReference, req.groupId,
      req.ses.declaration.getOrElse(throw new Exception("No declaration")),
      java.util.UUID.randomUUID.toString, RatesBillFlag
    )

  def ratesBillUploaded() = withLinkingSession { implicit request =>
    fileUploadConnector.closeEnvelope(request.ses.envelopeId).flatMap( _=>
      sessionRepository.remove().map( _ =>
      Ok(views.html.uploadRatesBill.ratesBillUploaded())
      )
    )
  }

  lazy val uploadRatesBillForm = Form(mapping(
    "hasRatesBill" -> EnumMapping(HasRatesBill)
  )(SubmitRatesBill.apply)(SubmitRatesBill.unapply))
}

object UploadRatesBill extends UploadRatesBill

case class UploadRatesBillVM(form: Form[_])

case class SubmitRatesBill(hasRatesBill: HasRatesBill)

sealed trait RatesBillUploadResult
case object RatesBillUploaded extends RatesBillUploadResult
case object NoRatesBill extends RatesBillUploadResult
case class RatesBillMissing(s: SubmitRatesBill) extends RatesBillUploadResult

