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

import controllers.VoaPropertyLinkingSpec
import models.attachment.InitiateAttachmentRequest
import models.upscan._
import play.api.libs.json.JsObject
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}
import models.attachment._

import scala.concurrent.ExecutionContext._

class BusinessRatesAttachmentConnectorSpec extends VoaPropertyLinkingSpec {

  implicit val ee = scala.concurrent.ExecutionContext.Implicits.global
  implicit val hc = HeaderCarrier()
  val initiateAttachmentRequest = InitiateAttachmentRequest("FILE_NAME", "img/jpeg", None)
  val attachments = mock[Attachment]

    "initiateAttachment" should  "call to initiateAttachment return a successful" in {
      val testConnector = new BusinessRatesAttachmentConnector(mockWSHttp)(applicationConfig, ee)
      mockHttpPOST[InitiateAttachmentRequest, PreparedUpload]("tst-url", preparedUpload)
      whenReady(testConnector.initiateAttachmentUpload(initiateAttachmentRequest))(_ mustBe preparedUpload)
    }


    "submitFile" should   "submit the file" in {
      val testConnector = new BusinessRatesAttachmentConnector(mockWSHttp)(applicationConfig, ee)
      mockHttpPATCH[JsObject, Attachment]("tst-url", attachments)
      whenReady(testConnector.submitFile("file-reference", "submission-id"))(_ mustBe Some(attachments))
    }

     "submitFile" should  "submit the file throws BAD_REQUEST" in {
      val testConnector = new BusinessRatesAttachmentConnector(mockWSHttp)(applicationConfig, ee)
      mockHttpFailedPATCH[JsObject, Attachment]("tst-url", new BadRequestException("400"))
      whenReady(testConnector.submitFile("file-reference", "submission-id"))(_ mustBe None)
    }




}