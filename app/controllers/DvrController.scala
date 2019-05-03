/*
 * Copyright 2019 HM Revenue & Customs
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

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import actions.AuthenticatedAction
import config.ApplicationConfig
import connectors._
import connectors.propertyLinking.PropertyLinkConnector
import javax.inject.Inject

import models.dvr.DetailedValuationRequest
import play.api.http.HttpEntity.Streamed
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, ResponseHeader, Result}

import scala.concurrent.Future

class DvrController @Inject()(
    propertyLinks: PropertyLinkConnector,
    authenticated: AuthenticatedAction,
    submissionIds: SubmissionIdConnector,
    dvrCaseManagement: DVRCaseManagementConnector)(
    implicit val messagesApi: MessagesApi,
    val config: ApplicationConfig)
    extends PropertyLinkingController {

  def detailedValuationRequestCheck(
                                     authId: Long,
                                     valuationId: Long,
                                     baRef: String): Action[AnyContent] = authenticated {
    implicit request =>
      propertyLinks.getLink(authId).flatMap {
        case Some(link) =>
          dvrCaseManagement
            .getDvrDocuments(link.uarn, valuationId, link.submissionId)
            .map {
              case Some(documents)  =>
                Ok(views.html.dvr.auto.downloadDvrFiles(
                  AvailableRequestDetailedValuation(
                    documents.checkForm.documentSummary.documentId,
                    documents.detailedValuation.documentSummary.documentId,
                    authId,
                    valuationId,
                    baRef,
                    link.address)))
              case None             => {
                val assessment = link.assessments.find(a => a.assessmentRef == valuationId).
                  getOrElse(throw new IllegalStateException(s"Assessment with ref: $valuationId does not exist"))
                val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy")

                Redirect(
                  routes.DvrController
                    .alreadySubmittedDetailedValuationRequest(authId, valuationId, baRef, link.address,
                      assessment.effectiveDate.format(formatter), assessment.rateableValue))
              }
            }
        case None       =>
          Future.successful(BadRequest(views.html.errors.propertyMissing()))
      }
  }

  def requestDetailedValuation(
                                authId: Long,
                                valuationId: Long,
                                baRef: String): Action[AnyContent] =
    authenticated { implicit request =>
      for {
        submissionId  <- submissionIds.get("DVR")
        agents        <- propertyLinks.getLink(authId).map(opt => opt.toList.flatMap(_.agents.map(_.organisationId)))
        dvr           = DetailedValuationRequest(authId,
                                        request.organisationId,
                                        request.personId,
                                        submissionId,
                                        valuationId,
                                        agents,
                                        baRef)
        _             <- dvrCaseManagement.requestDetailedValuationV2(dvr)
      } yield Redirect(routes.DvrController.confirmation(authId, submissionId))

    }

  def confirmation(
                    authId: Long,
                    submissionId: String) = authenticated {
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
                                                authId: Long,
                                                valuationId: Long,
      baRef: String, address: String, effectiveDate: String, rateableValue: Option[Long]): Action[AnyContent] = authenticated { implicit request =>
    dvrCaseManagement
      .dvrExists(request.organisationAccount.id, valuationId)
      .map { exists =>
        if (exists) {
          Ok(views.html.dvr.auto.duplicateRequestDetailedValuationAuto(authId))
        } else {
          Ok(views.html.dvr.auto.requestDetailedValuationAuto(
            RequestDetailedValuationWithoutForm(authId, valuationId, baRef, address, effectiveDate, rateableValue)))
        }
      }
  }

  def requestDvrFile(valuationId: Long,
                     authId: Long,
                     fileRef: String): Action[AnyContent] = authenticated {
    implicit request =>
      propertyLinks.getLink(authId).flatMap {
        case Some(link) =>
          dvrCaseManagement
            .getDvrDocument(link.uarn, valuationId, link.submissionId, fileRef)
            .map { document =>
              Result(ResponseHeader(200, document.headers.updated(CONTENT_DISPOSITION, s"""attachment;filename="${link.submissionId}.pdf"""")),
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
                                               baRef: String,
                                               address: String,
                                               effectiveDate: String,
                                               rateableValue: Option[Long])

case class AvailableRequestDetailedValuation(
                                            check: String,
                                            valuation: String,
                                            authId: Long,
                                            valuationId: Long,
                                            baRef: String,
                                            address: String
                                            )
