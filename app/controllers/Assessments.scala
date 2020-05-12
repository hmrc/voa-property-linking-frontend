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

package controllers

import actions.AuthenticatedAction
import config.ApplicationConfig
import connectors._
import connectors.authorisation.BusinessRatesAuthorisationConnector
import connectors.propertyLinking.PropertyLinkConnector
import javax.inject.{Inject, Named}
import models._
import play.api.Logger
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler
import uk.gov.hmrc.propertylinking.services.PropertyLinkService

import scala.concurrent.{ExecutionContext, Future}

class Assessments @Inject()(
      val errorHandler: CustomErrorHandler,
      propertyLinks: PropertyLinkConnector,
      propertyLinkService: PropertyLinkService,
      authenticated: AuthenticatedAction,
      businessRatesValuations: BusinessRatesValuationConnector,
      businessRatesAuthorisation: BusinessRatesAuthorisationConnector,
      override val controllerComponents: MessagesControllerComponents,
      @Named("detailed-valuation.external") isExternalValuation: Boolean,
      @Named("detailed-valuation.skip") isSkipAssessment: Boolean
)(
      implicit override val messagesApi: MessagesApi,
      val config: ApplicationConfig,
      executionContext: ExecutionContext
) extends PropertyLinkingController {

  private val logger = Logger(this.getClass.getName)

  def viewOwnerSummary(uarn: Long, isPending: Boolean = false): Action[AnyContent] =
    viewSummary(uarn, isOwner = true, isPending)

  def viewClientSummary(uarn: Long, isPending: Boolean = false): Action[AnyContent] =
    viewSummary(uarn, isOwner = false, isPending)

  private def viewSummary(uarn: Long, isOwner: Boolean, isPending: Boolean = false): Action[AnyContent] = Action {
    implicit request =>
      if (isOwner) {
        Redirect(config.valuationFrontendUrl + s"/property-link/$uarn/valuation/summary")
      } else {
        Redirect(config.valuationFrontendUrl + s"/property-link/clients/all/$uarn/valuation/summary")
      }
  }

  def viewDetailedAssessment(
        submissionId: String,
        authorisationId: Long,
        assessmentRef: Long,
        baRef: String,
        owner: Boolean
  ): Action[AnyContent] = authenticated.async { implicit request =>
    propertyLinkService.getSingularPropertyLink(submissionId, owner).flatMap {
      case Some(propertyLink) =>
        if (isExternalValuation) {
          businessRatesValuations
            .isViewableExternal(propertyLink.uarn, assessmentRef, submissionId)
            .map {
              case true =>
                if (owner) {
                  if (isExternalValuation) {
                    Redirect(config.businessRatesValuationFrontendUrl(
                      s"property-link/$authorisationId/valuations/$assessmentRef?submissionId=$submissionId"))
                  } else {
                    Redirect(config.businessRatesValuationFrontendUrl(
                      s"property-link/$authorisationId/assessment/$assessmentRef?submissionId=$submissionId"))
                  }
                } else {
                  if (isExternalValuation) {
                    Redirect(config.businessRatesValuationFrontendUrl(
                      s"property-link/clients/$authorisationId/valuations/$assessmentRef?submissionId=$submissionId"))
                  } else {
                    Redirect(config.businessRatesValuationFrontendUrl(
                      s"property-link/clients/$authorisationId/assessment/$assessmentRef?submissionId=$submissionId"))
                  }
                }
              case false =>
                Redirect(
                  if (owner)
                    controllers.detailedvaluationrequest.routes.DvrController
                      .myOrganisationRequestDetailValuationCheck(submissionId, assessmentRef)
                  else
                    controllers.detailedvaluationrequest.routes.DvrController
                      .myClientsRequestDetailValuationCheck(submissionId, assessmentRef)
                )
            }
        } else {
          businessRatesValuations
            .isViewable(propertyLink.uarn, assessmentRef, propertyLink.authorisationId)
            .map {
              case true =>
                if (owner) {
                  if (isExternalValuation) {
                    Redirect(config.businessRatesValuationFrontendUrl(
                      s"property-link/$authorisationId/valuations/$assessmentRef?submissionId=$submissionId"))
                  } else {
                    Redirect(config.businessRatesValuationFrontendUrl(
                      s"property-link/$authorisationId/assessment/$assessmentRef?submissionId=$submissionId"))
                  }
                } else {
                  if (isExternalValuation) {
                    Redirect(config.businessRatesValuationFrontendUrl(
                      s"property-link/clients/$authorisationId/valuations/$assessmentRef?submissionId=$submissionId"))
                  } else {
                    Redirect(config.businessRatesValuationFrontendUrl(
                      s"property-link/clients/$authorisationId/assessment/$assessmentRef?submissionId=$submissionId"))
                  }
                }
              case false =>
                Redirect(
                  if (owner)
                    controllers.detailedvaluationrequest.routes.DvrController
                      .myOrganisationRequestDetailValuationCheck(submissionId, assessmentRef)
                  else
                    controllers.detailedvaluationrequest.routes.DvrController
                      .myClientsRequestDetailValuationCheck(submissionId, assessmentRef)
                )
            }
        }
      case None => Future.successful(notFound)
    }
  }

  def startChallengeFromDVR(
        submissionId: String,
        valuationId: Long,
        owner: Boolean
  ): Action[AnyContent] = authenticated { implicit request =>
    Ok(views.html.dvr.challenge_valuation(submissionId, valuationId, owner))
  }
}

case class AssessmentsVM(
      assessmentsWithLinks: Seq[(String, ApiAssessment)],
      backLink: String,
      address: String,
      capacity: Option[String]
)

case class RequestDetailedValuationVM(form: Form[_], authId: Long, assessmentRef: Long, baRef: String)
