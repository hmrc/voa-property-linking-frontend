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
import models.Assessment
import play.api.mvc.Action

trait Assessments extends PropertyLinkingController {
  val propertyLinks = Wiring().propertyLinkConnector
  val reprConnector = Wiring().propertyRepresentationConnector
  val individuals = Wiring().individualAccountConnector
  val groups = Wiring().groupAccountConnector
  val auth = Wiring().authConnector
  val authenticated = Wiring().authenticated

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
    Redirect(ApplicationConfig.vmvUrl + s"/detail/2017/$uarn")
  }

  def viewDetailedAssessment(authorisationId: Long, assessmentRef: Long) = authenticated { implicit request =>
    Redirect(ApplicationConfig.businessRatesValuationUrl(s"property-link/$authorisationId/assessment/$assessmentRef"))
  }

  def requestDetailedValuation(authId: Long, assessmentRef: Long) = authenticated.toViewAssessment(authId, assessmentRef) { implicit request =>
    Ok("requested")
  }
}

object Assessments extends Assessments

case class AssessmentsVM(assessments: Seq[Assessment], backLink: Option[String], linkPending: Boolean)
