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
import actions.{AuthenticatedAction, BasicAuthenticatedRequest}
import views.helpers.Errors
import akka.actor.Status.{Failure, Success}
import connectors.propertyLinking.PropertyLinkConnector
import play.api.i18n.MessagesApi
import models.DeleteDraftCase
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class ManageDrafts @Inject()(authenticated: AuthenticatedAction,
                             propertyLinks: PropertyLinkConnector,
                             messagesConnector: MessagesConnector)
                            (implicit val messagesApi: MessagesApi,
                             val config: ApplicationConfig,
                             draftCases: DraftCases) extends PropertyLinkingController {

  val draftCaseForm: Form[DeleteDraftCase] = Form(
      mapping(
        "draft" -> optional(text).verifying("error.common.noValueSelected", s => s.isDefined &&  s.nonEmpty)
                      .transform[String](_.get, x => Some(x.toString))
      )(DeleteDraftCase.apply _)(DeleteDraftCase.unapply _))

  private def getIdUrl(value: String): (String, String) = value.split('?') match {
    case Array(id, url) => (id, url)
    case _ => ("Invalid Id", "Invalid Url")
  }

  def viewDraftCases() = authenticated { implicit request =>
    getDraftCases(draftCaseForm)
  }

  def continueCheck = authenticated { implicit request =>
    draftCaseForm.bindFromRequest.fold(
      formWithErrors => getDraftCases(formWithErrors),
      success        => Redirect(getIdUrl(success.draft)._2))
    }

  def deleteDraftCase =  authenticated { implicit request =>
    draftCaseForm.bindFromRequest.fold(
      formWithErrors => getDraftCases(formWithErrors),
      success        => draftCases.delete(getIdUrl(success.draft)._1).map(_ =>
                           Redirect(routes.ManageDrafts.viewDraftCases))
    ).recover {
      case _ => Redirect(routes.ManageDrafts.viewDraftCases)
    }
  }

  private def getDraftCases(form: Form[DeleteDraftCase])
                           (implicit request: BasicAuthenticatedRequest[_], hc: HeaderCarrier) =
    for {
      cases <- draftCases.get(request.personId)
      msgCount <- messagesConnector.countUnread(request.organisationId)
    } yield {
      BadRequest(views.html.dashboard.draftCases(DraftCasesVM(cases), msgCount.unread, form))
    }

}
