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

import connectors.attachments.BusinessRatesAttachmentConnector
import controllers.VoaPropertyLinkingSpec
import models.attachment._
import models.attachment.request.InitiateAttachmentRequest
import models.upscan._
import play.api.libs.json.JsObject

class BusinessRatesAttachmentConnectorSpec extends VoaPropertyLinkingSpec {

  val initiateAttachmentRequest = InitiateAttachmentRequest("FILE_NAME", "img/jpeg")
  val attachments = mock[Attachment]

  "initiateAttachment" should "call to initiateAttachment return a successful" in {
    val testConnector = new BusinessRatesAttachmentConnector(mockWSHttp, applicationConfig)
    mockHttpPOST[InitiateAttachmentRequest, PreparedUpload]("tst-url", preparedUpload)
    whenReady(testConnector.initiateAttachmentUpload(InitiateAttachmentPayload(initiateAttachmentRequest, "http://example.com", "http://example.com/failure")))(_ mustBe preparedUpload)
  }


    "submitFile" should   "submit the file" in {
      val testConnector = new BusinessRatesAttachmentConnector(mockWSHttp,applicationConfig)(ec)
      mockHttpPATCH[JsObject, Attachment]("tst-url", attachments)
      whenReady(testConnector.submitFile("file-reference", "submission-id"))(_ mustBe attachments)
    }
}