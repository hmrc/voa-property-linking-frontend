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

package connectors

import javax.inject.{Inject, Singleton}

import config.WSHttp
import models.attachment.InitiateAttachmentRequest
import models.attachment.SubmissionTypesValues.PropertyLinkEvidence
import models.upscan.PreparedUpload
import play.api.Logger
import play.api.libs.json.{JsObject, JsString, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpErrorFunctions}
import models.attachment._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class BusinessRatesAttachmentConnector @Inject()(val  http: WSHttp, val appConfig: ServicesConfig)(implicit executionContext: ExecutionContext)
  extends JsonHttpReads with OptionHttpReads with RawReads with AttachmentHttpErrorFunctions {

  val baseURL: String = appConfig.baseUrl("business-rates-attachments")
  def initiateAttachmentUpload(uploadSettings: InitiateAttachmentRequest)(
        implicit headerCarrier: HeaderCarrier): Future[PreparedUpload] = {
    http.POST[InitiateAttachmentRequest, PreparedUpload](s"$baseURL/business-rates-attachments/initiate", uploadSettings)

  }

  def submitFile(fileReference: String, submissionId: String)(
    implicit headerCarrier: HeaderCarrier): Future[Option[Attachment]] = {
    http.PATCH[JsObject, Attachment](s"$baseURL/business-rates-attachments/attachments/${fileReference}",
      Json.obj(PropertyLinkEvidence.submissionId.toString -> JsString(submissionId))).map(Some.apply).recover {
      case ex: Exception =>
        Logger.warn(s"File Submission failed for File Reference: ${fileReference} Response body}", ex)
        None
    }
  }

}


case class FileAttachmentFailed(errorMessage: String) extends Exception

import play.api.http.Status.BAD_REQUEST
import uk.gov.hmrc.http._

trait AttachmentHttpErrorFunctions extends HttpErrorFunctions {

  override def handleResponse(httpMethod: String, url: String)(response: HttpResponse): HttpResponse =
    response.status match {
      case BAD_REQUEST =>
        Logger.warn(s"Upload failed with status ${response.status}. Response body: ${response.body}")
        throw FileAttachmentFailed(response.body)
      case _           =>
        super.handleResponse(httpMethod, url)(response)
    }
}
