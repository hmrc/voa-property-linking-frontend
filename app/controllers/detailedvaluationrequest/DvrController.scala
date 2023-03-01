/*
 * Copyright 2023 HM Revenue & Customs
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
import cats.data.OptionT
import config.ApplicationConfig
import connectors.challenge.ChallengeConnector
import connectors.propertyLinking.PropertyLinkConnector
import connectors.vmv.VmvConnector
import connectors.{DVRCaseManagementConnector, _}
import controllers.PropertyLinkingController
import models.ListType.ListType
import models.dvr.cases.check.{CheckType, StartCheckForm}
import models.dvr.DetailedValuationRequest
import models.dvr.cases.check.projection.CaseDetails
import models.properties.PropertyHistory
import models.{ApiAssessment, ApiAssessments, ListType, Party}
import play.api.data.Form
import play.api.data.Forms._
import play.api.http.HttpEntity
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{Action, _}
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler
import uk.gov.hmrc.uritemplate.syntax.UriTemplateSyntax
import utils.{Cats, Formatters}

import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Named}
import models.dvr.cases.check.CheckType.{Internal, RateableValueTooHigh}
import models.dvr.cases.check.common.{Agent, AgentCount}

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
      propertyMissingView: views.html.errors.propertyMissing,
      @Named("check.summary.url") checkSummaryUrlTemplate: String,
      @Named("vmv.send-enquiry.url") enquiryUrlTemplate: String,
      @Named("vmv.estimator-dvr-valuation.url") estimatorUrlTemplate: String
)(
      implicit val executionContext: ExecutionContext,
      override val messagesApi: MessagesApi,
      override val controllerComponents: MessagesControllerComponents,
      val config: ApplicationConfig)
    extends PropertyLinkingController with Cats with UriTemplateSyntax {

  val startCheckForm: Form[StartCheckForm] = Form(
    mapping(
      "checkType" -> optional(text)
        .verifying(
          "available.requestvaluation.startCheckTab.checkType.error.missing",
          _.exists(value => CheckType.all.map(_.value).contains(value)))
        .transform[CheckType](_.map(CheckType.of).get, ct => Some(ct.value)),
      "propertyLinkSubmissionId" -> optional(text),
      "authorisationId"          -> optional(text),
      "uarn"                     -> optional(longNumber),
      "dvrCheck"                 -> optional(boolean),
      "isOwner"                  -> boolean
    )(StartCheckForm.apply)(StartCheckForm.unapply)
  )

  def myOrganisationRequestDetailValuationCheck(
        propertyLinkSubmissionId: String,
        valuationId: Long,
        uarn: Long,
        challengeCaseRef: Option[String] = None,
        otherValuationId: Option[Long] = None,
        fromFuture: Option[Boolean] = None,
        tabName: Option[String] = None): Action[AnyContent] =
    detailedValuationRequestCheck(
      propertyLinkSubmissionId,
      valuationId,
      uarn,
      owner = true,
      challengeCaseRef = challengeCaseRef,
      otherValuationId = otherValuationId,
      fromFuture = fromFuture,
      tabName = tabName
    )

  def myClientsRequestDetailValuationCheck(
        propertyLinkSubmissionId: String,
        valuationId: Long,
        uarn: Long,
        challengeCaseRef: Option[String] = None,
        otherValuationId: Option[Long] = None,
        fromFuture: Option[Boolean] = None,
        tabName: Option[String] = None): Action[AnyContent] =
    detailedValuationRequestCheck(
      propertyLinkSubmissionId,
      valuationId,
      uarn,
      owner = false,
      challengeCaseRef = challengeCaseRef,
      otherValuationId = otherValuationId,
      fromFuture = fromFuture,
      tabName = tabName
    )

  private def detailedValuationRequestCheck(
        propertyLinkSubmissionId: String,
        valuationId: Long,
        uarn: Long,
        owner: Boolean,
        formWithErrors: Option[Form[StartCheckForm]] = None,
        challengeCaseRef: Option[String] = None,
        otherValuationId: Option[Long] = None,
        fromFuture: Option[Boolean] = None,
        tabName: Option[String] = None): Action[AnyContent] = authenticated.async { implicit request =>
    val pLink =
      if (owner) propertyLinks.getOwnerAssessments(propertyLinkSubmissionId)
      else propertyLinks.getClientAssessments(propertyLinkSubmissionId)

    pLink.flatMap {
      case Some(link) =>
        dvrCaseManagement.getDvrDocuments(link.uarn, valuationId, link.submissionId).flatMap {
          case Some(documents) =>
            val assessment: ApiAssessment = link.assessments
              .find(a => a.assessmentRef == valuationId)
              .getOrElse(throw new IllegalStateException(s"Assessment with ref: $valuationId does not exist"))

            val backUrl = challengeCaseRef.fold {
              fromFuture
                .flatMap { fromFuture =>
                  if (fromFuture)
                    DvrController.getFutureValuationUrl(link, owner).map(_ + s"#${tabName.getOrElse("valuation-tab")}")
                  else None
                }
                .getOrElse(uk.gov.hmrc.propertylinking.controllers.valuations.routes.ValuationsController
                  .valuations(propertyLinkSubmissionId, owner)
                  .url)
            } { ref =>
              config.businessRatesChallengeUrl(
                s"summary/property-link/${link.authorisationId}/submission-id/$propertyLinkSubmissionId/challenge-cases/$ref?isAgent=${!owner}&isDvr=true&valuationId=${otherValuationId
                  .getOrElse(valuationId)}")
            }

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
              val agentsData: Option[Seq[AgentCount]] = optCases match {
                case Some(cases)   => Some(collateAgentTabData(cases._1 ++ cases._2, link.agents))
                case None if owner => Some(Seq.empty)
                case _             => None
              }
              val form = formWithErrors.getOrElse(startCheckForm)
              val evaluateRoute = { t: String =>
                if (owner) {
                  controllers.detailedvaluationrequest.routes.DvrController
                    .myOrganisationRequestDetailedValuationRequestFile(propertyLinkSubmissionId, valuationId, t)
                    .url
                } else {
                  controllers.detailedvaluationrequest.routes.DvrController
                    .myClientsRequestDetailedValuationRequestFile(propertyLinkSubmissionId, valuationId, t)
                    .url
                }
              }
              val view = dvrFilesView(
                model = AvailableRequestDetailedValuation(
                  activeTabId = formWithErrors.map(_ => "start-check-tab"),
                  check = documents.checkForm.documentSummary.documentId,
                  valuation = documents.detailedValuation.documentSummary.documentId,
                  valuationId = valuationId,
                  baRef = link.assessments.head.billingAuthorityReference,
                  address = link.address,
                  uarn = uarn,
                  isDraftList = assessment.isDraft,
                  isWelshProperty = assessment.isWelsh,
                  submissionId = propertyLinkSubmissionId,
                  owner = owner,
                  authorisationId = link.authorisationId,
                  clientOrgName = link.clientOrgName.getOrElse(""),
                  backUrl = backUrl,
                  checksAndChallenges = optCases,
                  rateableValueFormatted =
                    assessment.rateableValue.map(rv => Formatters.formatCurrencyRoundedToPounds(rv)),
                  listYear = assessment.listYear,
                  agentTabData = agentsData,
                  assessment = assessment,
                  evaluateRoute = evaluateRoute,
                  checkCasesDetailsTab = CheckCasesDetailsTab(
                    assessmentRef = assessment.assessmentRef,
                    authorisationId = link.authorisationId,
                    checkCases = optCases.fold(List.empty[CaseDetails])(_._1),
                    checkSummaryUrl = { checkReference: String =>
                      checkSummaryUrlTemplate.templated(
                        "checkRef"                 -> checkReference,
                        "propertyLinkSubmissionId" -> propertyLinkSubmissionId,
                        "isOwner"                  -> owner,
                        "valuationId"              -> valuationId,
                        "isDvr"                    -> true
                      )
                    },
                    downloadUrl = evaluateRoute(documents.checkForm.documentSummary.documentId),
                    isOwner = owner,
                    listYear = assessment.listYear,
                    propertyLinkSubmissionId = propertyLinkSubmissionId,
                    startCheckUrl = "#start-check-tab",
                    uarn = uarn
                  )
                ),
                startCheckForm = form,
                compiledListEnabled = config.compiledListEnabled,
                currentValuationUrl = link.assessments
                  .find(a =>
                    a.listType == ListType.CURRENT &&
                      a.currentFromDate.nonEmpty &&
                      a.currentToDate.isEmpty)
                  .map(current =>
                    if (owner) {
                      routes.DvrController
                        .myOrganisationRequestDetailValuationCheck(
                          propertyLinkSubmissionId = link.submissionId,
                          valuationId = current.assessmentRef,
                          uarn = current.uarn,
                          challengeCaseRef = None,
                          fromFuture = Some(true),
                          tabName = Some("valuation-tab")
                        )
                        .url
                    } else {
                      routes.DvrController
                        .myClientsRequestDetailValuationCheck(
                          propertyLinkSubmissionId = link.submissionId,
                          valuationId = current.assessmentRef,
                          uarn = current.uarn,
                          challengeCaseRef = None,
                          fromFuture = Some(true),
                          tabName = Some("valuation-tab")
                        )
                        .url
                  }),
                valuationsUrl = uk.gov.hmrc.propertylinking.controllers.valuations.routes.ValuationsController
                  .valuations(
                    link.submissionId,
                    owner
                  )
                  .url
              )
              if (formWithErrors.exists(_.hasErrors)) BadRequest(view) else Ok(view)
            }

          case None =>
            Future.successful(
              Redirect(
                if (owner)
                  controllers.detailedvaluationrequest.routes.DvrController
                    .myOrganisationAlreadyRequestedDetailValuation(
                      propertyLinkSubmissionId = propertyLinkSubmissionId,
                      valuationId = valuationId,
                      fromFuture = fromFuture,
                      tabName = tabName)
                else
                  controllers.detailedvaluationrequest.routes.DvrController.myClientsAlreadyRequestedDetailValuation(
                    propertyLinkSubmissionId = propertyLinkSubmissionId,
                    valuationId = valuationId,
                    fromFuture = fromFuture,
                    tabName = tabName)
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
                .myOrganisationRequestDetailValuationConfirmation(propertyLinkSubmissionId, submissionId, valuationId)
            else
              routes.DvrController
                .myClientsRequestDetailValuationConfirmation(propertyLinkSubmissionId, submissionId, valuationId)
          )
        case None =>
          notFound
      }
    }

  }

  def myOrganisationRequestDetailValuationConfirmation(
        propertyLinkSubmissionId: String,
        submissionId: String,
        valuationId: Long): Action[AnyContent] =
    confirmation(propertyLinkSubmissionId, submissionId, valuationId, owner = true)

  def myClientsRequestDetailValuationConfirmation(
        propertyLinkSubmissionId: String,
        submissionId: String,
        valuationId: Long): Action[AnyContent] =
    confirmation(propertyLinkSubmissionId, submissionId, valuationId, owner = false)

  private[detailedvaluationrequest] def confirmation(
        propertyLinkSubmissionId: String,
        submissionId: String,
        valuationId: Long,
        owner: Boolean
  ): Action[AnyContent] = authenticated.async { implicit request =>
    {
      for {
        apiAssessments <- OptionT {
                           if (owner) propertyLinks.getOwnerAssessments(propertyLinkSubmissionId)
                           else propertyLinks.getClientAssessments(propertyLinkSubmissionId)
                         }
        clientPropertyLink <- OptionT.liftF {
                               if (!owner) propertyLinks.clientPropertyLink(propertyLinkSubmissionId)
                               else Future.successful(None)
                             }
        assessment <- OptionT.fromOption[Future](apiAssessments.assessments.find(_.assessmentRef == valuationId))
      } yield {
        Ok(
          requestedDetailedValuationView(
            submissionId = submissionId,
            address = clientPropertyLink.fold(apiAssessments.address)(_.address),
            localAuthorityRef = clientPropertyLink.fold(assessment.billingAuthorityReference)(_.localAuthorityRef),
            clientDetails = clientPropertyLink.map(_.client),
            welshDvr = assessment.isWelsh,
            formattedFromDate = assessment.currentFromDate.fold("")(Formatters.formattedFullDate),
            formattedToDate = assessment.currentToDate.fold {
              assessment.listType match {
                case ListType.CURRENT if assessment.listYear == "2017" =>
                  Formatters.formattedFullDate(config.default2017AssessmentEndDate)
                case _ => implicitly[Messages].apply("assessments.enddate.present.lowercase")
              }
            }(Formatters.formattedFullDate)
          ))
      }
    }.getOrElse(BadRequest(propertyMissingView()))
  }

  def myOrganisationAlreadyRequestedDetailValuation(
        propertyLinkSubmissionId: String,
        valuationId: Long,
        fromFuture: Option[Boolean] = None,
        tabName: Option[String] = None): Action[AnyContent] =
    alreadySubmittedDetailedValuationRequest(propertyLinkSubmissionId, valuationId, owner = true, fromFuture, tabName)

  def myClientsAlreadyRequestedDetailValuation(
        propertyLinkSubmissionId: String,
        valuationId: Long,
        fromFuture: Option[Boolean] = None,
        tabName: Option[String] = None
  ): Action[AnyContent] =
    alreadySubmittedDetailedValuationRequest(propertyLinkSubmissionId, valuationId, owner = false, fromFuture, tabName)

  private[detailedvaluationrequest] def alreadySubmittedDetailedValuationRequest(
        submissionId: String,
        valuationId: Long,
        owner: Boolean,
        fromFuture: Option[Boolean] = None,
        tabName: Option[String] = None
  ): Action[AnyContent] = authenticated.async { implicit request =>
    val pLink =
      if (owner) propertyLinks.getOwnerAssessments(submissionId) else propertyLinks.getClientAssessments(submissionId)
    pLink.flatMap {
      case Some(link) =>
        val assessment = link.assessments
          .find(a => a.assessmentRef == valuationId)
          .getOrElse(throw new IllegalStateException(s"Assessment with ref: $valuationId does not exist"))

        for {
          record <- dvrCaseManagement.getDvrRecord(request.organisationAccount.id, valuationId)
          backUrl = fromFuture
            .flatMap { fromFuture =>
              if (fromFuture)
                DvrController.getFutureValuationUrl(link, owner).map(_ + s"#${tabName.getOrElse("valuation-tab")}")
              else None
            }
            .getOrElse(uk.gov.hmrc.propertylinking.controllers.valuations.routes.ValuationsController
              .valuations(submissionId, owner)
              .url)
        } yield {
          record.fold {
            Ok(
              requestDetailedValuationView(
                submissionId = submissionId,
                model = RequestDetailedValuationWithoutForm(link, assessment, owner),
                owner = owner,
                backUrl = backUrl,
                enquiryUrl = enquiryUrlTemplate.templated(
                  "valuationId"              -> valuationId,
                  "authorisationId"          -> link.authorisationId,
                  "propertyLinkSubmissionId" -> submissionId,
                  "isOwner"                  -> owner,
                  "uarn"                     -> link.uarn
                ),
                estimatorUrl = estimatorUrlTemplate.templated(
                  "authorisationId"          -> link.authorisationId,
                  "valuationId"              -> valuationId,
                  "propertyLinkSubmissionId" -> submissionId,
                  "isOwner"                  -> owner,
                  "uarn"                     -> link.uarn),
                localCouncilRef = assessment.billingAuthorityReference
              ))
          } { record =>
            Ok(
              alreadyRequestedDetailedValuationView(
                addressFormatted = Formatters.capitalisedAddress(link.address),
                backLink = backUrl,
                dvrSubmissionId = record.dvrSubmissionId,
                localCouncilRef = assessment.billingAuthorityReference,
                listType = assessment.listType,
                listYear = assessment.listYear,
                rateableValueFormatted = assessment.rateableValue.map(Formatters.formatCurrencyRoundedToPounds(_)),
                fromDateFormatted = assessment.currentFromDate.fold("")(Formatters.formattedFullDate(_)),
                toDateFormatted = assessment.currentToDate.map(Formatters.formattedFullDate(_)),
              ))
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
        isOwner: Boolean,
        listYear: String): Action[AnyContent] = authenticated.async { implicit request =>
    val eventualPropertyHistory: Future[PropertyHistory] = vmvConnector.getPropertyHistory(uarn)

    eventualPropertyHistory.flatMap { propertyHistory =>
      val propertyAddress: String = propertyHistory.addressFull
      val localAuthorityRef: String = propertyHistory.localAuthorityReference

      propertyLinks.canChallenge(plSubmissionId, assessmentRef, caseRef, isOwner) flatMap {
        case None =>
          Future.successful {
            val returnUrl =
              if (isOwner)
                s"${config.serviceUrl}${controllers.detailedvaluationrequest.routes.DvrController
                  .myOrganisationRequestDetailValuationCheck(plSubmissionId, assessmentRef, uarn, tabName = Some("valuation-tab"))
                  .url}"
              else
                s"${config.serviceUrl}${controllers.detailedvaluationrequest.routes.DvrController
                  .myClientsRequestDetailValuationCheck(plSubmissionId, assessmentRef, uarn, tabName = Some("valuation-tab"))
                  .url}"
            Redirect(
              config.businessRatesValuationFrontendUrl(
                s"property-link/valuations/startChallenge?backLinkUrl=$returnUrl"))
          }
        case Some(response) =>
          if (response.result) {
            val party = if (isOwner) "client" else "agent"
            Future.successful(Redirect(config.businessRatesChallengeUrl(
              s"property-link/$plSubmissionId/valuation/$assessmentRef/check/$caseRef/party/$party/start?isDvr=true&valuationListYear=$listYear")))
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
                      .myOrganisationRequestDetailValuationCheck(
                        plSubmissionId,
                        assessmentRef,
                        uarn,
                        tabName = Some("valuation-tab"))
                      .url
                  else
                    routes.DvrController
                      .myClientsRequestDetailValuationCheck(
                        plSubmissionId,
                        assessmentRef,
                        uarn,
                        tabName = Some("valuation-tab"))
                      .url
              ))
          }
      }
    }
  }

  def myOrganisationStartCheck(propertyLinkSubmissionId: String, valuationId: Long, uarn: Long): Action[AnyContent] =
    startCheck(propertyLinkSubmissionId, valuationId, isOwner = true, uarn)
  def myClientsStartCheck(propertyLinkSubmissionId: String, valuationId: Long, uarn: Long): Action[AnyContent] =
    startCheck(propertyLinkSubmissionId, valuationId, isOwner = false, uarn)

  private def getCheckType(checkType: CheckType): String = checkType match {
    case RateableValueTooHigh => Internal.value
    case c @ _                => c.value
  }

  private def collateAgentTabData(totalCases: Seq[CaseDetails], agents: Seq[Party]): Seq[AgentCount] =
    agents
      .map { agent =>
        val totalCasesForAgent =
          totalCases.filter(c => c.agent.fold(false)(a => a.organisationId == agent.organisationId))
        val totalOpenCasesForAgent = totalCasesForAgent.count(_.isOpen)
        AgentCount(Agent(agent), totalCasesForAgent.size, totalOpenCasesForAgent)
      }
      .sortWith(_.agent.organisationName < _.agent.organisationName)

  private def startCheck(
        propertyLinkSubmissionId: String,
        valuationId: Long,
        isOwner: Boolean,
        uarn: Long): Action[AnyContent] =
    authenticated.async { implicit request =>
      def startCheckUrl(form: StartCheckForm): String = {
        val rvth = form.checkType == RateableValueTooHigh
        val pathToFirstScreen =
          "property-link/{propertyLinkId}/assessment/{valuationId}/{checkType}{?propertyLinkSubmissionId,uarn,dvrCheck,rvth}"
            .templated(
              "propertyLinkId"           -> form.authorisationId,
              "valuationId"              -> valuationId,
              "checkType"                -> getCheckType(form.checkType),
              "propertyLinkSubmissionId" -> propertyLinkSubmissionId,
              "uarn"                     -> form.uarn,
              "dvrCheck"                 -> true,
              "rvth"                     -> rvth
            )
        config.businessRatesCheckUrl(pathToFirstScreen)
      }

      startCheckForm
        .bindFromRequest()
        .fold[Future[Result]](
          formWithErrors =>
            detailedValuationRequestCheck(
              propertyLinkSubmissionId = propertyLinkSubmissionId,
              valuationId = valuationId,
              uarn = uarn,
              owner = isOwner,
              formWithErrors = Some(formWithErrors))(request),
          form => Future.successful(Redirect(startCheckUrl(form)))
        )

    }
}

object DvrController {
  def getFutureValuationUrl(assessments: ApiAssessments, isOwner: Boolean): Option[String] =
    assessments.assessments
      .find(_.listType == ListType.DRAFT)
      .map(
        future =>
          if (isOwner)
            routes.DvrController
              .myOrganisationRequestDetailValuationCheck(
                propertyLinkSubmissionId = assessments.submissionId,
                valuationId = future.assessmentRef,
                uarn = future.uarn,
                challengeCaseRef = None,
                fromFuture = None,
                tabName = Some("valuation-tab")
              )
              .url
          else
            routes.DvrController
              .myClientsRequestDetailValuationCheck(
                propertyLinkSubmissionId = assessments.submissionId,
                valuationId = future.assessmentRef,
                uarn = future.uarn,
                challengeCaseRef = None,
                fromFuture = None,
                tabName = Some("valuation-tab")
              )
              .url)
}

case class RequestDetailedValuationWithoutForm(
      assessmentRef: Long,
      address: String,
      effectiveDate: String,
      rateableValueFormatted: Option[String],
      uarn: Long,
      listType: ListType,
      listYear: String,
      formattedFromDate: String,
      formattedToDate: Option[String],
      isWelsh: Boolean,
      currentValuationUrl: Option[String],
      valuationsUrl: String
)
object RequestDetailedValuationWithoutForm {

  private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

  def apply(assessments: ApiAssessments, assessment: ApiAssessment, isOwner: Boolean)(
        implicit messages: Messages): RequestDetailedValuationWithoutForm =
    RequestDetailedValuationWithoutForm(
      assessmentRef = assessment.assessmentRef,
      address = assessments.address,
      effectiveDate = formatter.format(
        assessment.effectiveDate.getOrElse(throw new RuntimeException(
          s"Assessment with ref: ${assessment.assessmentRef} does not contain an Effective Date"))),
      rateableValueFormatted = assessment.rateableValue.map(Formatters.formatCurrencyRoundedToPounds(_)),
      uarn = assessments.uarn,
      listType = assessment.listType,
      listYear = assessment.listYear,
      formattedFromDate = assessment.currentFromDate.fold("")(Formatters.formattedFullDate(_)),
      formattedToDate = assessment.currentToDate.map(Formatters.formattedFullDate(_)),
      isWelsh = assessment.isWelsh,
      currentValuationUrl = assessments.assessments
        .find(
          a =>
            a.listType == ListType.CURRENT &&
              a.currentFromDate.nonEmpty &&
              a.currentToDate.isEmpty)
        .map(current =>
          if (isOwner) {
            routes.DvrController
              .myOrganisationRequestDetailValuationCheck(
                propertyLinkSubmissionId = assessments.submissionId,
                valuationId = current.assessmentRef,
                uarn = current.uarn,
                challengeCaseRef = None,
                fromFuture = Some(true),
                tabName = Some("valuation-tab")
              )
              .url
          } else {
            routes.DvrController
              .myClientsRequestDetailValuationCheck(
                propertyLinkSubmissionId = assessments.submissionId,
                valuationId = current.assessmentRef,
                uarn = current.uarn,
                challengeCaseRef = None,
                fromFuture = Some(true),
                tabName = Some("valuation-tab")
              )
              .url
        }),
      valuationsUrl = uk.gov.hmrc.propertylinking.controllers.valuations.routes.ValuationsController
        .valuations(
          assessments.submissionId,
          isOwner
        )
        .url
    )
}

case class AvailableRequestDetailedValuation(
      activeTabId: Option[String],
      address: String,
      authorisationId: Long,
      baRef: String,
      backUrl: String,
      check: String,
      checksAndChallenges: Option[(List[CaseDetails], List[CaseDetails])],
      clientOrgName: String,
      evaluateRoute: String => String,
      isDraftList: Boolean,
      isWelshProperty: Boolean,
      owner: Boolean,
      submissionId: String,
      uarn: Long,
      valuation: String,
      valuationId: Long,
      rateableValueFormatted: Option[String],
      listYear: String,
      agentTabData: Option[Seq[AgentCount]] = None,
      assessment: ApiAssessment,
      checkCasesDetailsTab: CheckCasesDetailsTab
)

case class CheckCasesDetailsTab(
      assessmentRef: Long,
      authorisationId: Long,
      checkCases: List[CaseDetails],
      checkSummaryUrl: String => String,
      downloadUrl: String,
      isOwner: Boolean,
      listYear: String,
      propertyLinkSubmissionId: String,
      startCheckUrl: String,
      uarn: Long
)
