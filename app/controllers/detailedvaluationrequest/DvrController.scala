/*
 * Copyright 2022 HM Revenue & Customs
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

import actions.AuthenticatedAction
import config.ApplicationConfig
import connectors.challenge.ChallengeConnector
import connectors.propertyLinking.PropertyLinkConnector
import connectors.vmv.VmvConnector
import connectors.{DVRCaseManagementConnector, _}
import controllers.PropertyLinkingController
import models.dvr.DetailedValuationRequest
import models.dvr.cases.check.projection.CaseDetails
import models.properties.PropertyHistory
import models.{ApiAssessment, ApiAssessments, PropertyLink}
import play.api.http.HttpEntity
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, _}
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler
import utils.Cats

import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DvrController @Inject()(
      val errorHandler: CustomErrorHandler,
      propertyLinks: PropertyLinkConnector,
      challengeConnector: ChallengeConnector,
      vmvConnector: VmvConnector,
      authenticated: AuthenticatedAction,
      submissionIds: SubmissionIdConnector,
      dvrCaseManagement: DVRCaseManagementConnector,
      alreadyRequestedDetailedValuationView: views.html.dvr.alreadyRequestedDetailedValuation,
      requestDetailedValuationView: views.html.dvr.requestDetailedValuation,
      requestedDetailedValuationView: views.html.dvr.requestedDetailedValuation,
      dvrFilesView: views.html.dvr.dvrFiles,
      cannotRaiseChallengeView: views.html.dvr.cannotRaiseChallenge,
      propertyMissingView: views.html.errors.propertyMissing
)(
      implicit val executionContext: ExecutionContext,
      override val messagesApi: MessagesApi,
      override val controllerComponents: MessagesControllerComponents,
      val config: ApplicationConfig)
    extends PropertyLinkingController with Cats {

  def myOrganisationRequestDetailValuationCheck(
        propertyLinkSubmissionId: String,
        valuationId: Long,
        uarn: Long): Action[AnyContent] =
    detailedValuationRequestCheck(propertyLinkSubmissionId, valuationId, uarn, owner = true)

  def myClientsRequestDetailValuationCheck(
        propertyLinkSubmissionId: String,
        valuationId: Long,
        uarn: Long): Action[AnyContent] =
    detailedValuationRequestCheck(propertyLinkSubmissionId, valuationId, uarn, owner = false)

  private def detailedValuationRequestCheck(
        propertyLinkSubmissionId: String,
        valuationId: Long,
        uarn: Long,
        owner: Boolean): Action[AnyContent] = authenticated.async { implicit request =>
    val pLink =
      if (owner) propertyLinks.getOwnerAssessments(propertyLinkSubmissionId)
      else propertyLinks.getClientAssessments(propertyLinkSubmissionId)

    pLink.flatMap {
      case Some(link) =>
        dvrCaseManagement.getDvrDocuments(link.uarn, valuationId, link.submissionId).flatMap {
          case Some(documents) =>
            val backUrl = uk.gov.hmrc.propertylinking.controllers.valuations.routes.ValuationsController
              .valuations(propertyLinkSubmissionId, owner)
              .url
            val startCheckUrl = config.businessRatesCheckUrl(
              s"property-link/${link.authorisationId}/assessment/$valuationId?propertyLinkSubmissionId=$propertyLinkSubmissionId&uarn=$uarn&dvrCheck=true")

            val assessment: ApiAssessment = link.assessments
              .find(a => a.assessmentRef == valuationId)
              .getOrElse(throw new IllegalStateException(s"Assessment with ref: $valuationId does not exist"))

            val caseDetails: Future[Option[(List[CaseDetails], List[CaseDetails])]] =
              if (assessment.isDraft) {
                Future.successful(None)
              } else {
                def checkCases =
                  if (owner)
                    propertyLinks.getMyOrganisationsCheckCases(link.submissionId)
                  else propertyLinks.getMyClientsCheckCases(link.submissionId)

                def challengeCases =
                  if (owner)
                    challengeConnector.getMyOrganisationsChallengeCases(link.submissionId)
                  else challengeConnector.getMyClientsChallengeCases(link.submissionId)

                (checkCases, challengeCases).tupled.map(Option.apply)
              }

            caseDetails.map { optCases =>
              Ok(dvrFilesView(
                model = AvailableRequestDetailedValuation(
                  check = documents.checkForm.documentSummary.documentId,
                  valuation = documents.detailedValuation.documentSummary.documentId,
                  valuationId = valuationId,
                  baRef = link.assessments.head.billingAuthorityReference,
                  address = link.address,
                  uarn = uarn,
                  isDraftList = assessment.isDraft,
                  isWelshProperty = assessment.isWelsh
                ),
                submissionId = propertyLinkSubmissionId,
                owner = owner,
                authorisationId = link.authorisationId,
                clientOrgName = link.clientOrgName,
                backUrl = backUrl,
                startCheckUrl = startCheckUrl,
                checksAndChallenges = optCases
              ))
            }

          case None =>
            Future.successful(
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
              ))
        }
      case None =>
        Future.successful(BadRequest(propertyMissingView()))
    }
  }

  def myOrganisationRequestDetailValuation(propertyLinkSubmissionId: String, valuationId: Long): Action[AnyContent] =
    requestDetailedValuation(propertyLinkSubmissionId, valuationId, owner = true)

  def myClientsRequestDetailValuation(propertyLinkSubmissionId: String, valuationId: Long): Action[AnyContent] =
    requestDetailedValuation(propertyLinkSubmissionId, valuationId, owner = false)

  private[detailedvaluationrequest] def requestDetailedValuation(
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
      _ <- dvr
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
    confirmation(propertyLinkSubmissionId, submissionId, owner = true)

  def myClientsRequestDetailValuationConfirmation(
        propertyLinkSubmissionId: String,
        submissionId: String): Action[AnyContent] =
    confirmation(propertyLinkSubmissionId, submissionId, owner = false)

  private[detailedvaluationrequest] def confirmation(
        propertyLinkSubmissionId: String,
        submissionId: String,
        owner: Boolean
  ): Action[AnyContent] = authenticated.async { implicit request =>
    val pLink =
      if (owner) propertyLinks.getOwnerAssessments(propertyLinkSubmissionId)
      else propertyLinks.getClientAssessments(propertyLinkSubmissionId)
    pLink.map {
      case Some(link) =>
        Ok(requestedDetailedValuationView(submissionId, link.address))
      case None =>
        BadRequest(propertyMissingView())
    }
  }

  def myOrganisationAlreadyRequestedDetailValuation(
        propertyLinkSubmissionId: String,
        valuationId: Long): Action[AnyContent] =
    alreadySubmittedDetailedValuationRequest(propertyLinkSubmissionId, valuationId, owner = true)

  def myClientsAlreadyRequestedDetailValuation(
        propertyLinkSubmissionId: String,
        valuationId: Long): Action[AnyContent] =
    alreadySubmittedDetailedValuationRequest(propertyLinkSubmissionId, valuationId, owner = false)

  private[detailedvaluationrequest] def alreadySubmittedDetailedValuationRequest(
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

        for {
          exists <- dvrCaseManagement.dvrExists(request.organisationAccount.id, valuationId)
          backUrl = uk.gov.hmrc.propertylinking.controllers.valuations.routes.ValuationsController
            .valuations(submissionId, owner)
            .url
        } yield {
          if (exists) {
            Ok(alreadyRequestedDetailedValuationView(backUrl, isDraftList = assessment.isDraft))
          } else {
            Ok(
              requestDetailedValuationView(
                submissionId = submissionId,
                model = RequestDetailedValuationWithoutForm(link, assessment),
                owner = owner,
                backUrl = backUrl))
          }
        }
      case None =>
        Future.successful(BadRequest(propertyMissingView()))
    }

  }

  def myOrganisationRequestDetailedValuationRequestFile(
        propertyLinkSubmissionId: String,
        valuationId: Long,
        fileRef: String): Action[AnyContent] =
    requestDvrFile(propertyLinkSubmissionId, valuationId, fileRef, owner = true)

  def myClientsRequestDetailedValuationRequestFile(
        propertyLinkSubmissionId: String,
        valuationId: Long,
        fileRef: String): Action[AnyContent] =
    requestDvrFile(propertyLinkSubmissionId, valuationId, fileRef, owner = false)

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
        Future.successful(BadRequest(propertyMissingView()))
    }
  }

  def canChallenge(
        plSubmissionId: String,
        assessmentRef: Long,
        caseRef: String,
        authorisationId: Long,
        uarn: Long,
        isOwner: Boolean): Action[AnyContent] = authenticated.async { implicit request =>
    val eventualPropertyHistory: Future[PropertyHistory] = vmvConnector.getPropertyHistory(uarn)

    eventualPropertyHistory.flatMap { propertyHistory =>
      val propertyAddress: String = propertyHistory.addressFull
      val localAuthorityRef: String = propertyHistory.localAuthorityReference

      propertyLinks.canChallenge(plSubmissionId, assessmentRef, caseRef, isOwner) flatMap {
        case None =>
          Future.successful {
            val returnUrl =
              if (isOwner)
                s"${config.serviceUrl}${controllers.detailedvaluationrequest.routes.DvrController.myOrganisationRequestDetailValuationCheck(plSubmissionId, assessmentRef, uarn).url}"
              else
                s"${config.serviceUrl}${controllers.detailedvaluationrequest.routes.DvrController.myClientsRequestDetailValuationCheck(plSubmissionId, assessmentRef, uarn).url}"
            Redirect(
              config.businessRatesValuationFrontendUrl(
                s"property-link/valuations/startChallenge?backLinkUrl=$returnUrl"))
          }
        case Some(response) =>
          if (response.result) {
            val party = if (isOwner) "client" else "agent"
            Future.successful(Redirect(config.businessRatesChallengeUrl(
              s"property-link/$plSubmissionId/valuation/$assessmentRef/check/$caseRef/party/$party/start?isDvr=true")))
          } else {
            Future successful Ok(
              cannotRaiseChallengeView(
                model = response,
                address = propertyAddress,
                localAuth = localAuthorityRef,
                homePageUrl = config.dashboardUrl("home"),
                authorisationId = authorisationId,
                backLinkUrl =
                  if (isOwner)
                    routes.DvrController
                      .myOrganisationRequestDetailValuationCheck(plSubmissionId, assessmentRef, uarn)
                      .url
                  else
                    routes.DvrController
                      .myClientsRequestDetailValuationCheck(plSubmissionId, assessmentRef, uarn)
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
      uarn: Long,
      isDraftList: Boolean
)
object RequestDetailedValuationWithoutForm {

  private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

  def apply(assessments: ApiAssessments, assessment: ApiAssessment): RequestDetailedValuationWithoutForm =
    RequestDetailedValuationWithoutForm(
      assessmentRef = assessment.assessmentRef,
      address = assessments.address,
      effectiveDate = formatter.format(
        assessment.effectiveDate.getOrElse(throw new RuntimeException(
          s"Assessment with ref: ${assessment.assessmentRef} does not contain an Effective Date"))),
      rateableValue = assessment.rateableValue,
      uarn = assessments.uarn,
      isDraftList = assessment.isDraft
    )

}

case class AvailableRequestDetailedValuation(
      check: String,
      valuation: String,
      valuationId: Long,
      baRef: String,
      address: String,
      uarn: Long,
      isDraftList: Boolean,
      isWelshProperty: Boolean
)
