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

import javax.inject.Inject

import config.{ApplicationConfig, Global}
import controllers._
import play.api.data.Form
import play.api.data.Forms._
import session.{LinkingSessionRequest, WithLinkingSession}
import uk.gov.hmrc.circuitbreaker.UnhealthyServiceException
import views.html.propertyLinking.uploadRatesBill

class UploadRatesBill @Inject()(override val config: ApplicationConfig,
                                override val withLinkingSession: WithLinkingSession,
                                withCircuitBreaker: FileUploadCircuitBreaker)
  extends PropertyLinkingController with FileUploadHelpers {

  override val successUrl: String = routes.UploadRatesBill.fileUploaded().url

  def show(errorCode: Option[Int], errorMessage: Option[String]) = withLinkingSession { implicit request =>
    withCircuitBreaker {
      errorCode match {
        case Some(REQUEST_ENTITY_TOO_LARGE) => EntityTooLarge(uploadRatesBill(UploadRatesBillVM(fileTooLargeError, submissionUrl)))
        case Some(NOT_FOUND) => NotFound(Global.notFoundTemplate)
        case Some(UNSUPPORTED_MEDIA_TYPE) => UnsupportedMediaType(uploadRatesBill(UploadRatesBillVM(invalidFileTypeError, submissionUrl)))
        // this assumes BAD_REQUEST is caused by "Envelope does not allow zero length files, and submitted file has length 0"
        case Some(BAD_REQUEST) => UnsupportedMediaType(uploadRatesBill(UploadRatesBillVM(invalidFileTypeError, submissionUrl)))
        //if FUaaS repeatedly returns unexpected error codes e.g. 500s, trigger the circuit breaker
        case Some(err) => throw new IllegalArgumentException(s"Unexpected response from FUaaS: $err; ${errorMessage.map(msg => s"error: $msg")}")
        case None => Ok(uploadRatesBill(UploadRatesBillVM(form, submissionUrl)))
      }
    } recover {
      case _: UnhealthyServiceException => ServiceUnavailable(views.html.errors.serviceUnavailable())
    }
  }

  lazy val form = Form(single("ratesBill[]" -> text))
  lazy val fileTooLargeError = form.withError("ratesBill[]", "error.fileUpload.tooLarge")
  lazy val invalidFileTypeError = form.withError("ratesBill[]", "error.fileUpload.invalidFileType")

  private def submissionUrl(implicit request: LinkingSessionRequest[_]) = fileUploadUrl(routes.UploadRatesBill.show().url)
}

case class UploadRatesBillVM(form: Form[_], submissionUrl: String)