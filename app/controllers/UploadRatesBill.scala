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

import models.Property
import play.api.mvc.{Action, Result}
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData.FilePart
import session.WithLinkingSession

object UploadRatesBill extends PropertyLinkingController {

  def show() = WithLinkingSession { implicit request =>
    Ok(views.html.uploadRatesBill.show(UploadRatesBillVM(uploadRatesBillForm)))
  }

  def submit() = WithLinkingSession(parse.multipartFormData) { implicit request =>
    uploadRatesBillForm.bindFromRequest().fold(
      errors => BadRequest(views.html.uploadRatesBill.show(UploadRatesBillVM(uploadRatesBillForm))),
      answer => chooseNextStep(answer.hasRatesBill, request.body.file("ratesBill"), request.ses.claimedProperty)
    )
  }

  private def chooseNextStep(hasRatesBill: Boolean, ratesBill: Option[FilePart[TemporaryFile]], p: Property): Result =
    if (hasRatesBill && ratesBill.isDefined && p == Search.bankForRatesBillVerifiedJourney)
      Redirect(routes.UploadRatesBill.ratesBillApproved())
    else if (hasRatesBill && ratesBill.isDefined)
      Redirect(routes.UploadRatesBill.ratesBillPending())
    else
      BadRequest("Rates bill verification failure journey not implemented yet")

  def ratesBillApproved() = Action { implicit request =>
    Ok(views.html.uploadRatesBill.ratesBillApproved())
  }

  def ratesBillPending() = Action { implicit request =>
    Ok(views.html.uploadRatesBill.ratesBillPending())
  }

  lazy val uploadRatesBillForm = Form(mapping(
    "hasRatesBill" -> boolean
  )(SubmitRatesBill.apply)(SubmitRatesBill.unapply))
}

case class UploadRatesBillVM(form: Form[_])

case class SubmitRatesBill(hasRatesBill: Boolean)
