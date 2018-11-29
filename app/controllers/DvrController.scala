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

import actions.AuthenticatedAction
import config.ApplicationConfig
import connectors._
import connectors.propertyLinking.PropertyLinkConnector
import javax.inject.Inject
import models.dvr.DetailedValuationRequest
import models.dvr.documents.DvrDocumentFiles
import play.api.http.HttpEntity.Streamed
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, ResponseHeader, Result}

import scala.concurrent.Future

class DvrController @Inject()(
    propertyLinks: PropertyLinkConnector,
    authenticated: AuthenticatedAction,
    submissionIds: SubmissionIdConnector,
    dvrCaseManagement: DVRCaseManagementConnector,
    businessRatesValuations: BusinessRatesValuationConnector,
    checkCaseConnector: CheckCaseConnector,
    businessRatesAuthorisation: BusinessRatesAuthorisation)(
    implicit val messagesApi: MessagesApi,
    val config: ApplicationConfig)
    extends PropertyLinkingController {

  def detailedValuationRequestCheck(authId: Long,
                                    valuationId: Long,
                                    baRef: String) = authenticated {
    implicit request =>
      dvrCaseManagement
        .dvrExists(request.organisationAccount.id, valuationId)
        .map { exists =>
          if (exists) {
            Redirect(
              routes.DvrController
                .alreadySubmittedDetailedValuationRequest(valuationId, authId, baRef))
          } else {
            Ok(views.html.dvr.auto.requestDetailedValuationAuto(
              RequestDetailedValuationWithoutForm(authId, valuationId, baRef)))
          }
        }
  }

  def requestDetailedValuation(valuationId: Long, authId: Long, baRef: String) =
    authenticated { implicit request =>
      for {
        submissionId <- submissionIds.get("DVR")
        dvr = DetailedValuationRequest(authId,
                                       request.organisationId,
                                       request.personId,
                                       submissionId,
                                       valuationId,
                                       baRef)
        _ <- dvrCaseManagement.requestDetailedValuation(dvr)
      } yield {
        Redirect(routes.DvrController.confirmation(authId, submissionId))
      }
    }

  def confirmation(authId: Long, submissionId: String) = authenticated {
    implicit request =>
      propertyLinks.getLink(authId).map {
        case Some(link) =>
          Ok(
            views.html.dvr.auto.detailedValuationRequestedAuto(submissionId,
                                                               link.address))
        case None =>
          BadRequest(views.html.errors.propertyMissing())
      }
  }

  def alreadySubmittedDetailedValuationRequest(
      valuationId: Long,
      authId: Long,
      baRef: String): Action[AnyContent] = authenticated { implicit request =>
    propertyLinks.getLink(authId).flatMap {
      case Some(link) =>
        dvrCaseManagement
          .getDvrDocuments(link.uarn, valuationId, link.submissionId)
          .map {
            case Some(documents) =>
              Ok(views.html.dvr.auto.downloadDvrFiles(
                AvailableRequestDetailedValuation(
                  documents.checkForm.documentSummary.documentId,
                  documents.detailedValuation.documentSummary.documentId,
                  authId,
                  valuationId,
                  baRef,
                  link.address)))
            case None =>
              Ok(views.html.dvr.auto.duplicateRequestDetailedValuationAuto())
          }
      case None =>
        //Add Logger
        Future.successful(BadRequest(views.html.errors.propertyMissing())) //This page cannot be displayed.
    }
  }

  def requestDvrFile(valuationId: Long,
                     authId: Long,
                     fileRef: Long): Action[AnyContent] = authenticated {
    implicit request =>
      propertyLinks.getLink(authId).flatMap {
        case Some(link) =>
          dvrCaseManagement
            .getDvrDocument(link.uarn, valuationId, link.submissionId, fileRef)
            .map { document =>
              Result(ResponseHeader(200, document.headers),
                     Streamed(document.body,
                              document.contentLength,
                              document.contentType))
            }
        case None =>
          Future.successful(BadRequest(views.html.errors.propertyMissing()))
      }
  }
}

case class RequestDetailedValuationWithoutForm(authId: Long,
                                               assessmentRef: Long,
                                               baRef: String)

case class AvailableRequestDetailedValuation(
                                            check: Long,
                                            valuation: Long,
                                            authId: Long,
                                            valuationId: Long,
                                            baRef: String,
                                            address: String
                                            )
