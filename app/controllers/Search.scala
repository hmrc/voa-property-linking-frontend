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
import connectors.PrototypeTestData._
import connectors.propertyLinking.ServiceContract.CapacityDeclaration
import form.EnumMapping
import form.Mappings.{dmyDate, dmyPastDate}
import models._
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{Action, Result}
import session.WithLinkingSession

object Search extends PropertyLinkingController {
  lazy val sessionRepository = Wiring().sessionRepository
  lazy val connector = Wiring().propertyConnector

  def show() = Action { implicit request =>
    Ok(views.html.search(pretendSearchResults))
  }

  def declareCapacity(baRef: String) = Action.async { implicit request =>
    connector.find(baRef).flatMap {
      case Some(pd) => sessionRepository.start(pd) map { _ =>
        Ok(views.html.declareCapacity(DeclareCapacityVM(declareCapacityForm)))
      }
      case None => NotFound(views.html.defaultpages.notFound(request, Some(app.Routes)))
    }
  }

  def attemptLink() = WithLinkingSession.async { implicit request =>
    declareCapacityForm.bindFromRequest().fold(
      errors => BadRequest(views.html.declareCapacity(DeclareCapacityVM(errors))),
      dec => sessionRepository.saveOrUpdate(request.ses.withDeclaration(dec)) map { _ => chooseLinkingJourney(request.ses.claimedProperty, dec) }
    )
  }

  private def chooseLinkingJourney(p: Property, d: CapacityDeclaration): Result =
    if (IsAlreadyLinked(p, d))
      Redirect(routes.LinkErrors.conflict())
    else if (SelfCertificationEnabled(p))
      Redirect(routes.SelfCertification.show())
    else
      Redirect(routes.UploadRatesBill.show())

  lazy val declareCapacityForm = Form(mapping(
    "capacity" -> EnumMapping(CapacityType),
    "fromDate" -> dmyPastDate,
    "toDate" -> optional(dmyDate)
  )(CapacityDeclaration.apply)(CapacityDeclaration.unapply))
}

case class DeclareCapacityVM(form: Form[_])