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

import models._
import play.api.data.Forms._
import play.api.data.format.Formatter
import play.api.data.{Form, FormError, Forms}
import play.api.mvc.{Action, Result}
import session.{LinkingSession, LinkingSessionRepository, WithLinkingSession}

object Search extends PropertyLinkingController {
  val propertyToClaim = "propertyToClaim"

  def show() = Action { implicit request =>
    Ok(views.html.search(pretendSearchResults))
  }

  def declareCapacity(baRef: String) = Action.async { implicit request =>
    pretendSearchResults.find(_.billingAuthorityReference == baRef) match {
      case Some(pd) => LinkingSessionRepository.start(pd) map { _ =>
        Ok(views.html.declareCapacity(DeclareCapacityVM(declareCapacityForm)))
      }
      case None => NotFound(views.html.defaultpages.notFound(request, Some(app.Routes)))
    }
  }

  def attemptLink() = WithLinkingSession.async { implicit request =>
    declareCapacityForm.bindFromRequest().fold(
      errors => BadRequest(views.html.declareCapacity(DeclareCapacityVM(errors))),
      dec => LinkingSessionRepository.saveOrUpdate(request.ses.withDeclaration(dec)) map { _ => chooseLinkingJourney(request.ses) }
    )
  }

  private def chooseLinkingJourney(ses: LinkingSession): Result = SelfCertificationEnabled(ses.claimedProperty) match {
    case true => Redirect(routes.SelfCertification.show())
    case false => BadRequest("This journey is not implemented yet. Sorry.")
  }

  lazy val pretendSearchResults = Seq(
    Property("testselfcertifiableshop", Address(Seq("1 The Self-cert non-bank street", "The Town"), "AA11 1AA", true), Shop, false),
    Property("testconflict", Address(Seq("22 Conflict Self-cert", "The Town"), "AA11 1AA", true), Office, true),
    Property("testbank", Address(Seq("Banky McBankface", "Some Road", "Some Town"), "AA11 1AA", true), Shop, true)
  )

  implicit val capacityTypeFormatter = new Formatter[CapacityType] {
    override def bind(key: String, data: Map[String, String]) = {
      CapacityType.unapply(data.getOrElse(key, "MISSING")) match {
        case Some(ct) => Right(ct)
        case None => Left(Seq(FormError(key, "error.capacity.noValueSelected")))
      }
    }

    override def unbind(key: String, value: CapacityType) = Map(key -> value.name)
  }

  lazy val declareCapacityForm = Form(mapping(
    "capacity" -> Forms.of[CapacityType],
    "fromDate" -> nonEmptyText,
    "toDate" -> nonEmptyText
  )(CapacityDeclaration.apply)(CapacityDeclaration.unapply))
}

case class DeclareCapacityVM(form: Form[_])

case class CapacityDeclaration(capacity: CapacityType, fromDate: String, toDate: String)