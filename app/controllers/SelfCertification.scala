/*
 * Copyright 2017 HM Revenue & Customs
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
import form.Mappings.trueOnly
import models.{Property, SelfCertifyFlag}
import play.api.data.Form
import play.api.data.Forms._
import session.{LinkingSession, LinkingSessionRequest}
import uk.gov.hmrc.play.http.HeaderCarrier
import views.helpers.Errors

object SelfCertification extends PropertyLinkingController {
  lazy val connector = Wiring().propertyLinkConnector
  lazy val withLinkingSession = Wiring().withLinkingSession
  lazy val repo = Wiring().sessionRepository

  def show() = withLinkingSession { implicit request =>
    Ok(views.html.selfCertification.show(SelfCertifyVM(selfCertifyForm, request.ses)))
  }

  def submit() = withLinkingSession { implicit request =>
    selfCertifyForm.bindFromRequest().fold(
      errors => BadRequest(views.html.selfCertification.show(SelfCertifyVM(errors, request.ses))),
      _ => for {
        _ <- link(request)
        _ <- repo.saveOrUpdate(request.ses.copy(selfCertifyComplete = Some(true)))
      } yield Redirect(routes.SelfCertification.selfCertified())
    )
  }

  private def link(request: LinkingSessionRequest[_])(implicit hc: HeaderCarrier) =
    connector.linkToProperty(request.ses.claimedProperty, request.account.organisationId, request.account.individualId,
      request.ses.declaration.getOrElse(throw new Exception("No declaration")),
      request.ses.submissionId, SelfCertifyFlag,None
    )

  def selfCertified() = withLinkingSession { implicit request =>
    request.ses.selfCertifyComplete.contains(true) match {
      case true =>
        Ok(views.html.linkingRequestSubmitted())
      case false => Redirect(routes.Dashboard.home())
    }
  }

  lazy val selfCertifyForm = Form(mapping(
    "iAgree" -> trueOnly(Errors.mustAgreeToSelfCert)
  )(ConfirmSelfCertification.apply)(ConfirmSelfCertification.unapply))
}

case class ConfirmSelfCertification(iAgree: Boolean)

case class SelfCertifyVM(form: Form[_], session: LinkingSession)

case class LinkAuthorisedVM(linkedProperty: Property)
