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

package controllers.propertyLinking

import java.time.LocalDate
import javax.inject.{Inject, Named}

import actions.{AuthenticatedAction, AuthenticatedRequest}
import com.google.inject.Singleton
import config.ApplicationConfig
import connectors.{EnvelopeConnector, EnvelopeMetadata, SubmissionIdConnector}
import controllers.PropertyLinkingController
import form.Mappings._
import form.{ConditionalDateAfter, EnumMapping}
import models.{CapacityDeclaration, _}
import play.api.data.Form
import play.api.data.Forms._
import repositories.SessionRepo
import session.WithLinkingSession
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.Upstream5xxResponse
import uk.gov.voa.play.form.ConditionalMappings._
import views.helpers.Errors

@Singleton
class ClaimProperty @Inject()(val config: ApplicationConfig,
                              val envelopeConnector: EnvelopeConnector,
                              val authenticated: AuthenticatedAction,
                              val submissionIdConnector: SubmissionIdConnector,
                              @Named("propertyLinkingSession") val sessionRepository: SessionRepo,
                              val withLinkingSession: WithLinkingSession)
  extends PropertyLinkingController with ServicesConfig {

  import ClaimProperty._

  def show() = authenticated { implicit request =>
    Redirect(s"${config.vmvUrl}/search")
  }

  def declareCapacity(uarn: Long, address: String) = authenticated { implicit request =>
    Ok(views.html.propertyLinking.declareCapacity(DeclareCapacityVM(declareCapacityForm, address, uarn)))
  }

  def attemptLink(uarn: Long, address: String) = authenticated { implicit request =>
    ClaimProperty.declareCapacityForm.bindFromRequest().fold(
      errors => BadRequest(views.html.propertyLinking.declareCapacity(DeclareCapacityVM(errors, address, uarn))),
      formData => initialiseSession(formData, uarn, address).map { _ =>
        Redirect(routes.ChooseEvidence.show())
      }.recover {
        case Upstream5xxResponse(_, 503, _) => ServiceUnavailable(views.html.errors.serviceUnavailable())
      }
    )
  }

  def back = withLinkingSession { implicit request =>
    val form = declareCapacityForm.fillAndValidate(request.ses.declaration)
    Ok(views.html.propertyLinking.declareCapacity(DeclareCapacityVM(form, request.ses.address, request.ses.uarn)))
  }

  private def initialiseSession(declaration: CapacityDeclaration, uarn: Long, address: String)(implicit request: AuthenticatedRequest[_]) = {
    for {
      submissionId <- submissionIdConnector.get()
      envelopeId <- envelopeConnector.createEnvelope(EnvelopeMetadata(submissionId, request.personId))
      _ <- sessionRepository.start[LinkingSession](LinkingSession(address, uarn, envelopeId, submissionId, request.personId, declaration))
    } yield ()
  }

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
