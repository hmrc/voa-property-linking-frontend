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

package connectors

import connectors.attachments.BusinessRatesAttachmentsConnector
import controllers.VoaPropertyLinkingSpec
import models.RatesBillType
import models.attachment._
import models.attachment.request.InitiateAttachmentRequest
import models.upscan._
import play.api.libs.json.JsObject

class BusinessRatesAttachmentsConnectorSpec extends VoaPropertyLinkingSpec {

  val initiateAttachmentRequest = InitiateAttachmentRequest("FILE_NAME", "img/jpeg", RatesBillType)
  val attachments = mock[Attachment]

  "initiateAttachment" should "call to initiateAttachment return a successful" in {
    val testConnector = new BusinessRatesAttachmentsConnector(mockHttpClient, servicesConfig)
    mockHttpPOST[InitiateAttachmentRequest, PreparedUpload]("tst-url", preparedUpload)
    whenReady(
      testConnector.initiateAttachmentUpload(
        InitiateAttachmentPayload(initiateAttachmentRequest, "http://example.com", "http://example.com/failure")))(
      _ shouldBe preparedUpload)
  }

  "submitFile" should "submit the file" in {
    val testConnector = new BusinessRatesAttachmentsConnector(mockHttpClient, servicesConfig)(ec)
    mockHttpPATCH[JsObject, Attachment]("tst-url", attachments)
    whenReady(testConnector.submitFile("file-reference", "submission-id"))(_ shouldBe attachments)
  }
}
