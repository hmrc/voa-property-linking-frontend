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
import connectors.OtherEvidenceFlag
import form.EnumMapping
import models.{DoesHaveEvidence, DoesNotHaveEvidence, HasEvidence}
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.AnyContent
import session.{LinkingSession, LinkingSessionRequest, WithLinkingSession}
import views.helpers.Errors

object UploadEvidence extends PropertyLinkingController {
  lazy val propertyLinkConnector = Wiring().propertyLinkConnector
  lazy val withLinkingSession = Wiring().withLinkingSession
  lazy val uploadConnector = Wiring().fileUploadConnectorTODODELETETHIS

  def show() = withLinkingSession { implicit request =>
    Ok(views.html.uploadEvidence.show(UploadEvidenceVM(form)))
  }

  def submit() = withLinkingSession { implicit request =>
    form.bindFromRequest().fold(
      error => BadRequest(views.html.uploadEvidence.show(UploadEvidenceVM(error))),
      uploaded => uploaded.hasEvidence match {
        case DoesHaveEvidence => verifyUploadedFiles flatMap {
          case FilesAccepted => requestLink.map(_ => Redirect(routes.UploadEvidence.evidenceUploaded()))
          case FilesRejected => BadRequest(views.html.uploadEvidence.show(UploadEvidenceVM(form.withError("evidence", Errors.uploadedFiles))))
        }
        case DoesNotHaveEvidence => requestLink.map(_ => Redirect(routes.UploadEvidence.noEvidenceUploaded()))
      }
    )
  }

  private def requestLink(implicit r: LinkingSessionRequest[AnyContent]) =
    propertyLinkConnector.linkToProperty(r.ses.claimedProperty.uarn,
      r.ses.claimedProperty.billingAuthorityReference,
      r.groupId, r.ses.declaration.getOrElse(throw new Exception("No declaration")),
      java.util.UUID.randomUUID.toString, OtherEvidenceFlag
    )

  private def verifyUploadedFiles(implicit r: LinkingSessionRequest[AnyContent]) = {
    r.body.asMultipartFormData.get.files.filter(_.key.startsWith("evidence"))
    uploadConnector.retrieveFiles(
      r.groupId, r.sessionId, "evidence",
      if (Environment.isDev || Environment.isProd) r.body.asMultipartFormData.get.files.filter(_.key.startsWith("evidence")) else Seq.empty
    ).map(x => if (x.nonEmpty && x.length <=3) FilesAccepted else FilesRejected)
  }

  def evidenceUploaded() = withLinkingSession { implicit request =>
    Wiring().fileUploadConnector.closeEnvelope(request.ses.envelopeId).flatMap( _=>
      Wiring().sessionRepository.remove().map( _ =>
        Ok(views.html.uploadEvidence.evidenceUploaded())
      )
    )
  }

  def noEvidenceUploaded() = withLinkingSession { implicit request =>
    Ok(views.html.uploadEvidence.noEvidenceUploaded())
  }

  lazy val form = Form(mapping(
    "hasEvidence" -> EnumMapping(HasEvidence)
  )(UploadedEvidence.apply)(UploadedEvidence.unapply))
}

case class UploadedEvidence(hasEvidence: HasEvidence)

case class UploadEvidenceVM(form: Form[_])

sealed trait EvidenceUploadResult
case object FilesAccepted extends EvidenceUploadResult
case object FilesRejected extends EvidenceUploadResult
