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

package controllers.detailedvaluationrequest

import java.time.format.DateTimeFormatter

import actions.AuthenticatedAction
import config.ApplicationConfig
import connectors._
import connectors.authorisation.BusinessRatesAuthorisationConnector
import connectors.propertyLinking.PropertyLinkConnector
import controllers.PropertyLinkingController
import javax.inject.Inject
import models.ApiAssessments
import models.dvr.DetailedValuationRequest
import play.api.Logger
import play.api.http.HttpEntity
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, _}
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import scala.concurrent.{ExecutionContext, Future}

class DvrController @Inject()(
      val errorHandler: CustomErrorHandler,
      propertyLinks: PropertyLinkConnector,
      authenticated: AuthenticatedAction,
      submissionIds: SubmissionIdConnector,
      dvrCaseManagement: DVRCaseManagementConnector,
      businessRatesAuthorisation: BusinessRatesAuthorisationConnector
)(
      implicit executionContext: ExecutionContext,
      override val messagesApi: MessagesApi,
      override val controllerComponents: MessagesControllerComponents,
      val config: ApplicationConfig)
    extends PropertyLinkingController {

  private val logger = Logger(this.getClass.getName)

  def myOrganisationRequestDetailValuationCheck(
        propertyLinkSubmissionId: String,
        valuationId: Long,
        uarn: Long): Action[AnyContent] =
    detailedValuationRequestCheck(propertyLinkSubmissionId, valuationId, uarn, true)

  def myClientsRequestDetailValuationCheck(
        propertyLinkSubmissionId: String,
        valuationId: Long,
        uarn: Long): Action[AnyContent] =
    detailedValuationRequestCheck(propertyLinkSubmissionId, valuationId, uarn, false)

  private def detailedValuationRequestCheck(
        propertyLinkSubmissionId: String,
        valuationId: Long,
        uarn: Long,
        owner: Boolean): Action[AnyContent] = authenticated.async { implicit request =>
    val pLink =
      if (owner) propertyLinks.getOwnerAssessments(propertyLinkSubmissionId)
      else propertyLinks.getClientAssessments(propertyLinkSubmissionId)

    pLink.flatMap {
      case Some(link: ApiAssessments) =>
        for {
          optDocuments <- dvrCaseManagement.getDvrDocuments(link.uarn, valuationId, link.submissionId)
          backUrl      <- calculateBackLink(propertyLinkSubmissionId, owner)
          startCheckUrl = config.businessRatesCheckUrl(
            s"property-link/${link.authorisationId}/assessment/$valuationId?propertyLinkSubmissionId=$propertyLinkSubmissionId&uarn=$uarn&dvrCheck=true")
          checkCases <- if (owner)
                         propertyLinks.getMyOrganisationsCheckCases(link.submissionId)
                       else propertyLinks.getMyClientsCheckCases(link.submissionId)
        } yield
          optDocuments match {
            case Some(documents) =>
              Ok(
                views.html.dvr.dvr_files(
                  model = AvailableRequestDetailedValuation(
                    documents.checkForm.documentSummary.documentId,
                    documents.detailedValuation.documentSummary.documentId,
                    valuationId,
                    link.assessments.head.billingAuthorityReference,
                    link.address,
                    uarn
                  ),
                  submissionId = propertyLinkSubmissionId,
                  owner = owner,
                  authorisationId = link.authorisationId,
                  backUrl = backUrl,
                  startCheckUrl = startCheckUrl,
                  checkCases = checkCases
                ))
            case None =>
              Redirect(
                if (owner)
                  controllers.detailedvaluationrequest.routes.DvrController
                    .myOrganisationAlreadyRequestedDetailValuation(
                      propertyLinkSubmissionId = propertyLinkSubmissionId,
                      valuationId = valuationId)
                else
                  controllers.detailedvaluationrequest.routes.DvrController.myClientsAlreadyRequestedDetailValuation(
                    propertyLinkSubmissionId = propertyLinkSubmissionId,
                    valuationId = valuationId)
              )
          }
      case None =>
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
      submissionId <- submissionIds.get("DVR")
      pLink <- if (owner) propertyLinks.getOwnerAssessments(propertyLinkSubmissionId)
              else propertyLinks.getClientAssessments(propertyLinkSubmissionId)
      agents = pLink.map(opt => opt.agents.map(_.organisationId).toList).getOrElse(List.empty[Long])
      dvr = pLink.map { propertyLink =>
        DetailedValuationRequest(
          propertyLink.authorisationId,
          request.organisationId,
          request.personId,
          submissionId,
          valuationId,
          agents,
          propertyLink.assessments.head.billingAuthorityReference
        )
      }
      result <- dvr
                 .map(dvrCaseManagement.requestDetailedValuationV2)
                 .getOrElse(Future.failed(throw new NotFoundException("property link does not exist")))
    } yield {
      pLink match {
        case Some(p) =>
          Redirect(
            if (owner)
              routes.DvrController
                .myOrganisationRequestDetailValuationConfirmation(propertyLinkSubmissionId, submissionId)
            else
              routes.DvrController.myClientsRequestDetailValuationConfirmation(propertyLinkSubmissionId, submissionId)
          )
        case None =>
          notFound
      }
    }

  }

  def myOrganisationRequestDetailValuationConfirmation(
        propertyLinkSubmissionId: String,
        submissionId: String): Action[AnyContent] =
    confirmation(propertyLinkSubmissionId, submissionId, true)

  def myClientsRequestDetailValuationConfirmation(
        propertyLinkSubmissionId: String,
        submissionId: String): Action[AnyContent] =
    confirmation(propertyLinkSubmissionId, submissionId, false)

  private def confirmation(
        propertyLinkSubmissionId: String,
        submissionId: String,
        owner: Boolean
  ): Action[AnyContent] = authenticated.async { implicit request =>
    val pLink =
      if (owner) propertyLinks.getOwnerAssessments(propertyLinkSubmissionId)
      else propertyLinks.getClientAssessments(propertyLinkSubmissionId)
    pLink.map {
      case Some(link) =>
        Ok(views.html.dvr.requested_detailed_valuation(submissionId, link.address))
      case None =>
        BadRequest(views.html.errors.propertyMissing())
    }
  }

  def myOrganisationAlreadyRequestedDetailValuation(
        propertyLinkSubmissionId: String,
        valuationId: Long): Action[AnyContent] =
    alreadySubmittedDetailedValuationRequest(propertyLinkSubmissionId, valuationId, true)

  def myClientsAlreadyRequestedDetailValuation(
        propertyLinkSubmissionId: String,
        valuationId: Long): Action[AnyContent] =
    alreadySubmittedDetailedValuationRequest(propertyLinkSubmissionId, valuationId, false)

  private def alreadySubmittedDetailedValuationRequest(
        submissionId: String,
        valuationId: Long,
        owner: Boolean
  ): Action[AnyContent] = authenticated.async { implicit request =>
    val pLink =
      if (owner) propertyLinks.getOwnerAssessments(submissionId) else propertyLinks.getClientAssessments(submissionId)
    pLink.flatMap {
      case Some(link) =>
        val assessment = link.assessments
          .find(a => a.assessmentRef == valuationId)
          .getOrElse(throw new IllegalStateException(s"Assessment with ref: $valuationId does not exist"))
        val effectiveDate = assessment.effectiveDate.getOrElse(
          throw new IllegalStateException(s"Assessment with ref: $valuationId does not contain an Effective Date"))
        val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy")

        for {
          exists  <- dvrCaseManagement.dvrExists(request.organisationAccount.id, valuationId)
          backUrl <- calculateBackLink(submissionId, owner)
        } yield {
          if (exists) {
            Ok(views.html.dvr.already_requested_detailed_valuation(backUrl))
          } else {
            Ok(
              views.html.dvr.request_detailed_valuation(
                submissionId,
                RequestDetailedValuationWithoutForm(
                  valuationId,
                  link.address,
                  formatter.format(effectiveDate),
                  assessment.rateableValue,
                  link.uarn),
                owner,
                backUrl
              ))
          }
        }
      case None =>
        Future.successful(BadRequest(views.html.errors.propertyMissing()))
    }

  }

  def myOrganisationRequestDetailedValuationRequestFile(
        propertyLinkSubmissionId: String,
        valuationId: Long,
        fileRef: String): Action[AnyContent] =
    requestDvrFile(propertyLinkSubmissionId, valuationId, fileRef, true)

  def myClientsRequestDetailedValuationRequestFile(
        propertyLinkSubmissionId: String,
        valuationId: Long,
        fileRef: String): Action[AnyContent] =
    requestDvrFile(propertyLinkSubmissionId, valuationId, fileRef, false)

  private def requestDvrFile(
        submissionId: String,
        valuationId: Long,
        fileRef: String,
        owner: Boolean
  ): Action[AnyContent] = authenticated.async { implicit request =>
    val pLink =
      if (owner) propertyLinks.getOwnerAssessments(submissionId) else propertyLinks.getClientAssessments(submissionId)
    pLink.flatMap {
      case Some(link: ApiAssessments) =>
        dvrCaseManagement
          .getDvrDocument(link.uarn, valuationId, link.submissionId, fileRef)
          .map { response =>
            Ok.sendEntity(
                HttpEntity.Streamed(
                  data = response.bodyAsSource,
                  contentLength = response.header(CONTENT_LENGTH).map(_.toLong),
                  contentType = Some(response.contentType)))
              .withHeaders(CONTENT_DISPOSITION -> s"""attachment;filename="${link.submissionId}.pdf"""")
          }
      case None =>
        Future.successful(BadRequest(views.html.errors.propertyMissing()))
    }
  }

  private def calculateBackLink(submissionId: String, isOwner: Boolean)(implicit hc: HeaderCarrier): Future[String] = {
    val linkFOpt =
      if (isOwner) propertyLinks.getOwnerAssessments(submissionId)
      else propertyLinks.getClientAssessments(submissionId)

    linkFOpt.map {
      case Some(link) if link.assessments.size == 1 =>
        config.newDashboardUrl(if (!isOwner) "client-properties" else "your-properties")
      case _ =>
        uk.gov.hmrc.propertylinking.controllers.valuations.routes.ValuationsController
          .valuations(submissionId, isOwner)
          .url
    }
  }

  def canChallenge(
        plSubmissionId: String,
        assessmnetRef: Long,
        caseRef: String,
        authorisationId: Long,
        uarn: Long,
        isOwner: Boolean): Action[AnyContent] = authenticated.async { implicit request =>
    propertyLinks.canChallenge(plSubmissionId, assessmnetRef, caseRef, isOwner) flatMap {
      case None =>
        Future.successful(Redirect(config.businessRatesValuationFrontendUrl(
          s"/property-link/$plSubmissionId/assessment/$assessmnetRef/startChallenge?propertyLinkId=$authorisationId&isOwner=$isOwner")))
      case Some(response) => {
        response.result match {
          case true => {
            val party = if (isOwner) "client" else "agent"
            Future.successful(
              Redirect(config.businessRatesChallengeStartPageUrl(
                s"property-link/$plSubmissionId/valuation/$assessmnetRef/check/$caseRef/party/$party/start")))
          }
          case false =>
            Future successful Ok(
              views.html.dvr.cannotRaiseChallenge(
                model = response,
                homePageUrl = config.newDashboardUrl("home"),
                authorisationId = authorisationId,
                backLinkUrl =
                  if (isOwner)
                    routes.DvrController
                      .myOrganisationRequestDetailValuationCheck(plSubmissionId, assessmnetRef, uarn)
                      .url
                  else
                    routes.DvrController
                      .myClientsRequestDetailValuationCheck(plSubmissionId, assessmnetRef, uarn)
                      .url
              ))
        }
      }
    }
  }

}

case class RequestDetailedValuationWithoutForm(
      assessmentRef: Long,
      address: String,
      effectiveDate: String,
      rateableValue: Option[Long],
      uarn: Long
)

case class AvailableRequestDetailedValuation(
      check: String,
      valuation: String,
      valuationId: Long,
      baRef: String,
      address: String,
      uarn: Long
)
