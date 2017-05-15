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

import javax.inject.{Inject, Named}

import actions.AuthenticatedRequest
import com.google.inject.Singleton
import config.{ApplicationConfig, Wiring}
import connectors.fileUpload.{EnvelopeMetadata, FileUploadConnector}
import form.Mappings._
import form.{ConditionalDateAfter, EnumMapping}
import models.{CapacityDeclaration, _}
import org.joda.time.LocalDate
import play.api.data.Form
import play.api.data.Forms._
import repositories.{SessionRepo, SessionRepository}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.voa.play.form.ConditionalMappings._
import views.helpers.Errors

import scala.concurrent.Future

@Singleton
class ClaimProperty @Inject()(val fileUploadConnector: FileUploadConnector,
                              @Named("propertyLinkingSession") val sessionRepository: SessionRepo)
  extends PropertyLinkingController with ServicesConfig {
  lazy val authenticated = Wiring().authenticated
  lazy val submissionIdConnector = Wiring().submissionIdConnector

  def show() = authenticated { implicit request =>
    Redirect(s"${ApplicationConfig.vmvUrl}/cca/search")
  }

  def declareCapacity(uarn: Long, address: String) = authenticated { implicit request =>
    Ok(views.html.declareCapacity(DeclareCapacityVM(ClaimProperty.declareCapacityForm, address, uarn)))
  }

  def attemptLink(uarn: Long, address: String) = authenticated { implicit request =>
    ClaimProperty.declareCapacityForm.bindFromRequest().fold(
      errors => BadRequest(views.html.declareCapacity(DeclareCapacityVM(errors, address, uarn))),
      formData => initialiseSession(formData, uarn, address) map { _ =>
        Redirect(routes.ChooseEvidence.show())
      }
    )
  }

  private def initialiseSession(declaration: CapacityDeclaration, uarn: Long, address: String)(implicit request: AuthenticatedRequest[_]) = {
    for {
      submissionId <- submissionIdConnector.get()
      envelopeId <- fileUploadConnector.createEnvelope(EnvelopeMetadata(submissionId, request.personId))
      _ <- sessionRepository.start[LinkingSession](LinkingSession(address, uarn, envelopeId, submissionId, request.personId, declaration))
    } yield ()
  }

}

object ClaimProperty {
  lazy val declareCapacityForm = Form(mapping(
    "capacity" -> EnumMapping(CapacityType),
    "interestedBefore2017" -> mandatoryBoolean,
    "fromDate" -> mandatoryIfFalse("interestedBefore2017", dmyDateAfterThreshold.verifying(Errors.dateMustBeInPast, d => !d.isAfter(LocalDate.now))),
    "stillInterested" -> mandatoryBoolean,
    "toDate" -> mandatoryIfFalse("stillInterested", ConditionalDateAfter("interestedBefore2017", "fromDate")
      .verifying(Errors.dateMustBeInPast, d => !d.isAfter(LocalDate.now))
      .verifying(Errors.dateMustBeAfter1stApril2017, d => d.isAfter(new LocalDate(2017,4,1))))
  )(CapacityDeclaration.apply)(CapacityDeclaration.unapply))
}

case class DeclareCapacityVM(form: Form[_], address: String, uarn: Long)