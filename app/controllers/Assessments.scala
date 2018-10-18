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
import form.EnumMapping
import javax.inject.Inject

import models._
import play.api.data.Forms.text
import play.api.data.{Form, Forms}
import play.api.i18n.MessagesApi
import play.api.mvc.Action

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
                  Ok(views.html.dashboard.assessmentsCheckCases(AssessmentsVM(viewAssessmentForm, link.assessments, backLink, link.pending, checkCases, isAgentOwnProperty)))
                } }
          case Some(link) => Ok(views.html.dashboard.assessmentsCheckCases(AssessmentsVM(viewAssessmentForm, link.assessments, backLink, link.pending, None, false)))
        }
      }else {
        propertyLinks.getLink(authorisationId) map {
          case Some(PropertyLink(_, _, _, _, _, _, _, _, Seq(), _)) => notFound
          case Some(link) => Ok(views.html.dashboard.assessments(AssessmentsVM(viewAssessmentForm, link.assessments, backLink, link.pending)))
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
      case false => Redirect(routes.Assessments.requestDetailedValuation(authorisationId, assessmentRef, baRef))
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
                BadRequest(views.html.dashboard.assessmentsCheckCases(AssessmentsVM(viewAssessmentForm, link.assessments, backLink, link.pending, checkCases, isAgentOwnProperty)))
              } }
            case Some(link) => BadRequest(views.html.dashboard.assessmentsCheckCases(AssessmentsVM(viewAssessmentForm, link.assessments, backLink, link.pending, None, false)))
          }
        }else {
          propertyLinks.getLink(authorisationId) map {
            case Some(PropertyLink(_, _, _, _, _, _, _, _, Seq(), _)) => notFound
            case Some(link) => Ok(views.html.dashboard.assessments(AssessmentsVM(viewAssessmentForm, link.assessments, backLink, link.pending)))
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

  def startChallengeFromDVR = authenticated { implicit request =>
    Ok(views.html.dvr.startChallenge())
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

  lazy val dvRequestForm = Form(Forms.single("requestType" -> EnumMapping(DetailedValuationRequestTypes)))
}

case class AssessmentsVM(form: Form[_], assessments: Seq[Assessment], backLink: Option[String], linkPending: Boolean, checkCases: Option[CheckCasesResponse] = None, isAgentOwnProperty: Boolean = false)

case class RequestDetailedValuationVM(form: Form[_], authId: Long, assessmentRef: Long, baRef: String)