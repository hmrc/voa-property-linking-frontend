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

import config.Wiring
import connectors.propertyLinking.ServiceContract.LinkToProperty
import form.EnumMapping
import models.{DoesHaveEvidence, DoesNotHaveEvidence, HasEvidence}
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.AnyContent
import session.{LinkingSessionRequest, WithLinkingSession}

object UploadEvidence extends PropertyLinkingController {
  lazy val propertyLinkConnector = Wiring().propertyLinkConnector

  def show() = WithLinkingSession { implicit request =>
    Ok(views.html.uploadEvidence.show(UploadEvidenceVM(form)))
  }

  def submit() = WithLinkingSession.async { implicit request =>
    form.bindFromRequest().fold(
      error => BadRequest(views.html.uploadEvidence.show(UploadEvidenceVM(error))),
      uploaded => requestLink map { _ => uploaded.hasEvidence match {
        case DoesHaveEvidence => Redirect(routes.UploadEvidence.evidenceUploaded())
        case DoesNotHaveEvidence => Redirect(routes.UploadEvidence.noEvidenceUploaded())
      }}
    )
  }

  private def requestLink(implicit r: LinkingSessionRequest[AnyContent]) =
    propertyLinkConnector.linkToProperty(
      r.ses.claimedProperty.billingAuthorityReference,
      r.accountId, LinkToProperty(r.ses.declaration.getOrElse(throw new Exception("No declaration"))),
      java.util.UUID.randomUUID.toString
    )

  def evidenceUploaded() = WithLinkingSession { implicit request =>
    Ok(views.html.uploadEvidence.evidenceUploaded())
  }

  def noEvidenceUploaded() = WithLinkingSession { implicit request =>
    Ok(views.html.uploadEvidence.noEvidenceUploaded())
  }

  lazy val form = Form(mapping(
    "hasEvidence" -> EnumMapping(HasEvidence)
  )(UploadedEvidence.apply)(UploadedEvidence.unapply))
}

case class UploadedEvidence(hasEvidence: HasEvidence)

case class UploadEvidenceVM(form: Form[_])
