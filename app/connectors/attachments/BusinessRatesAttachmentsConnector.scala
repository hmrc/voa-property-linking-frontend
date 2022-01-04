/*
 * Copyright 2022 HM Revenue & Customs
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

package connectors.attachments

import models.attachment._
import models.attachment.request.MetaDataRequest
import models.upscan.PreparedUpload
import play.api.Logging
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BusinessRatesAttachmentsConnector @Inject()(
      val http: HttpClient,
      val servicesConfig: ServicesConfig
)(implicit executionContext: ExecutionContext)
    extends Logging {

  val baseURL: String = servicesConfig.baseUrl("business-rates-attachments")

  def initiateAttachmentUpload(uploadSettings: InitiateAttachmentPayload)(
        implicit headerCarrier: HeaderCarrier): Future[PreparedUpload] =
    http.POST[InitiateAttachmentPayload, PreparedUpload](
      s"$baseURL/business-rates-attachments/v2/initiate",
      uploadSettings)

  def getAttachment(reference: String)(implicit hc: HeaderCarrier): Future[Attachment] =
    http.GET[Attachment](s"$baseURL/business-rates-attachments/attachments/$reference")

  def submitFile(fileReference: String, submissionId: String)(
        implicit headerCarrier: HeaderCarrier): Future[Attachment] =
    http
      .PATCH[MetaDataRequest, Attachment](
        s"$baseURL/business-rates-attachments/attachments/$fileReference",
        MetaDataRequest(submissionId))
      .recover {
        case ex: Exception =>
          logger.warn(s"File Submission failed for File Reference: $fileReference Response body", ex)
          throw ex
      }
}
