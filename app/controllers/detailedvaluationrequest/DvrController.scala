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

package controllers.detailedvaluationrequest

import java.time.format.DateTimeFormatter

import actions.AuthenticatedAction
import config.ApplicationConfig
import connectors._
import connectors.propertyLinking.PropertyLinkConnector
import controllers.PropertyLinkingController
import javax.inject.Inject
import models.dvr.DetailedValuationRequest
import play.api.Logger
import play.api.http.HttpEntity.Streamed
import play.api.i18n.MessagesApi
import play.api.mvc._
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}

import scala.concurrent.Future

class DvrController @Inject()(
    propertyLinks: PropertyLinkConnector,
    authenticated: AuthenticatedAction,
    submissionIds: SubmissionIdConnector,
    dvrCaseManagement: DVRCaseManagementConnector,
    businessRatesAuthorisation: BusinessRatesAuthorisation)(
    implicit val messagesApi: MessagesApi,
    val config: ApplicationConfig)
    extends PropertyLinkingController {

  private val logger = Logger(this.getClass.getName)

  def myOrganisationRequestDetailValuationCheck(propertyLinkSubmissionId: String, valuationId: Long): Action[AnyContent] =
    detailedValuationRequestCheck(propertyLinkSubmissionId, valuationId, true)

  def myClientsRequestDetailValuationCheck(propertyLinkSubmissionId: String, valuationId: Long): Action[AnyContent] =
    detailedValuationRequestCheck(propertyLinkSubmissionId, valuationId, false)

  private def detailedValuationRequestCheck(propertyLinkSubmissionId: String, valuationId: Long, owner: Boolean): Action[AnyContent] = authenticated.async {
    implicit request =>
      val pLink = if(owner) propertyLinks.getOwnerAssessments(propertyLinkSubmissionId) else propertyLinks.getClientAssessments(propertyLinkSubmissionId)

      pLink.flatMap {
        case Some(link) =>
          for {
            optDocuments <- dvrCaseManagement.getDvrDocuments(link.uarn, valuationId, link.submissionId)
            backUrl      <- calculateBackLink(propertyLinkSubmissionId, owner)
          } yield optDocuments match {
              case Some(documents)  =>
                Ok(views.html.dvr.dvr_files(
                  model = AvailableRequestDetailedValuation(
                    documents.checkForm.documentSummary.documentId,
                    documents.detailedValuation.documentSummary.documentId,
                    valuationId,
                    link.assessments.head.billingAuthorityReference,
                    link.address),
                  submissionId = propertyLinkSubmissionId,
                  owner = owner,
                  backUrl = backUrl))
              case None             =>
                Redirect(
                  if (owner)
                    controllers.detailedvaluationrequest.routes.DvrController.myOrganisationAlreadyRequestedDetailValuation(propertyLinkSubmissionId = propertyLinkSubmissionId, valuationId = valuationId)
                  else
                    controllers.detailedvaluationrequest.routes.DvrController.myClientsAlreadyRequestedDetailValuation(propertyLinkSubmissionId = propertyLinkSubmissionId, valuationId = valuationId)
                )
              }
        case None       =>
          Future.successful(BadRequest(views.html.errors.propertyMissing()))
      }
  }

  def myOrganisationRequestDetailValuation(propertyLinkSubmissionId: String, valuationId: Long): Action[AnyContent] =
    requestDetailedValuation(propertyLinkSubmissionId, valuationId, true)

  def myClientsRequestDetailValuation(propertyLinkSubmissionId: String, valuationId: Long): Action[AnyContent] =
    requestDetailedValuation(propertyLinkSubmissionId, valuationId, false)

  private def requestDetailedValuation(
                                propertyLinkSubmissionId: String,
                                valuationId: Long,
                                owner: Boolean
                              ): Action[AnyContent] = authenticated.async { implicit request =>
      for {
        submissionId  <- submissionIds.get("DVR")
        pLink         <-  if(owner) propertyLinks.getOwnerAssessments(propertyLinkSubmissionId) else propertyLinks.getClientAssessments(propertyLinkSubmissionId)
        agents        = pLink.map(opt => opt.agents.map(_.organisationId).toList).getOrElse(List.empty[Long])
        dvr           = pLink.map { propertyLink =>
          DetailedValuationRequest(
            propertyLink.authorisationId,
            request.organisationId,
            request.personId,
            submissionId,
            valuationId,
            agents,
            propertyLink.assessments.head.billingAuthorityReference)
        }
        result        <- dvr.map(dvrCaseManagement.requestDetailedValuationV2).getOrElse(Future.failed(throw new NotFoundException("property link does not exist")))
      } yield {
        pLink match {
          case Some(p) =>
            Redirect(
              if (owner)
                routes.DvrController.myOrganisationRequestDetailValuationConfirmation(propertyLinkSubmissionId, submissionId)
              else
                routes.DvrController.myClientsRequestDetailValuationConfirmation(propertyLinkSubmissionId, submissionId)
            )
          case None    =>
            notFound
        }
      }

    }

  def myOrganisationRequestDetailValuationConfirmation(propertyLinkSubmissionId: String, submissionId: String): Action[AnyContent] =
    confirmation(propertyLinkSubmissionId, submissionId, true)

  def myClientsRequestDetailValuationConfirmation(propertyLinkSubmissionId: String, submissionId: String): Action[AnyContent] =
    confirmation(propertyLinkSubmissionId, submissionId, false)

  private def confirmation(
                    propertyLinkSubmissionId: String,
                    submissionId: String,
                    owner: Boolean
                  ): Action[AnyContent] = authenticated.async { implicit request =>
    val pLink = if(owner) propertyLinks.getOwnerAssessments(propertyLinkSubmissionId) else propertyLinks.getClientAssessments(propertyLinkSubmissionId)
      pLink.map {
        case Some(link) =>
          Ok(views.html.dvr.requested_detailed_valuation(submissionId, link.address))
        case None       =>
          BadRequest(views.html.errors.propertyMissing())
      }
  }

  def myOrganisationAlreadyRequestedDetailValuation(propertyLinkSubmissionId: String, valuationId: Long): Action[AnyContent] =
    alreadySubmittedDetailedValuationRequest(propertyLinkSubmissionId, valuationId, true)

  def myClientsAlreadyRequestedDetailValuation(propertyLinkSubmissionId: String, valuationId: Long): Action[AnyContent] =
    alreadySubmittedDetailedValuationRequest(propertyLinkSubmissionId, valuationId, false)


  private def alreadySubmittedDetailedValuationRequest(
                                                submissionId: String,
                                                valuationId: Long,
                                                owner: Boolean
                                              ): Action[AnyContent] = authenticated.async { implicit request =>
    val pLink = if(owner) propertyLinks.getOwnerAssessments(submissionId) else propertyLinks.getClientAssessments(submissionId)
    pLink.flatMap {
      case Some(link) =>
        val assessment = link.assessments.find(a => a.assessmentRef == valuationId).
          getOrElse(throw new IllegalStateException(s"Assessment with ref: $valuationId does not exist"))
        val effectiveDate = assessment.effectiveDate.
          getOrElse(throw new IllegalStateException(s"Assessment with ref: $valuationId does not contain an Effective Date"))
        val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy")

        for {
          exists <- dvrCaseManagement.dvrExists(request.organisationAccount.id, valuationId)
          backUrl <- calculateBackLink(submissionId, owner)
        } yield {
          if (exists) {
            Ok(views.html.dvr.already_requested_detailed_valuation(backUrl))
          } else {
            Ok(views.html.dvr.request_detailed_valuation(submissionId, RequestDetailedValuationWithoutForm(valuationId, link.address, formatter.format(effectiveDate), assessment.rateableValue), owner, backUrl))
          }
        }
      case None       =>
        Future.successful(BadRequest(views.html.errors.propertyMissing()))
    }

  }

  def myOrganisationRequestDetailedValuationRequestFile(propertyLinkSubmissionId: String, valuationId: Long, fileRef: String): Action[AnyContent] =
    requestDvrFile(propertyLinkSubmissionId, valuationId, fileRef, true)

  def myClientsRequestDetailedValuationRequestFile(propertyLinkSubmissionId: String, valuationId: Long, fileRef: String): Action[AnyContent] =
    requestDvrFile(propertyLinkSubmissionId, valuationId, fileRef, false)

  private def requestDvrFile(
                      submissionId: String,
                      valuationId: Long,
                      fileRef: String,
                      owner: Boolean
                    ): Action[AnyContent] = authenticated.async {
    implicit request =>
      val pLink = if(owner) propertyLinks.getOwnerAssessments(submissionId) else propertyLinks.getClientAssessments(submissionId)
      pLink.flatMap {
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

  private def calculateBackLink(submissionId: String, isOwner: Boolean)(implicit hc: HeaderCarrier): Future[String] = {
    val linkFOpt = if (isOwner) propertyLinks.getOwnerAssessments(submissionId) else propertyLinks.getClientAssessments(submissionId)

    linkFOpt.map {
      case Some(link) if link.assessments.size == 1 =>
        config.newDashboardUrl(if(!isOwner) "client-properties" else "your-properties")
      case _                                        =>
        controllers.routes.Assessments.assessments(submissionId, isOwner).url
    }
  }
}

case class RequestDetailedValuationWithoutForm(
                                                assessmentRef: Long,
                                                address: String,
                                                effectiveDate: String,
                                                rateableValue: Option[Long]
                                              )

case class AvailableRequestDetailedValuation(
                                            check: String,
                                            valuation: String,
                                            valuationId: Long,
                                            baRef: String,
                                            address: String
                                            )
