/*
 * Copyright 2018 HM Revenue & Customs
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

import config.ApplicationConfig
import javax.inject.Inject
import connectors.{DraftCases, MessagesConnector}
import actions.AuthenticatedAction
import connectors.propertyLinking.PropertyLinkConnector
import play.api.i18n.MessagesApi
import models.DeleteDraftCase
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{Action, AnyContent, Request, Result}

class ManageDrafts @Inject()(authenticated: AuthenticatedAction,
                             propertyLinks: PropertyLinkConnector,
                             messagesConnector: MessagesConnector)
                            (implicit val messagesApi: MessagesApi,
                             val config: ApplicationConfig,
                             draftCases: DraftCases) extends PropertyLinkingController {


  val draftCaseForm: Form[DeleteDraftCase] = Form(
      mapping(
        "draft" -> nonEmptyText
      )(DeleteDraftCase.apply _)(DeleteDraftCase.unapply _))


  def viewDraftCases() = authenticated { implicit request =>
    for {
      cases <- draftCases.get(request.personId)
      msgCount <- messagesConnector.countUnread(request.organisationId)
    } yield {
      Ok(views.html.dashboard.draftCases(DraftCasesVM(cases), msgCount.unread, draftCaseForm))
    }
  }

  def continueCheck = Action { implicit request =>

        draftCaseForm.bindFromRequest.fold(
          formWithErrors => Ok("delete clicked - Form with errors!!"),
          test => Ok(s"delete clicked - Result was good " +
            s"${test.draft}")
        )

//    val selectedValue: String = request.body.asFormUrlEncoded.get("draft").head
//    val urls: String = request.body.asFormUrlEncoded.get(s"draft-url-${selectedValue}").head
//    Ok(s"delete clicked - Result was good ${urls}")
//    Redirect(urls, 302)
  }


  def deleteDraftCase =  Action { implicit request =>
git
    draftCaseForm.bindFromRequest.fold(
      formWithErrors => Ok("delete clicked - Form with errors!!"),
      test =>
        {
//          draftCases.delete(test.draft)

          Ok(s"delete clicked - Result was good " +
            s"${test.draft}")
        }
    )

//    val selectedValue: String = request.body.asFormUrlEncoded.get("draft").head
//    val draftCaseId: String = request.body.asFormUrlEncoded.get(s"draft-id-${selectedValue}").head
//    val deleted = draftCases.delete(draftCaseId)
//    Ok(s"delete clicked - ${draftCaseId}")
  }

}
