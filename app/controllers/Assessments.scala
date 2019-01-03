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

import actions.AuthenticatedAction
import config.ApplicationConfig
import connectors._
import connectors.propertyLinking.PropertyLinkConnector
import form.EnumMapping
import javax.inject.Inject

import models._
import models.dvr.{DetailedValuationRequest, DetailedValuationRequestTypes, EmailRequest, PostRequest}
import play.api.data.Forms.text
import play.api.data.{Form, Forms}
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc.Action
import views.html.dashboard.cannotRaiseChallenge

import scala.concurrent.Future

class Assessments @Inject()(propertyLinks: PropertyLinkConnector, authenticated: AuthenticatedAction,
                            submissionIds: SubmissionIdConnector, dvrCaseManagement: DVRCaseManagementConnector,
                            businessRatesValuations: BusinessRatesValuationConnector,
                            checkCaseConnector: CheckCaseConnector,
                            businessRatesAuthorisation: BusinessRatesAuthorisation)
                           (implicit val messagesApi: MessagesApi, val config: ApplicationConfig) extends PropertyLinkingController {

  def assessments(authorisationId: Long) = authenticated.toViewAssessmentsFor(authorisationId) { implicit request =>
    val backLink = request.headers.get("Referer")

    if(config.checkCasesEnabled) {
      propertyLinks.getLink(authorisationId) flatMap {
        case Some(PropertyLink(_, _, _, _, _, _, _, _, Seq(), _)) => notFound
        case Some(link) if link.pending == false => {
          for {
            isAgentOwnProperty <- businessRatesAuthorisation.isAgentOwnProperty(authorisationId)
            checkCases <- checkCaseConnector.getCheckCases(Some(link), isAgentOwnProperty)
          } yield {
            Ok(views.html.dashboard.assessmentsCheckCases(AssessmentsVM(viewAssessmentForm, link.assessments, backLink, link.pending, checkCases, isAgentOwnProperty, Some(getPaperChallengeUrl(link.assessments)), link.submissionId)))
          } }
        case Some(link) => {
            for {
              isAgentOwnProperty <- businessRatesAuthorisation.isAgentOwnProperty(authorisationId)
            } yield {
              Ok(views.html.dashboard.assessmentsCheckCases(AssessmentsVM(viewAssessmentForm, link.assessments, backLink, link.pending, None, isAgentOwnProperty, None, link.submissionId, isPropertyLinkPending = link.pending)))
            }
        }
      }
    } else {
      propertyLinks.getLink(authorisationId) map {
        case Some(PropertyLink(_, _, _, _, _, _, _, _, Seq(), _)) => notFound
        case Some(link) => Ok(views.html.dashboard.assessments(AssessmentsVM(viewAssessmentForm, link.assessments, backLink, link.pending, plSubmissionId = link.submissionId)))
        case None => notFound
      }
    }

  }

  def viewSummary(uarn: Long) = Action { implicit request =>
    Redirect(config.vmvUrl + s"/detail/$uarn")
  }

  def viewDetailedAssessment(authorisationId: Long, assessmentRef: Long, baRef: String) = authenticated { implicit request =>
    businessRatesValuations.isViewable(authorisationId, assessmentRef) map {
      case true => Redirect(config.businessRatesValuationUrl(s"property-link/$authorisationId/assessment/$assessmentRef"))
      case false =>
        if (config.dvrEnabled){
          Redirect(routes.DvrController.detailedValuationRequestCheck(authorisationId, assessmentRef, baRef))
        } else {
          Redirect(routes.Assessments.requestDetailedValuation(authorisationId, assessmentRef, baRef))
        }
    }
  }

  lazy val viewAssessmentForm = Form(Forms.single("viewAssessmentRadio" -> text.transform[(String, Long, String)](x => x.split("-").toList match {
    case uarn :: assessmentRef :: baRef :: Nil => (uarn, assessmentRef.toLong, baRef)
  }, y => s"${y._1}-${y._2}-${y._3}")))

  def submitViewAssessment(authorisationId: Long) = authenticated { implicit request =>
    val backLink = request.headers.get("Referer")
    viewAssessmentForm.bindFromRequest().fold(
      errors => {
        if(config.checkCasesEnabled) {
          propertyLinks.getLink(authorisationId) flatMap {
            case Some(PropertyLink(_, _, _, _, _, _, _, _, Seq(), _)) => notFound
            case Some(link) if link.pending == false => {
              for {
                isAgentOwnProperty <- businessRatesAuthorisation.isAgentOwnProperty(authorisationId)
                checkCases <- checkCaseConnector.getCheckCases(Some(link), isAgentOwnProperty)
              } yield {
                BadRequest(views.html.dashboard.assessmentsCheckCases(AssessmentsVM(viewAssessmentForm, link.assessments, backLink, link.pending, checkCases, isAgentOwnProperty, plSubmissionId = link.submissionId)))
              } }
            case Some(link) => BadRequest(views.html.dashboard.assessmentsCheckCases(AssessmentsVM(viewAssessmentForm, link.assessments, backLink, link.pending, None, false, plSubmissionId = link.submissionId)))
          }
        }else {
          propertyLinks.getLink(authorisationId) map {
            case Some(PropertyLink(_, _, _, _, _, _, _, _, Seq(), _)) => notFound
            case Some(link) => Ok(views.html.dashboard.assessments(AssessmentsVM(viewAssessmentForm, link.assessments, backLink, link.pending, plSubmissionId = link.submissionId)))
            case None => notFound
          }
        }
      }
      ,
      {
        case (uarn, assessmentRef, baRef) =>
          uarn match {
            case "" => Future.successful(Redirect(routes.Assessments.viewDetailedAssessment(authorisationId, assessmentRef, baRef)))
            case _ => Future.successful(Redirect(routes.Assessments.viewSummary(uarn.toLong)))
          }
      }
    )
  }

  def requestDetailedValuation(authId: Long, assessmentRef: Long, baRef: String) = authenticated.toViewAssessment(authId, assessmentRef) { implicit request =>
    Ok(views.html.dvr.requestDetailedValuation(RequestDetailedValuationVM(dvRequestForm, authId, assessmentRef, baRef)))
  }

  def duplicateRequestDetailedValuation(authId: Long, assessmentRef: Long) = authenticated.toViewAssessment(authId, assessmentRef) { implicit request =>
    Ok(views.html.dvr.duplicateRequestDetailedValuation())
  }

  def startChallengeFromDVR(authorisationId: Long, assessmentRef: Long, baRef: String) = authenticated { implicit request =>
    Ok(views.html.dvr.challenge_valuation(authorisationId, assessmentRef, baRef))
  }

  def detailedValuationRequested(authId: Long, assessmentRef: Long, baRef: String) = authenticated.toViewAssessment(authId, assessmentRef) { implicit request =>
    dvRequestForm.bindFromRequest().fold(
      errs => BadRequest(views.html.dvr.requestDetailedValuation(RequestDetailedValuationVM(errs, authId, assessmentRef, baRef))),
      preference => {
        val prefix = preference match {
          case EmailRequest => "EMAIL"
          case PostRequest => "POST"
        }

        for {
          submissionId <- submissionIds.get(prefix)
          dvr = DetailedValuationRequest(authId, request.organisationId, request.personId, submissionId, assessmentRef, baRef)
          dvrExists <- dvrCaseManagement.dvrExists(request.organisationId, assessmentRef)
          _ <- if (!dvrExists) dvrCaseManagement.requestDetailedValuation(dvr)
        } yield {
          if (dvrExists) {
            Redirect(routes.Assessments.duplicateRequestDetailedValuation(authId, assessmentRef))
          } else {
            Redirect(routes.Assessments.dvRequestConfirmation(submissionId, authId))
          }
        }
      }
    )
  }

  def dvRequestConfirmation(submissionId: String, authorisationId: Long) = Action.async { implicit request =>
    val preference = if (submissionId.startsWith("EMAIL")) "email" else "post"
    propertyLinks.getLink(authorisationId).map {
      case Some(link) => Ok(views.html.dvr.detailedValuationRequested(submissionId, preference, link.address))
      case None => notFound
    }
  }

  def canChallenge(plSubmissionId: String, assessmnetRef: Long, caseRef: String, isAgent: Boolean, authorisationId: Long)  = authenticated { implicit request =>
    propertyLinks.canChallenge(plSubmissionId, assessmnetRef, caseRef, isAgent).flatMap{ responseOpt =>
      responseOpt match {
        case None => Redirect(config.businessRatesValuationUrl(s"property-link/$authorisationId/assessment/$assessmnetRef/startChallenge"))
        case Some(response) => {
          response.result match {
            case true  => {
              businessRatesAuthorisation.isAgentOwnProperty(authorisationId).map{ isAgentProperty =>
                val party = if(isAgentProperty) "client" else "agent"
                Redirect(config.businessRatesChallengeStartPageUrl(s"property-link/$authorisationId/valuation/$assessmnetRef/check/$caseRef/party/$party/start"))
              }

            }
            case false => Ok(cannotRaiseChallenge(response, config.newDashboardUrl("home"), authorisationId))
          }
        }
      }
    }
  }

  private def getPaperChallengeUrl(assessmentSeq: Seq[Assessment]): String = {
    val a = assessmentSeq.sortWith(_.effectiveDate.toEpochDay < _.effectiveDate.toEpochDay).head
    config.businessRatesValuationUrl(s"property-link/${a.authorisationId}/assessment/${a.assessmentRef}/startChallenge")
  }

  lazy val dvRequestForm = Form(Forms.single("requestType" -> EnumMapping(DetailedValuationRequestTypes)))
}

case class AssessmentsVM(form: Form[_], assessments: Seq[Assessment], backLink: Option[String], linkPending: Boolean, checkCases: Option[CheckCasesResponse] = None, isAgentOwnProperty: Boolean = false, paperChallengeUrl: Option[String] = None, plSubmissionId: String, isPropertyLinkPending: Boolean = false)

case class RequestDetailedValuationVM(form: Form[_], authId: Long, assessmentRef: Long, baRef: String)