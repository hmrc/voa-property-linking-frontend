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

package controllers.propertyLinking

import java.time.LocalDate

import actions.propertylinking.WithLinkingSession
import javax.inject.{Inject, Named}
import actions.{AuthenticatedAction, AuthenticatedRequest}
import binders.propertylinks.GetPropertyLinksParameters
import com.google.inject.Singleton
import config.ApplicationConfig
import connectors.propertyLinking.PropertyLinkConnector
import connectors.SubmissionIdConnector
import controllers._
import form.Mappings._
import form.{ConditionalDateAfter, EnumMapping}
import models.{CapacityDeclaration, _}
import play.api.{Configuration, Environment}
import play.api.Mode.Mode
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import repositories.SessionRepo
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.voa.play.form.ConditionalMappings._
import views.helpers.Errors
import uk.gov.hmrc.http.Upstream5xxResponse
import uk.gov.voa.propertylinking.errorhandler.CustomErrorHandler

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClaimProperty @Inject()(
                               val errorHandler: CustomErrorHandler,
                              val submissionIdConnector: SubmissionIdConnector,
                              @Named("propertyLinkingSession") val sessionRepository: SessionRepo,
                              authenticatedAction: AuthenticatedAction,
                              withLinkingSession: WithLinkingSession,
                              val propertyLinksConnector: PropertyLinkConnector,
                              val runModeConfiguration: Configuration,
                              val environment: Environment
                             )(implicit executionContext: ExecutionContext, val messagesApi: MessagesApi, val config: ApplicationConfig)
  extends PropertyLinkingController with ServicesConfig {

  import ClaimProperty._

  def show() = authenticatedAction { implicit request =>
    Redirect(s"${config.vmvUrl}/search")
  }

  def checkPropertyLinks() = authenticatedAction.async { implicit request =>

    val pLinks = propertyLinksConnector.getMyOrganisationsPropertyLinks(GetPropertyLinksParameters(), PaginationParams(1, 20, false))

    pLinks.map {
    res =>
      if(res.authorisations.nonEmpty){
        Redirect(s"${config.vmvUrl}/search")
      }
      else{
        Ok(views.html.propertyLinking.beforeYouStart())
      }
    }
  }

  def declareCapacity(uarn: Long, address: String) = authenticatedAction { implicit request =>
    Ok(views.html.propertyLinking.declareCapacity(DeclareCapacityVM(declareCapacityForm, address, uarn)))
  }

  def attemptLink(uarn: Long, address: String): Action[AnyContent] = authenticatedAction.async { implicit request =>
    ClaimProperty.declareCapacityForm.bindFromRequest().fold(
      errors => Future.successful(BadRequest(views.html.propertyLinking.declareCapacity(DeclareCapacityVM(errors, address, uarn)))),
      formData => initialiseSession(formData, uarn, address).map { _ =>
        Redirect(routes.ChooseEvidence.show())
      }.recover {
        case Upstream5xxResponse(_, 503, _) => ServiceUnavailable(views.html.errors.serviceUnavailable())
      }
    )
  }

  def back: Action[AnyContent] = authenticatedAction.andThen(withLinkingSession) { implicit request =>
    val form = declareCapacityForm.fillAndValidate(request.ses.declaration)
    Ok(views.html.propertyLinking.declareCapacity(DeclareCapacityVM(form, request.ses.address, request.ses.uarn)))
  }

  private def initialiseSession(declaration: CapacityDeclaration, uarn: Long, address: String)(implicit request: AuthenticatedRequest[_]): Future[Unit] = {
    for {
      submissionId <- submissionIdConnector.get()
      _ <- sessionRepository.start[LinkingSession](LinkingSession(address, uarn, submissionId, request.personId, declaration))
    } yield ()
  }

  override protected def mode: Mode = environment.mode
}

object ClaimProperty {

  lazy val declareCapacityForm = Form(mapping(
    "capacity" -> EnumMapping(CapacityType),
    "interestedBefore2017" -> mandatoryBoolean,
    "fromDate" -> mandatoryIfFalse("interestedBefore2017", dmyDateAfterThreshold.verifying(Errors.dateMustBeInPast, d => d.isBefore(LocalDate.now))),
    "stillInterested" -> mandatoryBoolean,
    "toDate" -> mandatoryIfFalse("stillInterested", ConditionalDateAfter("interestedBefore2017", "fromDate")
      .verifying(Errors.dateMustBeInPast, d => d.isBefore(LocalDate.now))
      .verifying(Errors.dateMustBeAfter1stApril2017, d => d.isAfter(LocalDate.of(2017,4,1))))
  )(CapacityDeclaration.apply)(CapacityDeclaration.unapply))
}

case class DeclareCapacityVM(form: Form[_], address: String, uarn: Long)
