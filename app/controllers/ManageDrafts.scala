/*
 * Copyright 2020 HM Revenue & Customs
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

import actions.AuthenticatedAction
import actions.requests.BasicAuthenticatedRequest
import config.ApplicationConfig
import connectors.DraftCases
import connectors.propertyLinking.PropertyLinkConnector
import javax.inject.Inject
import models.DeleteDraftCase
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.mvc.{MessagesControllerComponents, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.voa.propertylinking.errorhandler.CustomErrorHandler

import scala.concurrent.{ExecutionContext, Future}

class ManageDrafts @Inject()(
                              val errorHandler: CustomErrorHandler,
                              authenticated: AuthenticatedAction,
                              propertyLinks: PropertyLinkConnector
                            )(
                              implicit executionContext: ExecutionContext,
                              override val messagesApi: MessagesApi,
                              override val controllerComponents: MessagesControllerComponents,
                              val config: ApplicationConfig,
                              draftCases: DraftCases
                            ) extends PropertyLinkingController {

  val draftCaseForm: Form[DeleteDraftCase] = Form(
    mapping(
      "draft" -> optional(text).verifying("error.common.noValueSelected", s => s.isDefined && s.nonEmpty)
        .transform[String](_.get, x => Some(x.toString))
    )(DeleteDraftCase.apply)(DeleteDraftCase.unapply))


  private def getIdUrl(value: String): (String, String) = value.split('?') match {
    case Array(id, url) => (id, url)
    case _ => ("Invalid Id", "Invalid Url")
  }


  def viewDraftCases() = authenticated { implicit request =>
    Redirect(config.newDashboardUrl("your-drafts"))
  }


  def continueCheck = authenticated.async { implicit request =>
    draftCaseForm.bindFromRequest.fold(
      getDraftCases,
      success => Future.successful(Redirect(config.checkUrl + getIdUrl(success.draft)._2)))
  }


  def deleteDraftCase = authenticated.async { implicit request =>
    draftCaseForm.bindFromRequest.fold(
      getDraftCases,
      success => {
        val userSelectedId = getIdUrl(success.draft)._1
        for {
          cases <- draftCases.get(request.personId)
        } yield {
          val selectedCase = cases.filter(_.id == userSelectedId).head
          Ok(views.html.dashboard.confirmDeleteDraftCase(selectedCase))
        }
      }
    ).recover {
      case _ => Redirect(routes.ManageDrafts.viewDraftCases())
    }
  }


  def confirmDelete(draftId: String) = authenticated.async { implicit request =>
    draftCases.delete(draftId).map(_ => Redirect(routes.ManageDrafts.viewDraftCases()))
      .recover {
        case _ => Redirect(routes.ManageDrafts.viewDraftCases())
      }
  }


  private def getDraftCases(form: Form[DeleteDraftCase])
                           (implicit request: BasicAuthenticatedRequest[_], hc: HeaderCarrier): Future[Result] =
    for {
      cases <- draftCases.get(request.personId)
    } yield {
      BadRequest(views.html.dashboard.draftCases(DraftCasesVM(cases), form, ""))
    }

}
