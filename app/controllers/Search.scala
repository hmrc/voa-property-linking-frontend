/*
 * Copyright 2016 HM Revenue & Customs
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

import javax.inject.Inject

import config.Wiring
import connectors.{CapacityDeclaration, EnvelopeConnector}
import connectors.fileUpload.FileUploadConnector
import form.EnumMapping
import form.Mappings.{dmyDate, dmyPastDate}
import models._
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.Result
import uk.gov.hmrc.play.config.ServicesConfig

class Search @Inject()(val fileUploadConnector: FileUploadConnector,
                       val envelopeConnector: EnvelopeConnector) extends PropertyLinkingController with ServicesConfig {
  lazy val sessionRepository = Wiring().sessionRepository
  lazy val connector = Wiring().propertyConnector
  lazy val ggAction = Wiring().ggAction

  def show() = ggAction { _ => implicit request =>
    Redirect(s"${baseUrl("vmv-frontend")}/view-my-valuation/search")
  }

  def declareCapacity(uarn: Long) = ggAction.async { _ => implicit request =>
    connector.get(uarn).flatMap {
      case Some(pd) =>
        fileUploadConnector.createEnvelope().flatMap(envelopeId => {
          val submissionId = java.util.UUID.randomUUID().toString.replace("-", "")
          Logger.debug(s"envelope id: ${envelopeId}")
          sessionRepository.start(pd, envelopeId, submissionId) map { _ =>
            Ok(views.html.declareCapacity(DeclareCapacityVM(Search.declareCapacityForm, pd.address)))
          }
        }
        )
      case None => NotFound("Not found")
    }
  }

  def attemptLink() = ggAction.async { _ => implicit request =>
    sessionRepository.get() flatMap {
      case Some(session) => Search.declareCapacityForm.bindFromRequest().fold(
        errors => BadRequest(views.html.declareCapacity(DeclareCapacityVM(errors, session.claimedProperty.address))),
        formData => sessionRepository.saveOrUpdate(session.withDeclaration(formData)) map { _ =>
          chooseLinkingJourney(session.claimedProperty, formData)
        }
      )
      case None => NotFound("No linking session")
    }
  }

  private def chooseLinkingJourney(p: Property, d: CapacityDeclaration): Result =
    if (p.isSelfCertifiable)
      Redirect(routes.SelfCertification.show())
    else
      Redirect(routes.ChooseEvidence.show())

}

object Search {
  lazy val declareCapacityForm = Form(mapping(
    "capacity" -> EnumMapping(CapacityType),
    "fromDate" -> dmyPastDate,
    "toDate" -> optional(dmyDate)
  )(CapacityDeclaration.apply)(CapacityDeclaration.unapply))
}

case class DeclareCapacityVM(form: Form[_], address: Address)
