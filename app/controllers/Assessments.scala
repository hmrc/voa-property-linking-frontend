/*
 * Copyright 2017 HM Revenue & Customs
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

import config.{ApplicationConfig, Wiring}
import models.{Assessment, DetailedValuationRequest}
import play.api.mvc.Action

trait Assessments extends PropertyLinkingController {
  val propertyLinks = Wiring().propertyLinkConnector
  val reprConnector = Wiring().propertyRepresentationConnector
  val individuals = Wiring().individualAccountConnector
  val groups = Wiring().groupAccountConnector
  val auth = Wiring().authConnector
  val authenticated = Wiring().authenticated
  val submissionIds = Wiring().submissionIdConnector
  val dvrCaseManagement = Wiring().dvrCaseManagement
  val businessRatesValuations = Wiring().businessRatesValuation

  def assessments(authorisationId: Long, linkPending: Boolean) = authenticated { implicit request =>
    val backLink = request.headers.get("Referer")
    propertyLinks.assessments(authorisationId) map { assessments =>
      Ok(views.html.dashboard.assessments(
        AssessmentsVM(
          assessments,
          backLink,
          linkPending
        )))
    }
  }

  def viewSummary(uarn: Long) = Action { implicit request =>
    Redirect(ApplicationConfig.vmvUrl + s"/cca/detail/$uarn")
  }

  def viewDetailedAssessment(authorisationId: Long, assessmentRef: Long, baRef: String) = authenticated { implicit request =>
    businessRatesValuations.isViewable(authorisationId, assessmentRef) map {
      case true => Redirect(ApplicationConfig.businessRatesValuationUrl(s"property-link/$authorisationId/assessment/$assessmentRef"))
      case false => Redirect(routes.Assessments.requestDetailedValuation(authorisationId, assessmentRef, baRef))
    }
  }

  def requestDetailedValuation(authId: Long, assessmentRef: Long, baRef: String) = authenticated.toViewAssessment(authId, assessmentRef) { implicit request =>
    Ok(views.html.requestDetailedValuation(authId, assessmentRef, baRef))
  }

  def detailedValuationRequested(authId: Long, assessmentRef: Long, baRef: String) = authenticated.toViewAssessment(authId, assessmentRef) { implicit request =>
    for {
      submissionId <- submissionIds.get("RV")
      dvr = DetailedValuationRequest(authId, request.organisationId, request.personId, submissionId, assessmentRef, baRef)
      _ <- dvrCaseManagement.requestDetailedValuation(dvr)
    } yield {
      Ok(views.html.detailedValuationRequested(submissionId))
    }
  }
}

object Assessments extends Assessments

case class AssessmentsVM(assessments: Seq[Assessment], backLink: Option[String], linkPending: Boolean)
